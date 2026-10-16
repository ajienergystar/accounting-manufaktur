package com.aji.accounting.inventory;

import com.aji.accounting.masterdata.item.ItemType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public final class InventoryDtos {
    private InventoryDtos() {
    }

    // ------------------------------------------------------------- penyesuaian
    public record AdjustmentLineRequest(@NotNull Long itemId,
                                        @NotNull(message = "Qty wajib diisi") BigDecimal qtyChange,
                                        @DecimalMin("0") BigDecimal unitCost,
                                        @Size(max = 255) String note) {
    }

    public record AdjustmentRequest(@NotNull LocalDate docDate,
                                    @NotNull Long warehouseId,
                                    @NotNull StockAdjustment.Type adjustmentType,
                                    @NotBlank(message = "Alasan wajib diisi") @Size(max = 500) String reason,
                                    @NotEmpty List<@Valid AdjustmentLineRequest> lines) {
    }

    public record AdjustmentLineResponse(Long itemId, String itemCode, String itemName, String uomCode,
                                         BigDecimal qtyChange, BigDecimal unitCost, BigDecimal totalValue, String note) {
    }

    public record AdjustmentResponse(Long id, String docNo, LocalDate docDate, Long warehouseId, String warehouseName,
                                     StockAdjustment.Type adjustmentType, String reason, DocStatus status,
                                     BigDecimal totalValue, Long journalId, List<AdjustmentLineResponse> lines) {
    }

    // ------------------------------------------------------------- transfer
    public record TransferLineRequest(@NotNull Long itemId, @NotNull @DecimalMin(value = "0.0001") BigDecimal qty) {
    }

    public record TransferRequest(@NotNull LocalDate docDate, @NotNull Long fromWarehouseId,
                                  @NotNull Long toWarehouseId, @Size(max = 500) String notes,
                                  @NotEmpty List<@Valid TransferLineRequest> lines) {
    }

    public record TransferLineResponse(Long itemId, String itemCode, String itemName, String uomCode, BigDecimal qty,
                                       BigDecimal unitCost, BigDecimal totalValue) {
    }

    public record TransferResponse(Long id, String docNo, LocalDate docDate, Long fromWarehouseId,
                                   String fromWarehouseName, Long toWarehouseId, String toWarehouseName, String notes,
                                   DocStatus status, BigDecimal totalValue, List<TransferLineResponse> lines) {
    }

    // ------------------------------------------------------------- opname
    public record OpnameCreateRequest(@NotNull LocalDate docDate, @NotNull Long warehouseId,
                                      @Size(max = 500) String notes, boolean includeZeroStock) {
    }

    public record OpnameCountLine(@NotNull Long itemId, BigDecimal countedQty, @Size(max = 255) String note) {
    }

    public record OpnameCountRequest(@NotEmpty List<@Valid OpnameCountLine> lines) {
    }

    public record OpnameLineResponse(Long itemId, String itemCode, String itemName, String uomCode,
                                     BigDecimal systemQty, BigDecimal countedQty, BigDecimal diffQty,
                                     BigDecimal unitCost, BigDecimal diffValue, String note) {
    }

    public record OpnameResponse(Long id, String docNo, LocalDate docDate, Long warehouseId, String warehouseName,
                                 String notes, StockOpname.Status status, Long adjustmentId, String adjustmentNo,
                                 int countedLines, int totalLines, BigDecimal totalDiffValue,
                                 List<OpnameLineResponse> lines) {
    }

    // ------------------------------------------------------------- query
    public record StockRow(Long itemId, String itemCode, String itemName, ItemType itemType, String uomCode,
                           Long warehouseId, String warehouseName, BigDecimal qty, BigDecimal avgCost,
                           BigDecimal totalValue, BigDecimal minStock, boolean belowMinimum) {
    }

    public record StockCardLine(Long movementId, LocalDate date, String sourceType, String sourceTypeLabel,
                                Long sourceId, String sourceNo, String description, BigDecimal qtyIn,
                                BigDecimal qtyOut, BigDecimal unitCost, BigDecimal valueIn, BigDecimal valueOut,
                                BigDecimal balanceQty, BigDecimal balanceValue) {
    }

    public record StockCard(Long itemId, String itemCode, String itemName, String uomCode, Long warehouseId,
                            String warehouseName, LocalDate from, LocalDate to, BigDecimal openingQty,
                            BigDecimal openingValue, BigDecimal totalInQty, BigDecimal totalOutQty,
                            BigDecimal closingQty, BigDecimal closingValue, List<StockCardLine> lines) {
    }

    public record ValuationRow(ItemType itemType, String itemTypeLabel, String accountCode, String accountName,
                               BigDecimal stockValue, BigDecimal ledgerBalance, BigDecimal difference) {
    }
}
