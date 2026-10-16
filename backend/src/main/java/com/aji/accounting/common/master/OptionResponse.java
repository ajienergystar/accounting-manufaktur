package com.aji.accounting.common.master;

import com.aji.accounting.common.entity.MasterEntity;

/** Item ringkas untuk dropdown/picker di frontend. */
public record OptionResponse(Long id, String code, String name) {
    public static OptionResponse from(MasterEntity e) {
        return new OptionResponse(e.getId(), e.getCode(), e.getName());
    }
}
