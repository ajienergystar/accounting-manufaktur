package com.aji.accounting.masterdata.item;

import com.aji.accounting.audit.AuditService;
import com.aji.accounting.common.master.AbstractMasterService;
import com.aji.accounting.common.master.MasterRequest;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.stereotype.Service;

@Service
public class ItemCategoryService extends AbstractMasterService<ItemCategory, ItemCategoryService.CategoryRequest,
        ItemCategoryService.CategoryResponse> {

    public record CategoryRequest(
            @NotBlank(message = "Kode wajib diisi") @Size(max = 20) String code,
            @NotBlank(message = "Nama wajib diisi") @Size(max = 100) String name,
            Boolean active) implements MasterRequest {
    }

    public record CategoryResponse(Long id, String code, String name, boolean active) {
    }

    public ItemCategoryService(ItemCategoryRepository repository, AuditService audit) {
        super(repository, audit);
    }

    @Override
    protected String entityName() {
        return "Kategori Item";
    }

    @Override
    protected ItemCategory newEntity() {
        return new ItemCategory();
    }

    @Override
    protected void apply(CategoryRequest req, ItemCategory entity) {
        // tidak ada field tambahan
    }

    @Override
    protected CategoryResponse toResponse(ItemCategory e) {
        return new CategoryResponse(e.getId(), e.getCode(), e.getName(), e.isActive());
    }
}
