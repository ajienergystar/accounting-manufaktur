package com.aji.accounting.masterdata.item;

import com.aji.accounting.audit.AuditService;
import com.aji.accounting.common.exception.BusinessException;
import com.aji.accounting.common.exception.NotFoundException;
import com.aji.accounting.common.master.AbstractMasterService;
import com.aji.accounting.common.master.MasterRequest;
import com.aji.accounting.common.util.Money;
import com.aji.accounting.common.web.PageResponse;
import com.aji.accounting.masterdata.uom.UomRepository;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.List;

@Service
public class ItemService extends AbstractMasterService<Item, ItemService.ItemRequest, ItemService.ItemResponse> {

    public record ItemRequest(
            @NotBlank(message = "Kode wajib diisi") @Size(max = 30) String code,
            @NotBlank(message = "Nama wajib diisi") @Size(max = 150) String name,
            @NotNull(message = "Tipe item wajib dipilih") ItemType itemType,
            Long categoryId,
            @NotNull(message = "Satuan wajib dipilih") Long uomId,
            @DecimalMin(value = "0", message = "Tidak boleh negatif") BigDecimal standardCost,
            @DecimalMin(value = "0", message = "Tidak boleh negatif") BigDecimal sellingPrice,
            @DecimalMin(value = "0", message = "Tidak boleh negatif") BigDecimal minStock,
            @DecimalMin(value = "0", message = "Tidak boleh negatif") BigDecimal reorderPoint,
            @Size(max = 500) String description,
            Boolean active) implements MasterRequest {
    }

    public record ItemResponse(Long id, String code, String name, ItemType itemType, String itemTypeLabel,
                               Long categoryId, String categoryName, Long uomId, String uomCode,
                               BigDecimal standardCost, BigDecimal sellingPrice, BigDecimal minStock,
                               BigDecimal reorderPoint, boolean stocked, String description, boolean active) {
    }

    public record ItemOption(Long id, String code, String name, ItemType itemType, Long uomId, String uomCode,
                             BigDecimal standardCost, BigDecimal sellingPrice) {
        static ItemOption from(Item i) {
            return new ItemOption(i.getId(), i.getCode(), i.getName(), i.getItemType(), i.getUom().getId(),
                    i.getUom().getCode(), i.getStandardCost(), i.getSellingPrice());
        }
    }

    private final ItemRepository items;
    private final ItemCategoryRepository categories;
    private final UomRepository uoms;

    public ItemService(ItemRepository repository, ItemCategoryRepository categories, UomRepository uoms,
                       AuditService audit) {
        super(repository, audit);
        this.items = repository;
        this.categories = categories;
        this.uoms = uoms;
    }

    @Override
    protected String entityName() {
        return "Item";
    }

    @Override
    protected Item newEntity() {
        return new Item();
    }

    @Override
    protected void apply(ItemRequest r, Item e) {
        if (e.getId() != null && e.getItemType() != r.itemType() && hasStockMovements(e.getId())) {
            throw new BusinessException("Tipe item tidak bisa diubah karena sudah memiliki mutasi stok");
        }
        e.setItemType(r.itemType());
        e.setStocked(r.itemType() != ItemType.SERVICE);
        e.setCategory(r.categoryId() == null ? null
                : categories.findById(r.categoryId()).orElseThrow(() -> NotFoundException.of("Kategori", r.categoryId())));
        e.setUom(uoms.findById(r.uomId()).orElseThrow(() -> NotFoundException.of("Satuan", r.uomId())));
        e.setStandardCost(Money.cost(r.standardCost()));
        e.setSellingPrice(Money.amount(r.sellingPrice()));
        e.setMinStock(Money.cost(r.minStock()));
        e.setReorderPoint(Money.cost(r.reorderPoint()));
        e.setDescription(r.description());
    }

    /** Diperiksa lewat native query agar modul master tidak bergantung pada modul persediaan. */
    private boolean hasStockMovements(Long itemId) {
        Number tableExists = (Number) em.createNativeQuery(
                        "SELECT COUNT(*) FROM information_schema.tables "
                                + "WHERE table_schema = DATABASE() AND table_name = 'stock_movements'")
                .getSingleResult();
        if (tableExists.longValue() == 0) {
            return false;
        }
        Number n = (Number) em.createNativeQuery("SELECT COUNT(*) FROM stock_movements WHERE item_id = ?1")
                .setParameter(1, itemId).getSingleResult();
        return n.longValue() > 0;
    }

    @Override
    protected ItemResponse toResponse(Item e) {
        return new ItemResponse(e.getId(), e.getCode(), e.getName(), e.getItemType(), e.getItemType().label(),
                e.getCategory() == null ? null : e.getCategory().getId(),
                e.getCategory() == null ? null : e.getCategory().getName(),
                e.getUom().getId(), e.getUom().getCode(), e.getStandardCost(), e.getSellingPrice(), e.getMinStock(),
                e.getReorderPoint(), e.isStocked(), e.getDescription(), e.isActive());
    }

    @Transactional(readOnly = true)
    public PageResponse<ItemResponse> searchItems(String q, Boolean active, ItemType type, int page, int size) {
        var pageable = PageRequest.of(page, Math.min(Math.max(size, 1), 200), Sort.by("code"));
        return PageResponse.of(items.searchItems(q == null || q.isBlank() ? null : q.trim(), active, type, pageable),
                this::toResponse);
    }

    @Transactional(readOnly = true)
    public List<ItemOption> itemOptions(Collection<ItemType> types) {
        Collection<ItemType> t = types == null || types.isEmpty() ? List.of(ItemType.values()) : types;
        return items.findByActiveTrueAndItemTypeInOrderByCodeAsc(t).stream().map(ItemOption::from).toList();
    }
}
