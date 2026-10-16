package com.aji.accounting.masterdata.item;

import com.aji.accounting.common.master.AbstractMasterController;
import com.aji.accounting.masterdata.item.ItemCategoryService.CategoryRequest;
import com.aji.accounting.masterdata.item.ItemCategoryService.CategoryResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Master - Kategori Item")
@RestController
@RequestMapping("/api/v1/item-categories")
public class ItemCategoryController extends AbstractMasterController<CategoryRequest, CategoryResponse> {
    public ItemCategoryController(ItemCategoryService service) {
        super(service);
    }
}
