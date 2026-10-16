-- =====================================================================
-- V3: Seed permission (modul x aksi) dan role standar beserta hak aksesnya
-- =====================================================================

INSERT INTO permissions (code, module, action, description)
SELECT CONCAT(m.module, ':', a.action), m.module, a.action, CONCAT(a.label, ' ', m.label)
FROM (
    SELECT 'DASHBOARD' AS module, 'dashboard' AS label UNION ALL
    SELECT 'MASTER', 'master data' UNION ALL
    SELECT 'COA', 'bagan akun' UNION ALL
    SELECT 'LEDGER', 'jurnal & buku besar' UNION ALL
    SELECT 'PERIOD', 'periode akuntansi' UNION ALL
    SELECT 'PURCHASE', 'pembelian & utang' UNION ALL
    SELECT 'INVENTORY', 'persediaan' UNION ALL
    SELECT 'PRODUCTION', 'produksi & biaya' UNION ALL
    SELECT 'SALES', 'penjualan & piutang' UNION ALL
    SELECT 'CASHBANK', 'kas & bank' UNION ALL
    SELECT 'ASSET', 'aset tetap' UNION ALL
    SELECT 'TAX', 'pajak' UNION ALL
    SELECT 'BUDGET', 'anggaran' UNION ALL
    SELECT 'REPORT', 'laporan' UNION ALL
    SELECT 'SETTINGS', 'pengaturan' UNION ALL
    SELECT 'USER', 'pengguna' UNION ALL
    SELECT 'AUDIT', 'audit trail'
) m
CROSS JOIN (
    SELECT 'VIEW' AS action, 'Melihat' AS label UNION ALL
    SELECT 'CREATE', 'Membuat' UNION ALL
    SELECT 'EDIT', 'Mengubah' UNION ALL
    SELECT 'DELETE', 'Menghapus' UNION ALL
    SELECT 'APPROVE', 'Menyetujui' UNION ALL
    SELECT 'POST', 'Memposting'
) a;

INSERT INTO roles (code, name, description, system_role, created_at, created_by) VALUES
('ADMIN', 'Administrator', 'Akses penuh ke semua modul, manajemen user, dan tutup periode', TRUE, NOW(6), 'system'),
('AKUNTAN', 'Akuntan', 'GL, jurnal, AP/AR, kas-bank, aset tetap, pajak, anggaran, laporan', TRUE, NOW(6), 'system'),
('PRODUKSI', 'Staf Produksi', 'BOM, work order, pemakaian bahan, tenaga kerja, laporan produksi', TRUE, NOW(6), 'system'),
('GUDANG', 'Staf Gudang', 'Item, penerimaan barang, transfer, stock opname', TRUE, NOW(6), 'system'),
('PEMBELIAN', 'Staf Pembelian', 'Purchase request, purchase order, faktur pembelian', TRUE, NOW(6), 'system'),
('PENJUALAN', 'Staf Penjualan', 'Quotation, sales order, faktur penjualan', TRUE, NOW(6), 'system'),
('VIEWER', 'Viewer', 'Hanya melihat dashboard dan laporan', TRUE, NOW(6), 'system');

-- ADMIN: semua permission
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r CROSS JOIN permissions p WHERE r.code = 'ADMIN';

-- AKUNTAN
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r CROSS JOIN permissions p
WHERE r.code = 'AKUNTAN'
  AND (p.module IN ('MASTER', 'COA', 'LEDGER', 'PURCHASE', 'SALES', 'CASHBANK', 'ASSET', 'TAX', 'BUDGET')
       OR (p.module IN ('DASHBOARD', 'REPORT', 'INVENTORY', 'PRODUCTION', 'SETTINGS', 'AUDIT') AND p.action = 'VIEW')
       OR (p.module = 'REPORT' AND p.action = 'CREATE')
       OR (p.module = 'PERIOD' AND p.action IN ('VIEW', 'EDIT', 'POST')));

-- PRODUKSI
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r CROSS JOIN permissions p
WHERE r.code = 'PRODUKSI'
  AND (p.module = 'PRODUCTION'
       OR (p.module = 'INVENTORY' AND p.action IN ('VIEW', 'CREATE'))
       OR (p.module IN ('MASTER', 'REPORT', 'DASHBOARD') AND p.action = 'VIEW'));

-- GUDANG
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r CROSS JOIN permissions p
WHERE r.code = 'GUDANG'
  AND ((p.module = 'INVENTORY' AND p.action <> 'DELETE')
       OR (p.module = 'MASTER' AND p.action IN ('VIEW', 'CREATE', 'EDIT'))
       OR (p.module IN ('PURCHASE', 'SALES', 'DASHBOARD') AND p.action = 'VIEW')
       OR (p.module = 'PURCHASE' AND p.action = 'CREATE')
       OR (p.module = 'SALES' AND p.action = 'CREATE'));

-- PEMBELIAN
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r CROSS JOIN permissions p
WHERE r.code = 'PEMBELIAN'
  AND ((p.module = 'PURCHASE' AND p.action IN ('VIEW', 'CREATE', 'EDIT', 'APPROVE'))
       OR (p.module = 'MASTER' AND p.action IN ('VIEW', 'CREATE', 'EDIT'))
       OR (p.module IN ('INVENTORY', 'DASHBOARD') AND p.action = 'VIEW'));

-- PENJUALAN
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r CROSS JOIN permissions p
WHERE r.code = 'PENJUALAN'
  AND ((p.module = 'SALES' AND p.action IN ('VIEW', 'CREATE', 'EDIT', 'APPROVE'))
       OR (p.module = 'MASTER' AND p.action IN ('VIEW', 'CREATE', 'EDIT'))
       OR (p.module IN ('INVENTORY', 'DASHBOARD') AND p.action = 'VIEW'));

-- VIEWER
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r CROSS JOIN permissions p
WHERE r.code = 'VIEWER' AND p.module IN ('DASHBOARD', 'REPORT') AND p.action = 'VIEW';
