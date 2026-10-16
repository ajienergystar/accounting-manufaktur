-- =====================================================================
-- V5: Seed Bagan Akun (Chart of Accounts) standar perusahaan manufaktur
-- Struktur kode X-XXXX; akun header hanya untuk pengelompokan (tidak bisa dijurnal)
-- =====================================================================

INSERT INTO accounts (code, name, account_type, normal_balance, is_header, is_cash_bank, cash_flow_category, level, created_at, created_by) VALUES
('1-0000', 'ASET', 'ASSET', 'DEBIT', TRUE, FALSE, NULL, 1, NOW(6), 'system'),
('1-1000', 'Aset Lancar', 'ASSET', 'DEBIT', TRUE, FALSE, NULL, 2, NOW(6), 'system'),
('1-1100', 'Kas', 'ASSET', 'DEBIT', FALSE, TRUE, 'CASH', 3, NOW(6), 'system'),
('1-1200', 'Bank', 'ASSET', 'DEBIT', FALSE, TRUE, 'CASH', 3, NOW(6), 'system'),
('1-1300', 'Piutang Usaha', 'ASSET', 'DEBIT', FALSE, FALSE, 'OPERATING', 3, NOW(6), 'system'),
('1-1310', 'Cadangan Kerugian Piutang', 'ASSET', 'CREDIT', FALSE, FALSE, 'OPERATING', 3, NOW(6), 'system'),
('1-1400', 'Persediaan Bahan Baku', 'ASSET', 'DEBIT', FALSE, FALSE, 'OPERATING', 3, NOW(6), 'system'),
('1-1410', 'Persediaan Bahan Penolong', 'ASSET', 'DEBIT', FALSE, FALSE, 'OPERATING', 3, NOW(6), 'system'),
('1-1420', 'Persediaan Barang Dalam Proses (BDP)', 'ASSET', 'DEBIT', FALSE, FALSE, 'OPERATING', 3, NOW(6), 'system'),
('1-1430', 'Persediaan Barang Jadi', 'ASSET', 'DEBIT', FALSE, FALSE, 'OPERATING', 3, NOW(6), 'system'),
('1-1440', 'Persediaan Supplies Pabrik', 'ASSET', 'DEBIT', FALSE, FALSE, 'OPERATING', 3, NOW(6), 'system'),
('1-1500', 'PPN Masukan', 'ASSET', 'DEBIT', FALSE, FALSE, 'OPERATING', 3, NOW(6), 'system'),
('1-1510', 'PPh Dibayar di Muka', 'ASSET', 'DEBIT', FALSE, FALSE, 'OPERATING', 3, NOW(6), 'system'),
('1-1600', 'Uang Muka Pembelian', 'ASSET', 'DEBIT', FALSE, FALSE, 'OPERATING', 3, NOW(6), 'system'),
('1-1700', 'Biaya Dibayar di Muka', 'ASSET', 'DEBIT', FALSE, FALSE, 'OPERATING', 3, NOW(6), 'system'),
('1-2000', 'Aset Tetap', 'ASSET', 'DEBIT', TRUE, FALSE, NULL, 2, NOW(6), 'system'),
('1-2100', 'Tanah', 'ASSET', 'DEBIT', FALSE, FALSE, 'INVESTING', 3, NOW(6), 'system'),
('1-2200', 'Bangunan Pabrik', 'ASSET', 'DEBIT', FALSE, FALSE, 'INVESTING', 3, NOW(6), 'system'),
('1-2210', 'Akumulasi Penyusutan Bangunan Pabrik', 'ASSET', 'CREDIT', FALSE, FALSE, 'INVESTING', 3, NOW(6), 'system'),
('1-2300', 'Mesin & Peralatan Pabrik', 'ASSET', 'DEBIT', FALSE, FALSE, 'INVESTING', 3, NOW(6), 'system'),
('1-2310', 'Akumulasi Penyusutan Mesin', 'ASSET', 'CREDIT', FALSE, FALSE, 'INVESTING', 3, NOW(6), 'system'),
('1-2400', 'Kendaraan', 'ASSET', 'DEBIT', FALSE, FALSE, 'INVESTING', 3, NOW(6), 'system'),
('1-2410', 'Akumulasi Penyusutan Kendaraan', 'ASSET', 'CREDIT', FALSE, FALSE, 'INVESTING', 3, NOW(6), 'system'),
('1-2500', 'Peralatan Kantor', 'ASSET', 'DEBIT', FALSE, FALSE, 'INVESTING', 3, NOW(6), 'system'),
('1-2510', 'Akumulasi Penyusutan Peralatan Kantor', 'ASSET', 'CREDIT', FALSE, FALSE, 'INVESTING', 3, NOW(6), 'system'),
('2-0000', 'LIABILITAS', 'LIABILITY', 'CREDIT', TRUE, FALSE, NULL, 1, NOW(6), 'system'),
('2-1000', 'Liabilitas Jangka Pendek', 'LIABILITY', 'CREDIT', TRUE, FALSE, NULL, 2, NOW(6), 'system'),
('2-1100', 'Utang Usaha', 'LIABILITY', 'CREDIT', FALSE, FALSE, 'OPERATING', 3, NOW(6), 'system'),
('2-1200', 'Utang Gaji & Upah', 'LIABILITY', 'CREDIT', FALSE, FALSE, 'OPERATING', 3, NOW(6), 'system'),
('2-1300', 'PPN Keluaran', 'LIABILITY', 'CREDIT', FALSE, FALSE, 'OPERATING', 3, NOW(6), 'system'),
('2-1400', 'Utang PPh 21 / 23', 'LIABILITY', 'CREDIT', FALSE, FALSE, 'OPERATING', 3, NOW(6), 'system'),
('2-1500', 'Uang Muka Penjualan', 'LIABILITY', 'CREDIT', FALSE, FALSE, 'OPERATING', 3, NOW(6), 'system'),
('2-1600', 'Biaya Masih Harus Dibayar', 'LIABILITY', 'CREDIT', FALSE, FALSE, 'OPERATING', 3, NOW(6), 'system'),
('2-1700', 'Barang Diterima Belum Ditagih (GRNI)', 'LIABILITY', 'CREDIT', FALSE, FALSE, 'OPERATING', 3, NOW(6), 'system'),
('2-1800', 'Utang PPN (Kurang Bayar)', 'LIABILITY', 'CREDIT', FALSE, FALSE, 'OPERATING', 3, NOW(6), 'system'),
('2-1900', 'Utang Pajak Penghasilan Badan', 'LIABILITY', 'CREDIT', FALSE, FALSE, 'OPERATING', 3, NOW(6), 'system'),
('2-2000', 'Liabilitas Jangka Panjang', 'LIABILITY', 'CREDIT', TRUE, FALSE, NULL, 2, NOW(6), 'system'),
('2-2100', 'Utang Bank', 'LIABILITY', 'CREDIT', FALSE, FALSE, 'FINANCING', 3, NOW(6), 'system'),
('3-0000', 'EKUITAS', 'EQUITY', 'CREDIT', TRUE, FALSE, NULL, 1, NOW(6), 'system'),
('3-1000', 'Modal Disetor', 'EQUITY', 'CREDIT', FALSE, FALSE, 'FINANCING', 2, NOW(6), 'system'),
('3-2000', 'Laba Ditahan', 'EQUITY', 'CREDIT', FALSE, FALSE, NULL, 2, NOW(6), 'system'),
('3-3000', 'Laba Tahun Berjalan', 'EQUITY', 'CREDIT', FALSE, FALSE, NULL, 2, NOW(6), 'system'),
('3-4000', 'Prive / Dividen', 'EQUITY', 'DEBIT', FALSE, FALSE, 'FINANCING', 2, NOW(6), 'system'),
('3-9000', 'Ekuitas Saldo Awal', 'EQUITY', 'CREDIT', FALSE, FALSE, NULL, 2, NOW(6), 'system'),
('4-0000', 'PENDAPATAN', 'REVENUE', 'CREDIT', TRUE, FALSE, NULL, 1, NOW(6), 'system'),
('4-1000', 'Penjualan', 'REVENUE', 'CREDIT', FALSE, FALSE, NULL, 2, NOW(6), 'system'),
('4-1100', 'Retur Penjualan', 'REVENUE', 'DEBIT', FALSE, FALSE, NULL, 2, NOW(6), 'system'),
('4-1200', 'Potongan Penjualan', 'REVENUE', 'DEBIT', FALSE, FALSE, NULL, 2, NOW(6), 'system'),
('5-0000', 'HARGA POKOK', 'COST_OF_GOODS', 'DEBIT', TRUE, FALSE, NULL, 1, NOW(6), 'system'),
('5-1000', 'Harga Pokok Penjualan', 'COST_OF_GOODS', 'DEBIT', FALSE, FALSE, NULL, 2, NOW(6), 'system'),
('5-2000', 'Biaya Overhead Pabrik Aktual (Kontrol)', 'COST_OF_GOODS', 'DEBIT', TRUE, FALSE, NULL, 2, NOW(6), 'system'),
('5-2100', 'Bahan Penolong', 'COST_OF_GOODS', 'DEBIT', FALSE, FALSE, NULL, 3, NOW(6), 'system'),
('5-2200', 'Tenaga Kerja Tidak Langsung', 'COST_OF_GOODS', 'DEBIT', FALSE, FALSE, NULL, 3, NOW(6), 'system'),
('5-2300', 'Listrik & Air Pabrik', 'COST_OF_GOODS', 'DEBIT', FALSE, FALSE, NULL, 3, NOW(6), 'system'),
('5-2400', 'Penyusutan Mesin & Bangunan Pabrik', 'COST_OF_GOODS', 'DEBIT', FALSE, FALSE, NULL, 3, NOW(6), 'system'),
('5-2500', 'Pemeliharaan Mesin', 'COST_OF_GOODS', 'DEBIT', FALSE, FALSE, NULL, 3, NOW(6), 'system'),
('5-2600', 'BOP Lain-lain', 'COST_OF_GOODS', 'DEBIT', FALSE, FALSE, NULL, 3, NOW(6), 'system'),
('5-3000', 'BOP Dibebankan', 'COST_OF_GOODS', 'CREDIT', FALSE, FALSE, NULL, 2, NOW(6), 'system'),
('5-4000', 'Selisih BOP', 'COST_OF_GOODS', 'DEBIT', FALSE, FALSE, NULL, 2, NOW(6), 'system'),
('5-5000', 'Selisih Harga Bahan', 'COST_OF_GOODS', 'DEBIT', FALSE, FALSE, NULL, 2, NOW(6), 'system'),
('5-5100', 'Selisih Kuantitas Bahan', 'COST_OF_GOODS', 'DEBIT', FALSE, FALSE, NULL, 2, NOW(6), 'system'),
('5-5200', 'Selisih Tarif Tenaga Kerja', 'COST_OF_GOODS', 'DEBIT', FALSE, FALSE, NULL, 2, NOW(6), 'system'),
('5-5300', 'Selisih Efisiensi Tenaga Kerja', 'COST_OF_GOODS', 'DEBIT', FALSE, FALSE, NULL, 2, NOW(6), 'system'),
('5-6000', 'Rugi Produk Rusak Abnormal', 'COST_OF_GOODS', 'DEBIT', FALSE, FALSE, NULL, 2, NOW(6), 'system'),
('5-7000', 'Selisih Penyesuaian Persediaan', 'COST_OF_GOODS', 'DEBIT', FALSE, FALSE, NULL, 2, NOW(6), 'system'),
('5-9000', 'Gaji & Upah (Clearing)', 'COST_OF_GOODS', 'DEBIT', FALSE, FALSE, NULL, 2, NOW(6), 'system'),
('6-0000', 'BEBAN OPERASIONAL', 'EXPENSE', 'DEBIT', TRUE, FALSE, NULL, 1, NOW(6), 'system'),
('6-1000', 'Beban Penjualan', 'EXPENSE', 'DEBIT', FALSE, FALSE, NULL, 2, NOW(6), 'system'),
('6-2000', 'Beban Administrasi & Umum', 'EXPENSE', 'DEBIT', TRUE, FALSE, NULL, 2, NOW(6), 'system'),
('6-2100', 'Beban Gaji Kantor', 'EXPENSE', 'DEBIT', FALSE, FALSE, NULL, 3, NOW(6), 'system'),
('6-2200', 'Beban Penyusutan Kantor', 'EXPENSE', 'DEBIT', FALSE, FALSE, NULL, 3, NOW(6), 'system'),
('6-2300', 'Beban Kerugian Piutang', 'EXPENSE', 'DEBIT', FALSE, FALSE, NULL, 3, NOW(6), 'system'),
('6-2400', 'Beban Listrik, Air & Telepon Kantor', 'EXPENSE', 'DEBIT', FALSE, FALSE, NULL, 3, NOW(6), 'system'),
('6-2900', 'Beban Administrasi Lain-lain', 'EXPENSE', 'DEBIT', FALSE, FALSE, NULL, 3, NOW(6), 'system'),
('7-0000', 'PENDAPATAN & BEBAN LAIN-LAIN', 'OTHER_INCOME', 'CREDIT', TRUE, FALSE, NULL, 1, NOW(6), 'system'),
('7-1000', 'Pendapatan Lain-lain', 'OTHER_INCOME', 'CREDIT', FALSE, FALSE, NULL, 2, NOW(6), 'system'),
('7-2000', 'Beban Bunga', 'OTHER_EXPENSE', 'DEBIT', FALSE, FALSE, NULL, 2, NOW(6), 'system'),
('7-3000', 'Laba/Rugi Pelepasan Aset', 'OTHER_INCOME', 'CREDIT', FALSE, FALSE, NULL, 2, NOW(6), 'system'),
('7-4000', 'Selisih Pembulatan', 'OTHER_EXPENSE', 'DEBIT', FALSE, FALSE, NULL, 2, NOW(6), 'system'),
('7-5000', 'Beban Administrasi Bank', 'OTHER_EXPENSE', 'DEBIT', FALSE, FALSE, NULL, 2, NOW(6), 'system'),
('8-0000', 'BEBAN PAJAK PENGHASILAN', 'INCOME_TAX', 'DEBIT', TRUE, FALSE, NULL, 1, NOW(6), 'system'),
('8-1000', 'Beban Pajak Penghasilan', 'INCOME_TAX', 'DEBIT', FALSE, FALSE, NULL, 2, NOW(6), 'system');

