package com.aji.accounting.masterdata.item;

/** Tipe item menentukan akun persediaan default (lihat AccountMappingKey). */
public enum ItemType {
    RAW_MATERIAL("Bahan Baku"),
    SUPPORTING_MATERIAL("Bahan Penolong"),
    WORK_IN_PROCESS("Barang Dalam Proses"),
    FINISHED_GOOD("Barang Jadi"),
    SUPPLIES("Supplies"),
    SERVICE("Jasa");

    private final String label;

    ItemType(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }
}
