package com.aji.accounting.inventory;

import com.aji.accounting.coa.Account;
import com.aji.accounting.coa.AccountMappingKey;
import com.aji.accounting.coa.AccountMappingService;
import com.aji.accounting.common.exception.BusinessException;
import com.aji.accounting.common.util.Money;
import com.aji.accounting.company.Company;
import com.aji.accounting.company.CompanyService;
import com.aji.accounting.company.InventoryCostingMethod;
import com.aji.accounting.config.CurrentUser;
import com.aji.accounting.ledger.SourceType;
import com.aji.accounting.masterdata.item.Item;
import com.aji.accounting.masterdata.item.ItemType;
import com.aji.accounting.masterdata.warehouse.Warehouse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Inti sub-ledger persediaan (sistem perpetual).
 * Semua perubahan stok WAJIB lewat kelas ini agar saldo stok, kartu stok, dan layer FIFO selalu konsisten
 * dengan jurnal. Nilai keluar dihitung sesuai metode perusahaan: Moving Average atau FIFO.
 * Baris saldo dikunci (SELECT ... FOR UPDATE) selama transaksi untuk mencegah race condition.
 */
@Service
@RequiredArgsConstructor
public class InventoryService {

    private final StockBalanceRepository balances;
    private final StockMovementRepository movements;
    private final StockCostLayerRepository layers;
    private final CompanyService companyService;
    private final AccountMappingService mappings;

    /** Akun persediaan sesuai tipe item. */
    public Account inventoryAccount(ItemType type) {
        return mappings.get(switch (type) {
            case RAW_MATERIAL -> AccountMappingKey.INVENTORY_RAW_MATERIAL;
            case SUPPORTING_MATERIAL -> AccountMappingKey.INVENTORY_SUPPORTING_MATERIAL;
            case WORK_IN_PROCESS -> AccountMappingKey.INVENTORY_WIP;
            case FINISHED_GOOD -> AccountMappingKey.INVENTORY_FINISHED_GOOD;
            case SUPPLIES -> AccountMappingKey.INVENTORY_SUPPLIES;
            case SERVICE -> throw new BusinessException("Item jasa tidak memiliki akun persediaan");
        });
    }

    /** Barang masuk dengan biaya per unit tertentu. */
    @Transactional(propagation = Propagation.MANDATORY)
    public StockMovement receive(Item item, Warehouse wh, LocalDate date, BigDecimal qty, BigDecimal unitCost,
                                 SourceType sourceType, Long sourceId, String sourceNo, String description) {
        BigDecimal total = Money.multiply(qty, unitCost);
        return receiveValue(item, wh, date, qty, total, sourceType, sourceId, sourceNo, description);
    }

    /** Barang masuk dengan total nilai tertentu (dipakai saat nilai sudah dihitung, mis. penyelesaian produksi). */
    @Transactional(propagation = Propagation.MANDATORY)
    public StockMovement receiveValue(Item item, Warehouse wh, LocalDate date, BigDecimal qty, BigDecimal totalValue,
                                      SourceType sourceType, Long sourceId, String sourceNo, String description) {
        assertStocked(item);
        assertPositive(qty);
        if (totalValue.signum() < 0) {
            throw new BusinessException("Nilai barang masuk tidak boleh negatif");
        }
        BigDecimal total = Money.amount(totalValue);
        BigDecimal unitCost = Money.divideCost(total, qty);
        StockBalance b = lockBalance(item, wh);

        BigDecimal newQty = b.getQty().add(qty);
        BigDecimal newValue = b.getTotalValue().add(total);
        b.setQty(newQty);
        b.setTotalValue(newValue);
        b.setAvgCost(newQty.signum() > 0 ? Money.divideCost(newValue, newQty) : unitCost);
        b.setLastMovementDate(maxDate(b.getLastMovementDate(), date));

        StockMovement m = movement(item, wh, date, StockDirection.IN, qty, unitCost, total, b, sourceType, sourceId,
                sourceNo, description);
        if (method() == InventoryCostingMethod.FIFO) {
            StockCostLayer layer = new StockCostLayer();
            layer.setItemId(item.getId());
            layer.setWarehouseId(wh.getId());
            layer.setMovementId(m.getId());
            layer.setLayerDate(date);
            layer.setQtyIn(qty);
            layer.setQtyRemaining(qty);
            layer.setUnitCost(unitCost);
            layers.save(layer);
        }
        return m;
    }

