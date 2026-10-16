package com.aji.accounting.masterdata.item;

import com.aji.accounting.common.master.AbstractMasterController;
import com.aji.accounting.common.web.PageResponse;
import com.aji.accounting.masterdata.item.ItemService.ItemOption;
import com.aji.accounting.masterdata.item.ItemService.ItemRequest;
import com.aji.accounting.masterdata.item.ItemService.ItemResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "Master - Item")
@RestController
@RequestMapping("/api/v1/items")
public class ItemController extends AbstractMasterController<ItemRequest, ItemResponse> {

    private final ItemService itemService;

    public ItemController(ItemService service) {
        super(service);
        this.itemService = service;
    }

    /** Pencarian item dengan filter tipe (menggantikan pencarian generik). */
    @GetMapping("/search")
    @PreAuthorize("hasAuthority('MASTER:VIEW')")
    public PageResponse<ItemResponse> searchItems(@RequestParam(required = false) String q,
                                                  @RequestParam(required = false) Boolean active,
                                                  @RequestParam(required = false) ItemType type,
                                                  @RequestParam(defaultValue = "0") int page,
                                                  @RequestParam(defaultValue = "20") int size) {
        return itemService.searchItems(q, active, type, page, size);
    }

    @GetMapping("/item-options")
    @PreAuthorize("isAuthenticated()")
    public List<ItemOption> itemOptions(@RequestParam(required = false) List<ItemType> types) {
        return itemService.itemOptions(types);
    }
}
