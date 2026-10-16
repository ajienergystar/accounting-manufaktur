package com.aji.accounting.inventory;

import com.aji.accounting.common.web.PageResponse;
import com.aji.accounting.inventory.InventoryDtos.AdjustmentRequest;
import com.aji.accounting.inventory.InventoryDtos.AdjustmentResponse;
import com.aji.accounting.inventory.InventoryDtos.OpnameCountRequest;
import com.aji.accounting.inventory.InventoryDtos.OpnameCreateRequest;
import com.aji.accounting.inventory.InventoryDtos.OpnameResponse;
import com.aji.accounting.inventory.InventoryDtos.StockCard;
import com.aji.accounting.inventory.InventoryDtos.StockRow;
import com.aji.accounting.inventory.InventoryDtos.TransferRequest;
import com.aji.accounting.inventory.InventoryDtos.TransferResponse;
import com.aji.accounting.inventory.InventoryDtos.ValuationRow;
import com.aji.accounting.ledger.JournalDtos.ReverseRequest;
import com.aji.accounting.masterdata.item.ItemType;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@Tag(name = "Persediaan")
@RestController
@RequestMapping("/api/v1/inventory")
@RequiredArgsConstructor
public class InventoryController {

    private final StockQueryService query;
    private final StockAdjustmentService adjustments;
    private final StockTransferService transfers;
    private final StockOpnameService opnames;

    // ---------------------------------------------------------------- query
    @GetMapping("/stock")
    @PreAuthorize("hasAuthority('INVENTORY:VIEW')")
    public PageResponse<StockRow> stock(@RequestParam(required = false) Long warehouseId,
                                        @RequestParam(required = false) ItemType type,
                                        @RequestParam(required = false) String q,
                                        @RequestParam(defaultValue = "true") boolean nonZero,
                                        @RequestParam(defaultValue = "false") boolean lowOnly,
                                        @RequestParam(defaultValue = "0") int page,
                                        @RequestParam(defaultValue = "50") int size) {
        return query.stock(warehouseId, type, q, nonZero, lowOnly, page, size);
    }

    @GetMapping("/stock-card")
    @PreAuthorize("hasAuthority('INVENTORY:VIEW')")
    public StockCard stockCard(@RequestParam Long itemId, @RequestParam Long warehouseId,
                               @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                               @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return query.stockCard(itemId, warehouseId, from, to);
    }

    @GetMapping("/valuation")
    @PreAuthorize("hasAuthority('INVENTORY:VIEW')")
    public List<ValuationRow> valuation(@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate asOf) {
        return query.valuation(asOf);
    }

    // ---------------------------------------------------------------- penyesuaian
    @GetMapping("/adjustments")
    @PreAuthorize("hasAuthority('INVENTORY:VIEW')")
    public PageResponse<AdjustmentResponse> adjustments(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) String q,
            @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
        return adjustments.search(from, to, q, page, size);
    }

    @GetMapping("/adjustments/{id}")
    @PreAuthorize("hasAuthority('INVENTORY:VIEW')")
    public AdjustmentResponse adjustment(@PathVariable Long id) {
        return adjustments.get(id);
    }

    @PostMapping("/adjustments")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('INVENTORY:POST') or (hasAuthority('INVENTORY:CREATE') and #req.adjustmentType().name() != 'OPENING')")
    public AdjustmentResponse createAdjustment(@Valid @RequestBody AdjustmentRequest req) {
        return adjustments.create(req);
    }

    @PostMapping("/adjustments/{id}/void")
    @PreAuthorize("hasAuthority('INVENTORY:POST')")
    public AdjustmentResponse voidAdjustment(@PathVariable Long id, @Valid @RequestBody ReverseRequest req) {
        return adjustments.voidDoc(id, req.date(), req.reason());
    }

    // ---------------------------------------------------------------- transfer
    @GetMapping("/transfers")
    @PreAuthorize("hasAuthority('INVENTORY:VIEW')")
    public PageResponse<TransferResponse> transfers(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) String q,
            @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
        return transfers.search(from, to, q, page, size);
    }

    @GetMapping("/transfers/{id}")
    @PreAuthorize("hasAuthority('INVENTORY:VIEW')")
    public TransferResponse transfer(@PathVariable Long id) {
        return transfers.get(id);
    }

    @PostMapping("/transfers")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('INVENTORY:CREATE')")
    public TransferResponse createTransfer(@Valid @RequestBody TransferRequest req) {
        return transfers.create(req);
    }

    @PostMapping("/transfers/{id}/void")
    @PreAuthorize("hasAuthority('INVENTORY:POST')")
    public TransferResponse voidTransfer(@PathVariable Long id, @Valid @RequestBody ReverseRequest req) {
        return transfers.voidDoc(id, req.date(), req.reason());
    }

    // ---------------------------------------------------------------- opname
    @GetMapping("/opnames")
    @PreAuthorize("hasAuthority('INVENTORY:VIEW')")
    public PageResponse<OpnameResponse> opnames(@RequestParam(defaultValue = "0") int page,
                                                @RequestParam(defaultValue = "20") int size) {
        return opnames.list(page, size);
    }

    @GetMapping("/opnames/{id}")
    @PreAuthorize("hasAuthority('INVENTORY:VIEW')")
    public OpnameResponse opname(@PathVariable Long id) {
        return opnames.get(id);
    }

    @PostMapping("/opnames")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('INVENTORY:CREATE')")
    public OpnameResponse createOpname(@Valid @RequestBody OpnameCreateRequest req) {
        return opnames.create(req);
    }

    @PutMapping("/opnames/{id}/counts")
    @PreAuthorize("hasAuthority('INVENTORY:EDIT')")
    public OpnameResponse saveCounts(@PathVariable Long id, @Valid @RequestBody OpnameCountRequest req) {
        return opnames.saveCounts(id, req);
    }

    @PostMapping("/opnames/{id}/post")
    @PreAuthorize("hasAuthority('INVENTORY:POST')")
    public OpnameResponse postOpname(@PathVariable Long id) {
        return opnames.post(id);
    }

    @PostMapping("/opnames/{id}/void")
    @PreAuthorize("hasAuthority('INVENTORY:POST')")
    public OpnameResponse voidOpname(@PathVariable Long id, @Valid @RequestBody ReverseRequest req) {
        return opnames.voidDoc(id, req.date(), req.reason());
    }

    @DeleteMapping("/opnames/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAuthority('INVENTORY:EDIT')")
    public void deleteOpname(@PathVariable Long id) {
        opnames.deleteDraft(id);
    }
}