    /** Barang keluar dinilai sesuai metode persediaan perusahaan. */
    @Transactional(propagation = Propagation.MANDATORY)
    public StockMovement issue(Item item, Warehouse wh, LocalDate date, BigDecimal qty,
                               SourceType sourceType, Long sourceId, String sourceNo, String description) {
        assertStocked(item);
        assertPositive(qty);
        StockBalance b = lockBalance(item, wh);
        assertAvailable(item, wh, b, qty);

        BigDecimal total = method() == InventoryCostingMethod.FIFO
                ? consumeFifo(item, wh, b, qty)
                : movingAverageCost(item, b, qty);
        return applyOut(item, wh, date, qty, total, b, sourceType, sourceId, sourceNo, description);
    }

    /**
     * Barang keluar pada biaya tertentu (retur ke pemasok). Untuk FIFO, layer dikonsumsi FIFO
     * sehingga nilai aktual bisa berbeda — selisihnya ditangani pemanggil.
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public StockMovement issueAtCost(Item item, Warehouse wh, LocalDate date, BigDecimal qty, BigDecimal unitCost,
                                     SourceType sourceType, Long sourceId, String sourceNo, String description) {
        if (method() == InventoryCostingMethod.FIFO) {
            return issue(item, wh, date, qty, sourceType, sourceId, sourceNo, description);
        }
        assertStocked(item);
        assertPositive(qty);
        StockBalance b = lockBalance(item, wh);
        assertAvailable(item, wh, b, qty);
        BigDecimal total = b.getQty().compareTo(qty) == 0 ? b.getTotalValue() : Money.multiply(qty, unitCost);
        return applyOut(item, wh, date, qty, total, b, sourceType, sourceId, sourceNo, description);
    }

    /** Membatalkan semua mutasi stok milik dokumen sumber (saat dokumen di-void). @return total nilai yang dibalik (IN positif). */
    @Transactional(propagation = Propagation.MANDATORY)
    public void reverseSource(SourceType sourceType, Long sourceId, LocalDate date, String reason) {
        List<StockMovement> list = movements.findBySource(sourceType, sourceId);
        for (StockMovement m : list) {
            StockBalance b = lockBalance(m.getItem(), m.getWarehouse());
            String desc = "Pembatalan: " + reason;
            if (m.getDirection() == StockDirection.IN) {
                if (b.getQty().compareTo(m.getQty()) < 0 && !company().isAllowNegativeStock()) {
                    throw new BusinessException("Tidak dapat dibatalkan: stok " + m.getItem().getCode()
                            + " sudah terpakai (tersisa " + b.getQty().stripTrailingZeros().toPlainString() + ")");
                }
                if (method() == InventoryCostingMethod.FIFO) {
                    layers.findByMovementId(m.getId()).ifPresent(layer -> {
                        if (layer.getQtyRemaining().compareTo(layer.getQtyIn()) < 0) {
                            throw new BusinessException("Tidak dapat dibatalkan: layer biaya " + m.getItem().getCode()
                                    + " dari dokumen " + m.getSourceNo() + " sudah terpakai sebagian");
                        }
                        layer.setQtyRemaining(BigDecimal.ZERO);
                    });
                }
                applyOut(m.getItem(), m.getWarehouse(), date, m.getQty(), m.getTotalCost(), b, sourceType, sourceId,
                        m.getSourceNo(), desc);
            } else {
                receiveValue(m.getItem(), m.getWarehouse(), date, m.getQty(), m.getTotalCost(), sourceType, sourceId,
                        m.getSourceNo(), desc);
            }
        }
    }

    /** Saldo saat ini (tanpa lock) untuk tampilan/default biaya. */
    @Transactional(readOnly = true)
    public StockBalance currentBalance(Long itemId, Long warehouseId) {
        return balances.find(itemId, warehouseId).orElse(null);
    }

    /** Estimasi biaya per unit saat ini (avg cost; fallback biaya standar item). */
    @Transactional(readOnly = true)
    public BigDecimal currentUnitCost(Item item, Long warehouseId) {
        return balances.find(item.getId(), warehouseId)
                .filter(b -> b.getQty().signum() > 0)
                .map(StockBalance::getAvgCost)
                .orElse(item.getStandardCost());
    }

    // ------------------------------------------------------------------ internal

    private BigDecimal movingAverageCost(Item item, StockBalance b, BigDecimal qty) {
        if (b.getQty().compareTo(qty) == 0) {
            return b.getTotalValue(); // keluarkan semua: tidak ada sisa pembulatan
        }
        BigDecimal unit = b.getQty().signum() > 0 ? b.getAvgCost() : item.getStandardCost();
        return Money.multiply(qty, unit);
    }

