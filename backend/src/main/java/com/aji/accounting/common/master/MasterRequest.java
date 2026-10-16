package com.aji.accounting.common.master;

/** Kontrak minimal request master data. */
public interface MasterRequest {
    String code();

    String name();

    Boolean active();
}
