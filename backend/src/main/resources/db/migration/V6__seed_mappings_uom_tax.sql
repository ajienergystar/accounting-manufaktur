-- =====================================================================
-- V6: Seed account mapping default untuk jurnal otomatis
-- =====================================================================

INSERT INTO account_mappings (mapping_key, account_id, group_name, description, created_at, created_by)
SELECT m.mapping_key, a.id, m.group_name, m.description, NOW(6), 'system'
FROM (
    SELECT 'INVENTORY_RAW_MATERIAL' AS mapping_key, '1-1400' AS code, 'Persediaan' AS group_name, 'Persediaan bahan baku' AS description
    UNION ALL
    SELECT 'INVENTORY_SUPPORTING_MATERIAL' AS mapping_key, '1-1410' AS code, 'Persediaan' AS group_name, 'Persediaan bahan penolong' AS description
    UNION ALL
    SELECT 'INVENTORY_WIP' AS mapping_key, '1-1420' AS code, 'Persediaan' AS group_name, 'Barang dalam proses (BDP)' AS description
    UNION ALL
    SELECT 'INVENTORY_FINISHED_GOOD' AS mapping_key, '1-1430' AS code, 'Persediaan' AS group_name, 'Persediaan barang jadi' AS description
    UNION ALL
    SELECT 'INVENTORY_SUPPLIES' AS mapping_key, '1-1440' AS code, 'Persediaan' AS group_name, 'Persediaan supplies pabrik' AS description
    UNION ALL
    SELECT 'INVENTORY_ADJUSTMENT' AS mapping_key, '5-7000' AS code, 'Persediaan' AS group_name, 'Selisih stock opname / penyesuaian persediaan' AS description
    UNION ALL
    SELECT 'ACCOUNTS_RECEIVABLE' AS mapping_key, '1-1300' AS code, 'Penjualan & Piutang' AS group_name, 'Piutang usaha' AS description
    UNION ALL
    SELECT 'ALLOWANCE_DOUBTFUL' AS mapping_key, '1-1310' AS code, 'Penjualan & Piutang' AS group_name, 'Cadangan kerugian piutang' AS description
    UNION ALL
    SELECT 'BAD_DEBT_EXPENSE' AS mapping_key, '6-2300' AS code, 'Penjualan & Piutang' AS group_name, 'Beban kerugian piutang' AS description
    UNION ALL
    SELECT 'SALES' AS mapping_key, '4-1000' AS code, 'Penjualan & Piutang' AS group_name, 'Pendapatan penjualan' AS description
    UNION ALL
    SELECT 'SALES_RETURN' AS mapping_key, '4-1100' AS code, 'Penjualan & Piutang' AS group_name, 'Retur penjualan' AS description
    UNION ALL
    SELECT 'SALES_DISCOUNT' AS mapping_key, '4-1200' AS code, 'Penjualan & Piutang' AS group_name, 'Potongan penjualan' AS description
    UNION ALL
    SELECT 'SALES_ADVANCE' AS mapping_key, '2-1500' AS code, 'Penjualan & Piutang' AS group_name, 'Uang muka penjualan' AS description
    UNION ALL
    SELECT 'COGS' AS mapping_key, '5-1000' AS code, 'Penjualan & Piutang' AS group_name, 'Harga pokok penjualan' AS description
    UNION ALL
    SELECT 'ACCOUNTS_PAYABLE' AS mapping_key, '2-1100' AS code, 'Pembelian & Utang' AS group_name, 'Utang usaha' AS description
    UNION ALL
    SELECT 'GRNI' AS mapping_key, '2-1700' AS code, 'Pembelian & Utang' AS group_name, 'Barang diterima belum ditagih' AS description
    UNION ALL
    SELECT 'PURCHASE_ADVANCE' AS mapping_key, '1-1600' AS code, 'Pembelian & Utang' AS group_name, 'Uang muka pembelian' AS description
    UNION ALL
    SELECT 'PPN_INPUT' AS mapping_key, '1-1500' AS code, 'Pajak' AS group_name, 'PPN masukan' AS description
    UNION ALL
    SELECT 'PPN_OUTPUT' AS mapping_key, '2-1300' AS code, 'Pajak' AS group_name, 'PPN keluaran' AS description
    UNION ALL
    SELECT 'PPN_PAYABLE' AS mapping_key, '2-1800' AS code, 'Pajak' AS group_name, 'Utang PPN (kurang bayar)' AS description
    UNION ALL
    SELECT 'PPH_PREPAID' AS mapping_key, '1-1510' AS code, 'Pajak' AS group_name, 'PPh dibayar di muka (dipotong pelanggan)' AS description
    UNION ALL
    SELECT 'PPH_PAYABLE' AS mapping_key, '2-1400' AS code, 'Pajak' AS group_name, 'Utang PPh 21/23 (dipotong dari pemasok/karyawan)' AS description
    UNION ALL
    SELECT 'INCOME_TAX_EXPENSE' AS mapping_key, '8-1000' AS code, 'Pajak' AS group_name, 'Beban pajak penghasilan' AS description
    UNION ALL
    SELECT 'INCOME_TAX_PAYABLE' AS mapping_key, '2-1900' AS code, 'Pajak' AS group_name, 'Utang pajak penghasilan badan' AS description
    UNION ALL
    SELECT 'WAGES_PAYABLE' AS mapping_key, '2-1200' AS code, 'Produksi & Biaya' AS group_name, 'Utang gaji & upah' AS description
    UNION ALL
    SELECT 'PAYROLL_CLEARING' AS mapping_key, '5-9000' AS code, 'Produksi & Biaya' AS group_name, 'Gaji & upah (clearing payroll)' AS description
    UNION ALL
    SELECT 'FOH_SUPPORTING_MATERIAL' AS mapping_key, '5-2100' AS code, 'Produksi & Biaya' AS group_name, 'BOP aktual - bahan penolong' AS description
    UNION ALL
    SELECT 'FOH_INDIRECT_LABOR' AS mapping_key, '5-2200' AS code, 'Produksi & Biaya' AS group_name, 'BOP aktual - tenaga kerja tidak langsung' AS description
    UNION ALL
    SELECT 'FOH_DEPRECIATION' AS mapping_key, '5-2400' AS code, 'Produksi & Biaya' AS group_name, 'BOP aktual - penyusutan pabrik' AS description
    UNION ALL
    SELECT 'FOH_OTHER' AS mapping_key, '5-2600' AS code, 'Produksi & Biaya' AS group_name, 'BOP aktual - lain-lain' AS description
    UNION ALL
    SELECT 'FOH_ACTUAL_CONTROL' AS mapping_key, '5-2000' AS code, 'Produksi & Biaya' AS group_name, 'Akun kontrol BOP aktual (header, untuk laporan)' AS description
    UNION ALL
    SELECT 'FOH_APPLIED' AS mapping_key, '5-3000' AS code, 'Produksi & Biaya' AS group_name, 'BOP dibebankan' AS description
    UNION ALL
    SELECT 'FOH_VARIANCE' AS mapping_key, '5-4000' AS code, 'Produksi & Biaya' AS group_name, 'Selisih BOP' AS description
    UNION ALL
    SELECT 'MATERIAL_PRICE_VARIANCE' AS mapping_key, '5-5000' AS code, 'Produksi & Biaya' AS group_name, 'Selisih harga bahan' AS description
    UNION ALL
    SELECT 'MATERIAL_QUANTITY_VARIANCE' AS mapping_key, '5-5100' AS code, 'Produksi & Biaya' AS group_name, 'Selisih kuantitas bahan' AS description
    UNION ALL
    SELECT 'LABOR_RATE_VARIANCE' AS mapping_key, '5-5200' AS code, 'Produksi & Biaya' AS group_name, 'Selisih tarif tenaga kerja' AS description
    UNION ALL
    SELECT 'LABOR_EFFICIENCY_VARIANCE' AS mapping_key, '5-5300' AS code, 'Produksi & Biaya' AS group_name, 'Selisih efisiensi tenaga kerja' AS description
    UNION ALL
    SELECT 'ABNORMAL_SPOILAGE' AS mapping_key, '5-6000' AS code, 'Produksi & Biaya' AS group_name, 'Rugi produk rusak abnormal' AS description
    UNION ALL
    SELECT 'OFFICE_SALARY' AS mapping_key, '6-2100' AS code, 'Beban' AS group_name, 'Beban gaji kantor' AS description
    UNION ALL
    SELECT 'OFFICE_DEPRECIATION' AS mapping_key, '6-2200' AS code, 'Beban' AS group_name, 'Beban penyusutan kantor' AS description
    UNION ALL
    SELECT 'RETAINED_EARNINGS' AS mapping_key, '3-2000' AS code, 'Ekuitas & Tutup Buku' AS group_name, 'Laba ditahan' AS description
    UNION ALL
    SELECT 'CURRENT_YEAR_EARNINGS' AS mapping_key, '3-3000' AS code, 'Ekuitas & Tutup Buku' AS group_name, 'Laba tahun berjalan' AS description
    UNION ALL
    SELECT 'OPENING_BALANCE_EQUITY' AS mapping_key, '3-9000' AS code, 'Ekuitas & Tutup Buku' AS group_name, 'Ekuitas saldo awal (lawan saldo awal)' AS description
    UNION ALL
    SELECT 'ASSET_DISPOSAL_GAIN_LOSS' AS mapping_key, '7-3000' AS code, 'Lain-lain' AS group_name, 'Laba/rugi pelepasan aset' AS description
    UNION ALL
    SELECT 'ROUNDING' AS mapping_key, '7-4000' AS code, 'Lain-lain' AS group_name, 'Selisih pembulatan' AS description
    UNION ALL
    SELECT 'BANK_CHARGES' AS mapping_key, '7-5000' AS code, 'Lain-lain' AS group_name, 'Beban administrasi bank' AS description
    UNION ALL
    SELECT 'OTHER_INCOME' AS mapping_key, '7-1000' AS code, 'Lain-lain' AS group_name, 'Pendapatan lain-lain' AS description
) m
JOIN accounts a ON a.code = m.code;