    private BigDecimal consumeFifo(Item item, Warehouse wh, StockBalance b, BigDecimal qty) {
        BigDecimal remaining = qty;
        BigDecimal total = BigDecimal.ZERO;
        BigDecimal lastCost = item.getStandardCost();
        for (StockCostLayer layer : layers.findOpenLayersForUpdate(item.getId(), wh.getId())) {
            if (remaining.signum() == 0) {
                break;
            }
            BigDecimal take = layer.getQtyRemaining().min(remaining);
            layer.setQtyRemaining(layer.getQtyRemaining().subtract(take));
            total = total.add(Money.multiply(take, layer.getUnitCost()));
            remaining = remaining.subtract(take);
            lastCost = layer.getUnitCost();
        }
        if (remaining.signum() > 0) {
            // hanya terjadi bila stok negatif diizinkan
            total = total.add(Money.multiply(remaining, lastCost));
        }
        if (b.getQty().compareTo(qty) == 0) {
            total = b.getTotalValue();
        }
        return total;
    }

    private StockMovement applyOut(Item item, Warehouse wh, LocalDate date, BigDecimal qty, BigDecimal total,
                                   StockBalance b, SourceType sourceType, Long sourceId, String sourceNo,
                                   String description) {
        BigDecimal newQty = b.getQty().subtract(qty);
        BigDecimal newValue = b.getTotalValue().subtract(total);
        if (newQty.signum() == 0 && newValue.abs().compareTo(new BigDecimal("0.05")) <= 0) {
            newValue = BigDecimal.ZERO;
        }
        b.setQty(newQty);
        b.setTotalValue(newValue);
        if (newQty.signum() > 0) {
            b.setAvgCost(Money.divideCost(newValue, newQty));
        }
        b.setLastMovementDate(maxDate(b.getLastMovementDate(), date));
        return movement(item, wh, date, StockDirection.OUT, qty, Money.divideCost(total, qty), total, b, sourceType,
                sourceId, sourceNo, description);
    }

    private StockMovement movement(Item item, Warehouse wh, LocalDate date, StockDirection dir, BigDecimal qty,
                                   BigDecimal unitCost, BigDecimal total, StockBalance after, SourceType sourceType,
                                   Long sourceId, String sourceNo, String description) {
        StockMovement m = new StockMovement();
        m.setMovementDate(date);
        m.setItem(item);
        m.setWarehouse(wh);
        m.setDirection(dir);
        m.setQty(qty);
        m.setUnitCost(unitCost);
        m.setTotalCost(total);
        m.setQtyAfter(after.getQty());
        m.setValueAfter(after.getTotalValue());
        m.setSourceType(sourceType);
        m.setSourceId(sourceId);
        m.setSourceNo(sourceNo);
        m.setDescription(description == null ? null : description.substring(0, Math.min(255, description.length())));
        m.setCreatedAt(LocalDateTime.now());
        m.setCreatedBy(CurrentUser.usernameOrSystem());
        return movements.save(m);
    }

    private StockBalance lockBalance(Item item, Warehouse wh) {
        return balances.findForUpdate(item.getId(), wh.getId()).orElseGet(() -> {
            StockBalance b = new StockBalance();
            b.setItem(item);
            b.setWarehouse(wh);
            balances.saveAndFlush(b);
            return balances.findForUpdate(item.getId(), wh.getId()).orElseThrow();
        });
    }

    private void assertAvailable(Item item, Warehouse wh, StockBalance b, BigDecimal qty) {
        if (b.getQty().compareTo(qty) < 0 && !company().isAllowNegativeStock()) {
            throw new BusinessException("INSUFFICIENT_STOCK", "Stok " + item.getCode() + " " + item.getName()
                    + " di gudang " + wh.getName() + " tidak cukup: tersedia "
                    + b.getQty().stripTrailingZeros().toPlainString() + ", dibutuhkan "
                    + qty.stripTrailingZeros().toPlainString());
        }
    }

    private static void assertStocked(Item item) {
        if (!item.isStocked()) {
            throw new BusinessException("Item " + item.getCode() + " adalah jasa (tidak dicatat di persediaan)");
        }
    }

    private static void assertPositive(BigDecimal qty) {
        if (qty == null || qty.signum() <= 0) {
            throw new BusinessException("Kuantitas harus lebih dari nol");
        }
    }

    private InventoryCostingMethod method() {
        return company().getInventoryCostingMethod();
    }

    private Company company() {
        return companyService.current();
    }

    private static LocalDate maxDate(LocalDate a, LocalDate b) {
        return a == null || b.isAfter(a) ? b : a;
    }
}