-- Relasi induk-anak
UPDATE accounts child JOIN (
    SELECT '1-1000' AS code, '1-0000' AS parent_code
    UNION ALL
    SELECT '1-1100' AS code, '1-1000' AS parent_code
    UNION ALL
    SELECT '1-1200' AS code, '1-1000' AS parent_code
    UNION ALL
    SELECT '1-1300' AS code, '1-1000' AS parent_code
    UNION ALL
    SELECT '1-1310' AS code, '1-1000' AS parent_code
    UNION ALL
    SELECT '1-1400' AS code, '1-1000' AS parent_code
    UNION ALL
    SELECT '1-1410' AS code, '1-1000' AS parent_code
    UNION ALL
    SELECT '1-1420' AS code, '1-1000' AS parent_code
    UNION ALL
    SELECT '1-1430' AS code, '1-1000' AS parent_code
    UNION ALL
    SELECT '1-1440' AS code, '1-1000' AS parent_code
    UNION ALL
    SELECT '1-1500' AS code, '1-1000' AS parent_code
    UNION ALL
    SELECT '1-1510' AS code, '1-1000' AS parent_code
    UNION ALL
    SELECT '1-1600' AS code, '1-1000' AS parent_code
    UNION ALL
    SELECT '1-1700' AS code, '1-1000' AS parent_code
    UNION ALL
    SELECT '1-2000' AS code, '1-0000' AS parent_code
    UNION ALL
    SELECT '1-2100' AS code, '1-2000' AS parent_code
    UNION ALL
    SELECT '1-2200' AS code, '1-2000' AS parent_code
    UNION ALL
    SELECT '1-2210' AS code, '1-2000' AS parent_code
    UNION ALL
    SELECT '1-2300' AS code, '1-2000' AS parent_code
    UNION ALL
    SELECT '1-2310' AS code, '1-2000' AS parent_code
    UNION ALL
    SELECT '1-2400' AS code, '1-2000' AS parent_code
    UNION ALL
    SELECT '1-2410' AS code, '1-2000' AS parent_code
    UNION ALL
    SELECT '1-2500' AS code, '1-2000' AS parent_code
    UNION ALL
    SELECT '1-2510' AS code, '1-2000' AS parent_code
    UNION ALL
    SELECT '2-1000' AS code, '2-0000' AS parent_code
    UNION ALL
    SELECT '2-1100' AS code, '2-1000' AS parent_code
    UNION ALL
    SELECT '2-1200' AS code, '2-1000' AS parent_code
    UNION ALL
    SELECT '2-1300' AS code, '2-1000' AS parent_code
    UNION ALL
    SELECT '2-1400' AS code, '2-1000' AS parent_code
    UNION ALL
    SELECT '2-1500' AS code, '2-1000' AS parent_code
    UNION ALL
    SELECT '2-1600' AS code, '2-1000' AS parent_code
    UNION ALL
    SELECT '2-1700' AS code, '2-1000' AS parent_code
    UNION ALL
    SELECT '2-1800' AS code, '2-1000' AS parent_code
    UNION ALL
    SELECT '2-1900' AS code, '2-1000' AS parent_code
    UNION ALL
    SELECT '2-2000' AS code, '2-0000' AS parent_code
    UNION ALL
    SELECT '2-2100' AS code, '2-2000' AS parent_code
    UNION ALL
    SELECT '3-1000' AS code, '3-0000' AS parent_code
    UNION ALL
    SELECT '3-2000' AS code, '3-0000' AS parent_code
    UNION ALL
    SELECT '3-3000' AS code, '3-0000' AS parent_code
    UNION ALL
    SELECT '3-4000' AS code, '3-0000' AS parent_code
    UNION ALL
    SELECT '3-9000' AS code, '3-0000' AS parent_code
    UNION ALL
    SELECT '4-1000' AS code, '4-0000' AS parent_code
    UNION ALL
    SELECT '4-1100' AS code, '4-0000' AS parent_code
    UNION ALL
    SELECT '4-1200' AS code, '4-0000' AS parent_code
    UNION ALL
    SELECT '5-1000' AS code, '5-0000' AS parent_code
    UNION ALL
    SELECT '5-2000' AS code, '5-0000' AS parent_code
    UNION ALL
    SELECT '5-2100' AS code, '5-2000' AS parent_code
    UNION ALL
    SELECT '5-2200' AS code, '5-2000' AS parent_code
    UNION ALL
    SELECT '5-2300' AS code, '5-2000' AS parent_code
    UNION ALL
    SELECT '5-2400' AS code, '5-2000' AS parent_code
    UNION ALL
    SELECT '5-2500' AS code, '5-2000' AS parent_code
    UNION ALL
    SELECT '5-2600' AS code, '5-2000' AS parent_code
    UNION ALL
    SELECT '5-3000' AS code, '5-0000' AS parent_code
    UNION ALL
    SELECT '5-4000' AS code, '5-0000' AS parent_code
    UNION ALL
    SELECT '5-5000' AS code, '5-0000' AS parent_code
    UNION ALL
    SELECT '5-5100' AS code, '5-0000' AS parent_code
    UNION ALL
    SELECT '5-5200' AS code, '5-0000' AS parent_code
    UNION ALL
    SELECT '5-5300' AS code, '5-0000' AS parent_code
    UNION ALL
    SELECT '5-6000' AS code, '5-0000' AS parent_code
    UNION ALL
    SELECT '5-7000' AS code, '5-0000' AS parent_code
    UNION ALL
    SELECT '5-9000' AS code, '5-0000' AS parent_code
    UNION ALL
    SELECT '6-1000' AS code, '6-0000' AS parent_code
    UNION ALL
    SELECT '6-2000' AS code, '6-0000' AS parent_code
    UNION ALL
    SELECT '6-2100' AS code, '6-2000' AS parent_code
    UNION ALL
    SELECT '6-2200' AS code, '6-2000' AS parent_code
    UNION ALL
    SELECT '6-2300' AS code, '6-2000' AS parent_code
    UNION ALL
    SELECT '6-2400' AS code, '6-2000' AS parent_code
    UNION ALL
    SELECT '6-2900' AS code, '6-2000' AS parent_code
    UNION ALL
    SELECT '7-1000' AS code, '7-0000' AS parent_code
    UNION ALL
    SELECT '7-2000' AS code, '7-0000' AS parent_code
    UNION ALL
    SELECT '7-3000' AS code, '7-0000' AS parent_code
    UNION ALL
    SELECT '7-4000' AS code, '7-0000' AS parent_code
    UNION ALL
    SELECT '7-5000' AS code, '7-0000' AS parent_code
    UNION ALL
    SELECT '8-1000' AS code, '8-0000' AS parent_code
) rel ON rel.code = child.code
JOIN accounts parent ON parent.code = rel.parent_code
SET child.parent_id = parent.id;
