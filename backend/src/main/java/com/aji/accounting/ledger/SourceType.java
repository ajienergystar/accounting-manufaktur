package com.aji.accounting.ledger;

/** Dokumen sumber dari sebuah jurnal (untuk drill-down laporan → jurnal → dokumen). */
public enum SourceType {
    MANUAL("Jurnal manual"),
    RECURRING("Jurnal berulang"),
    OPENING_BALANCE("Saldo awal"),
    GOODS_RECEIPT("Penerimaan barang"),
    PURCHASE_INVOICE("Faktur pembelian"),
    PURCHASE_RETURN("Retur pembelian"),
    AP_PAYMENT("Pembayaran utang"),
    PURCHASE_ADVANCE("Uang muka pembelian"),
    DELIVERY_ORDER("Pengiriman barang"),
    SALES_INVOICE("Faktur penjualan"),
    SALES_RETURN("Retur penjualan"),
    AR_RECEIPT("Penerimaan piutang"),
    SALES_ADVANCE("Uang muka penjualan"),
    BAD_DEBT("Penyisihan piutang"),
    STOCK_ADJUSTMENT("Penyesuaian stok"),
    STOCK_OPNAME("Stock opname"),
    STOCK_TRANSFER("Transfer gudang"),
    MATERIAL_ISSUE("Pemakaian bahan"),
    MATERIAL_RETURN("Retur bahan ke gudang"),
    LABOR_ENTRY("Tenaga kerja langsung"),
    PAYROLL("Penggajian"),
    OVERHEAD_ACTUAL("BOP aktual"),
    OVERHEAD_APPLIED("BOP dibebankan"),
    SERVICE_DEPT_ALLOCATION("Alokasi departemen pendukung"),
    PRODUCTION_COMPLETION("Penyelesaian produksi"),
    SPOILAGE("Produk rusak"),
    COST_VARIANCE("Selisih biaya standar"),
    WORK_ORDER_CLOSE("Penutupan work order"),
    CASH_TRANSACTION("Transaksi kas/bank"),
    BANK_TRANSFER("Transfer antar rekening"),
    BANK_RECONCILIATION("Rekonsiliasi bank"),
    ASSET_ACQUISITION("Perolehan aset"),
    DEPRECIATION("Penyusutan"),
    ASSET_DISPOSAL("Pelepasan aset"),
    TAX_SETTLEMENT("Kompensasi PPN"),
    FOH_VARIANCE_CLOSING("Penutupan selisih BOP"),
    YEAR_END_CLOSING("Tutup buku tahunan");

    private final String label;

    SourceType(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }
}