-- Satuan standar
INSERT INTO uoms (code, name, created_at, created_by) VALUES
('PCS', 'Pieces', NOW(6), 'system'), ('UNIT', 'Unit', NOW(6), 'system'), ('KG', 'Kilogram', NOW(6), 'system'),
('GR', 'Gram', NOW(6), 'system'), ('LTR', 'Liter', NOW(6), 'system'), ('MTR', 'Meter', NOW(6), 'system'),
('M2', 'Meter Persegi', NOW(6), 'system'), ('BOX', 'Box', NOW(6), 'system'), ('SET', 'Set', NOW(6), 'system'),
('ROLL', 'Roll', NOW(6), 'system'), ('LBR', 'Lembar', NOW(6), 'system'), ('JAM', 'Jam', NOW(6), 'system');

INSERT INTO uom_conversions (from_uom_id, to_uom_id, factor, created_at, created_by)
SELECT f.id, t.id, 1000, NOW(6), 'system' FROM uoms f JOIN uoms t ON f.code = 'KG' AND t.code = 'GR';

-- Tarif pajak default (dapat diubah)
INSERT INTO tax_rates (code, name, tax_type, rate, account_id, created_at, created_by)
SELECT 'PPN11', 'PPN 11%', 'PPN', 11.0000, NULL, NOW(6), 'system' UNION ALL
SELECT 'PPN12', 'PPN 12% (barang mewah)', 'PPN', 12.0000, NULL, NOW(6), 'system' UNION ALL
SELECT 'PPH23', 'PPh 23 Jasa 2%', 'PPH23', 2.0000, NULL, NOW(6), 'system' UNION ALL
SELECT 'PPH22', 'PPh 22 Impor/Pembelian 1,5%', 'PPH22', 1.5000, NULL, NOW(6), 'system';

-- Kategori item default
INSERT INTO item_categories (code, name, created_at, created_by) VALUES
('BB', 'Bahan Baku', NOW(6), 'system'), ('BP', 'Bahan Penolong', NOW(6), 'system'),
('BJ', 'Barang Jadi', NOW(6), 'system'), ('SP', 'Supplies & Sparepart', NOW(6), 'system');
