# PROMPT — Aplikasi Web Full-Stack Pembukuan Akuntansi Manufaktur

## 0. Peran & Tujuan

Kamu adalah senior full-stack engineer sekaligus akuntan biaya (cost accountant) yang paham standar akuntansi Indonesia (SAK/PSAK) dan sistem informasi akuntansi manufaktur.

Bangun **aplikasi web full-stack pembukuan akuntansi untuk perusahaan manufaktur yang sangat lengkap**, siap dipakai (production-grade), dengan halaman **Login** dan **Registrasi**, menggunakan:

- **Frontend:** Next.js (App Router) + TypeScript + TailwindCSS + HTML semantik
- **Backend:** Java Spring Boot (REST API)
- **Database:** MySQL 8.4

Kerjakan secara bertahap per fase (lihat bagian 12). Setelah setiap fase selesai, **berhenti**, ringkas apa yang sudah dibuat, dan tunggu konfirmasi sebelum lanjut ke fase berikutnya.

---

## 1. Lingkungan Lokal (sudah terpasang di mesin developer)

- macOS Apple Silicon (arm64)
- Java 21 (Temurin), Maven, Spring Boot CLI
- Node.js LTS + npm
- MySQL 8.4 via Homebrew, berjalan di `localhost:3306`
  - Database: `accounting_db` (utf8mb4 / utf8mb4_unicode_ci)
  - User: `accounting` / password dari environment variable `DB_PASSWORD`
- Root folder project: `~/Documents/FullStack-SpringBoot`
- Port: backend `8080`, frontend `3000`

Jangan hardcode credential. Pakai `.env` / environment variable, dan sediakan `.env.example`.

---

## 2. Struktur Repository (monorepo)

```
FullStack-SpringBoot/
├── backend/                 # Spring Boot (Maven)
├── frontend/                # Next.js
├── docs/                    # ERD, API docs, panduan alur akuntansi
├── docker-compose.yml       # opsional: MySQL + backend + frontend
└── README.md                # cara setup & run lengkap
```

---

## 3. Stack & Library

### Backend
- Java 21, Spring Boot versi stabil terbaru (3.5+ atau 4.x)
- Spring Web, Spring Data JPA (Hibernate), Spring Security, Spring Validation
- JWT (jjwt atau spring-security-oauth2-resource-server), BCrypt
- Flyway untuk migrasi database (**dilarang** `ddl-auto=update` di profil selain dev)
- MapStruct (mapping DTO), Lombok
- springdoc-openapi (Swagger UI di `/swagger-ui.html`)
- Apache POI (export Excel), OpenPDF / JasperReports (export PDF)
- JUnit 5, Mockito, Testcontainers (MySQL) untuk integration test

### Frontend
- Next.js App Router versi stabil terbaru, TypeScript strict mode
- TailwindCSS + shadcn/ui (komponen berbasis Radix)
- TanStack Query (data fetching & cache), TanStack Table (tabel data)
- React Hook Form + Zod (form & validasi)
- Recharts (grafik dashboard)
- lucide-react (ikon), date-fns (tanggal, locale `id`)
- Format angka Rupiah: `Intl.NumberFormat('id-ID', { style: 'currency', currency: 'IDR' })`

---

## 4. Autentikasi & Otorisasi

### Halaman Registrasi (`/register`)
- Field: nama lengkap, email, username, password, konfirmasi password, nama perusahaan (khusus user pertama yang membuat perusahaan)
- Validasi: email unik, username unik, password minimal 8 karakter dengan huruf besar, huruf kecil, angka, dan simbol, serta indikator kekuatan password
- User pertama otomatis menjadi `ADMIN` perusahaan. Registrasi berikutnya berstatus `PENDING` sampai di-approve Admin.

### Halaman Login (`/login`)
- Login dengan email atau username + password
- Checkbox "Ingat saya", link "Lupa password"
- Rate limiting: kunci akun 15 menit setelah 5 kali gagal login
- Tampilkan pesan error yang jelas tanpa membocorkan apakah email terdaftar

### Lupa / Reset Password
- `/forgot-password` → kirim token reset (di dev cukup log ke console / Mailpit)
- `/reset-password?token=...` → token berlaku 30 menit, sekali pakai

### Mekanisme Token
- Access token JWT berumur 15 menit, refresh token berumur 7 hari dengan rotasi dan disimpan di DB (bisa di-revoke)
- Simpan token di **httpOnly secure cookie** (bukan localStorage)
- Next.js `middleware.ts` melindungi semua route di grup `(dashboard)` dan me-redirect ke `/login` bila belum login
- Logout me-revoke refresh token

### Role & Permission (RBAC)
| Role | Akses |
|---|---|
| ADMIN | Semua modul, manajemen user, setting perusahaan, tutup periode |
| AKUNTAN | GL, jurnal, AP/AR, kas-bank, aset tetap, pajak, laporan |
| PRODUKSI | BOM, work order, pemakaian bahan, tenaga kerja, laporan produksi |
| GUDANG | Item, penerimaan barang, transfer, stock opname |
| PEMBELIAN / PENJUALAN | PO, SO, faktur masing-masing |
| VIEWER | Read-only dashboard & laporan |

Permission granular per aksi (`VIEW`, `CREATE`, `EDIT`, `DELETE`, `APPROVE`, `POST`). Terapkan di backend (`@PreAuthorize`) **dan** sembunyikan tombol/menu di frontend sesuai permission.

---

## 5. Modul Fungsional

### 5.1 Pengaturan & Master Data
- **Profil perusahaan:** nama, NPWP, alamat, logo, mata uang dasar IDR, awal tahun fiskal
- **Periode akuntansi:** tahun fiskal & 12 periode bulanan dengan status `OPEN` / `CLOSED` / `LOCKED`
- **Bagan Akun (Chart of Accounts):** hierarki induk–anak (header vs detail), tipe akun (Aset, Liabilitas, Ekuitas, Pendapatan, HPP, Beban, Pendapatan/Beban Lain), saldo normal (D/K), dan flag akun kas/bank. Seed default COA manufaktur Indonesia (lihat bagian 7).
- **Account Mapping:** tabel konfigurasi akun default untuk auto-journal (persediaan BB, BDP, BOP dibebankan, HPP, PPN masukan/keluaran, dst.) sehingga tidak ada akun yang di-hardcode
- **Pajak:** tarif PPN (configurable, default 11%), PPh 23, PPh 22
- **Satuan (UoM)** beserta konversi satuan
- **Gudang** (multi-gudang)
- **Departemen / Cost Center:** produksi vs non-produksi, departemen pendukung (service department)
- **Pelanggan & Pemasok:** NPWP, termin pembayaran, limit kredit
- **Item** dengan kategori `BAHAN_BAKU`, `BAHAN_PENOLONG`, `BARANG_DALAM_PROSES`, `BARANG_JADI`, `SUPPLIES`, `JASA`, serta biaya standar per item
- **Bill of Materials (BOM):** multi-level, versioning, persentase scrap, kuantitas per output
- **Work Center & Routing:** urutan operasi, jam mesin/jam tenaga kerja standar, tarif per jam
- **Tarif BOP (overhead rate):** predetermined rate per departemen dengan dasar pembebanan pilihan (jam mesin, jam TK langsung, biaya TK langsung, unit)

### 5.2 Buku Besar (General Ledger)
- Jurnal umum manual multi-baris dengan validasi **total debit = total kredit** (backend wajib menolak bila tidak balance)
- Status jurnal: `DRAFT` → `POSTED`. Jurnal yang sudah posted tidak bisa diedit atau dihapus; koreksi dilakukan lewat **jurnal pembalik (reversal)**.
- Jurnal otomatis (auto-posting) dari setiap transaksi modul lain, dengan referensi ke dokumen sumber (`source_type`, `source_id`)
- Jurnal berulang (recurring), jurnal penyesuaian (adjusting)
- Penomoran dokumen otomatis per tipe & periode, misal `JU/2026/10/0001`
- Lampiran dokumen (upload bukti)
- **Tutup buku bulanan:** cek BOP over/under-applied, alokasi selisih, lalu kunci periode
- **Tutup buku tahunan:** jurnal penutup ke Laba Ditahan, saldo awal tahun berikutnya otomatis

### 5.3 Pembelian & Utang Usaha (AP)
- Purchase Request → Purchase Order (approval) → Penerimaan Barang (GRN) → Faktur Pembelian → Pembayaran
- Three-way matching (PO vs GRN vs Faktur) dengan toleransi selisih
- Uang muka pembelian, retur pembelian (nota debit), diskon, biaya angkut (dikapitalisasi ke harga perolehan persediaan)
- Laporan umur utang (AP Aging): 0–30, 31–60, 61–90, >90 hari

### 5.4 Persediaan (Inventory)
- Sistem perpetual
- Metode penilaian: **Rata-rata Tertimbang (Moving Average)** sebagai default, **FIFO** sebagai opsi (setting per perusahaan)
- Kartu stok per item per gudang (qty & nilai)
- Transfer antar gudang, penyesuaian stok, stock opname dengan jurnal selisih otomatis
- Reorder point & notifikasi stok minimum
- Mutasi stok tidak boleh menyebabkan stok negatif (configurable)

### 5.5 Produksi & Akuntansi Biaya (inti aplikasi)
- **Work Order / Perintah Produksi:** produk, qty rencana, BOM version, tanggal, departemen; status `PLANNED` → `RELEASED` → `IN_PROGRESS` → `COMPLETED` → `CLOSED`
- **Pemakaian Bahan Baku (BBB):** material issue berdasarkan BOM, termasuk tambahan pemakaian & retur bahan ke gudang
- **Biaya Tenaga Kerja Langsung (BTKL):** input timesheet per work order (jam × tarif), pembedaan BTKL vs BTKTL
- **Biaya Overhead Pabrik (BOP):**
  - BOP aktual dicatat ke akun kontrol BOP aktual (bahan penolong, TK tidak langsung, listrik pabrik, penyusutan mesin, dll.)
  - BOP dibebankan ke BDP memakai tarif ditentukan di muka × dasar pembebanan aktual
  - Alokasi biaya departemen pendukung ke departemen produksi (metode langsung & bertahap/step-down)
- **Penyelesaian produksi:** transfer BDP → Persediaan Barang Jadi, dengan dukungan penyelesaian parsial
- **Produk rusak, sisa bahan (scrap) & produk sampingan:** normal (dibebankan ke produk baik) vs abnormal (dibebankan ke rugi periode)
- **Dua metode kalkulasi biaya** (pilih per produk / work order):
  1. **Job Order Costing** (harga pokok pesanan): kartu biaya pesanan (job cost sheet)
  2. **Process Costing** (harga pokok proses): Unit Ekuivalen (EUP) metode **Rata-rata Tertimbang** dan **FIFO**, multi-departemen, termasuk Laporan Biaya Produksi per departemen
- **Standard Costing & Analisis Selisih:**
  - Selisih harga & kuantitas bahan
  - Selisih tarif & efisiensi tenaga kerja
  - Selisih BOP: dua selisih dan tiga selisih (pengeluaran, kapasitas, efisiensi)
  - Status favorable / unfavorable untuk setiap selisih
- Biaya per unit aktual vs standar per work order

### 5.6 Penjualan & Piutang (AR)
- Quotation → Sales Order → Surat Jalan (Delivery Order) → Faktur Penjualan → Penerimaan Pembayaran
- Saat pengiriman, jurnal HPP Penjualan dibuat otomatis berdasarkan nilai persediaan barang jadi
- Retur penjualan (nota kredit), uang muka penjualan, diskon
- Cek limit kredit pelanggan
- Laporan umur piutang (AR Aging) & cadangan kerugian piutang

### 5.7 Kas & Bank
- Kas masuk/keluar, transfer antar rekening, kas kecil (petty cash)
- Rekonsiliasi bank: import mutasi CSV, matching otomatis + manual
- Giro/cek mundur

### 5.8 Aset Tetap
- Register aset: kategori, harga perolehan, tanggal, umur ekonomis, nilai residu, departemen (pabrik vs kantor)
- Metode penyusutan: garis lurus, saldo menurun ganda, unit produksi
- Jurnal penyusutan bulanan otomatis. Penyusutan aset pabrik masuk ke BOP aktual, sedangkan aset kantor masuk ke beban operasional.
- Pelepasan / penjualan aset dengan laba/rugi pelepasan

### 5.9 Pajak
- Rekap PPN Masukan vs PPN Keluaran per masa pajak dan jurnal kompensasi
- Rekap PPh 23 dipotong / dipungut
- Export data faktur (CSV) sebagai persiapan e-Faktur / Coretax

### 5.10 Anggaran (Budgeting)
- Anggaran per akun per periode per departemen
- Laporan realisasi vs anggaran beserta persentase selisihnya

### 5.11 Laporan (semua bisa filter periode/departemen, komparatif, export PDF & Excel, dan print-friendly)
1. Jurnal Umum
2. Buku Besar (per akun, dengan saldo berjalan)
3. Neraca Saldo (sebelum & setelah penyesuaian)
4. **Laporan Harga Pokok Produksi** (Cost of Goods Manufactured): BBB + BTKL + BOP dibebankan + BDP awal − BDP akhir
5. **Laporan Harga Pokok Penjualan:** HPP + Persediaan Barang Jadi awal − Persediaan Barang Jadi akhir ± selisih BOP
6. Laporan Laba Rugi (format manufaktur, multi-step)
7. Laporan Posisi Keuangan / Neraca
8. Laporan Perubahan Ekuitas
9. Laporan Arus Kas (metode tidak langsung)
10. Laporan Biaya Produksi per departemen (process costing)
11. Kartu Biaya Pesanan (job order)
12. Laporan Analisis Selisih Biaya Standar
13. Kartu Stok & Laporan Nilai Persediaan
14. AP Aging & AR Aging
15. Laporan Penyusutan Aset Tetap
16. Rasio keuangan: likuiditas, profitabilitas, perputaran persediaan

Drill-down: klik angka di laporan → buku besar → jurnal → dokumen sumber.

### 5.12 Dashboard
- KPI card: pendapatan, laba kotor, laba bersih, kas & bank, piutang, utang, nilai persediaan (BB, BDP, BJ)
- Grafik: tren pendapatan vs HPP 12 bulan, komposisi biaya produksi (BBB/BTKL/BOP), top 5 produk, work order aktif, selisih BOP
- Notifikasi: stok minimum, faktur jatuh tempo, approval menunggu

### 5.13 Administrasi
- Manajemen user (approve, nonaktifkan, reset password, atur role)
- **Audit trail:** siapa, kapan, aksi, data sebelum/sesudah (JSON) untuk semua entitas penting
- Setting penomoran dokumen
- Backup / export data

---

## 6. Aturan Akuntansi Wajib (business rules)

1. Semua nilai uang memakai `BigDecimal` (Java) dan `DECIMAL(19,2)` (MySQL); harga/biaya per unit memakai `DECIMAL(19,4)`; kuantitas memakai `DECIMAL(19,4)`. **Dilarang memakai float/double.** Di frontend, jangan menghitung uang dengan floating point untuk data yang disimpan; kalkulasi final selalu di backend.
2. Setiap transaksi yang berdampak keuangan **wajib** menghasilkan jurnal double-entry yang balance, dalam satu `@Transactional` bersama dokumen sumbernya (all-or-nothing).
3. Tidak boleh posting ke periode `CLOSED` / `LOCKED`.
4. Dokumen yang sudah posted tidak bisa dihapus; pembatalan = jurnal pembalik + status `VOID`.
5. Hanya akun detail (bukan header) yang boleh dipakai di jurnal.
6. Pembulatan memakai `RoundingMode.HALF_UP`. Selisih pembulatan dibukukan ke akun selisih pembulatan.
7. Neraca harus selalu balance (Aset = Liabilitas + Ekuitas). Buat endpoint health-check akuntansi yang memverifikasi hal ini serta neraca saldo.
8. Soft delete untuk master data yang sudah dipakai transaksi (nonaktif, bukan hapus).
9. Optimistic locking (`@Version`) pada dokumen transaksi.
10. Semua tanggal transaksi memakai timezone `Asia/Jakarta`.

---

## 7. Seed Bagan Akun Default (Manufaktur)

Buat via Flyway seed, struktur kode `X-XXXX`:

```
1-0000 ASET
  1-1000 Aset Lancar
    1-1100 Kas
    1-1200 Bank
    1-1300 Piutang Usaha
    1-1310 Cadangan Kerugian Piutang
    1-1400 Persediaan Bahan Baku
    1-1410 Persediaan Bahan Penolong
    1-1420 Persediaan Barang Dalam Proses (BDP)
    1-1430 Persediaan Barang Jadi
    1-1440 Persediaan Supplies Pabrik
    1-1500 PPN Masukan
    1-1510 PPh Dibayar di Muka
    1-1600 Uang Muka Pembelian
    1-1700 Biaya Dibayar di Muka
  1-2000 Aset Tetap
    1-2100 Tanah
    1-2200 Bangunan Pabrik
    1-2210 Akumulasi Penyusutan Bangunan Pabrik
    1-2300 Mesin & Peralatan Pabrik
    1-2310 Akumulasi Penyusutan Mesin
    1-2400 Kendaraan
    1-2410 Akumulasi Penyusutan Kendaraan
    1-2500 Peralatan Kantor
    1-2510 Akumulasi Penyusutan Peralatan Kantor
2-0000 LIABILITAS
  2-1000 Liabilitas Jangka Pendek
    2-1100 Utang Usaha
    2-1200 Utang Gaji & Upah
    2-1300 PPN Keluaran
    2-1400 Utang PPh 21 / 23
    2-1500 Uang Muka Penjualan
    2-1600 Biaya Masih Harus Dibayar
    2-1700 Barang Diterima Belum Ditagih (GRNI)
  2-2000 Liabilitas Jangka Panjang
    2-2100 Utang Bank
3-0000 EKUITAS
    3-1000 Modal Disetor
    3-2000 Laba Ditahan
    3-3000 Laba Tahun Berjalan
    3-4000 Prive / Dividen
4-0000 PENDAPATAN
    4-1000 Penjualan
    4-1100 Retur Penjualan
    4-1200 Potongan Penjualan
5-0000 HARGA POKOK
    5-1000 Harga Pokok Penjualan
    5-2000 Biaya Overhead Pabrik Aktual (Kontrol)
      5-2100 Bahan Penolong
      5-2200 Tenaga Kerja Tidak Langsung
      5-2300 Listrik & Air Pabrik
      5-2400 Penyusutan Mesin & Bangunan Pabrik
      5-2500 Pemeliharaan Mesin
      5-2600 BOP Lain-lain
    5-3000 BOP Dibebankan
    5-4000 Selisih BOP
    5-5000 Selisih Harga Bahan
    5-5100 Selisih Kuantitas Bahan
    5-5200 Selisih Tarif Tenaga Kerja
    5-5300 Selisih Efisiensi Tenaga Kerja
    5-6000 Rugi Produk Rusak Abnormal
    5-9000 Gaji & Upah (Clearing)
6-0000 BEBAN OPERASIONAL
    6-1000 Beban Penjualan
    6-2000 Beban Administrasi & Umum
    6-2100 Beban Gaji Kantor
    6-2200 Beban Penyusutan Kantor
    6-2300 Beban Kerugian Piutang
7-0000 PENDAPATAN & BEBAN LAIN-LAIN
    7-1000 Pendapatan Lain-lain
    7-2000 Beban Bunga
    7-3000 Laba/Rugi Pelepasan Aset
    7-4000 Selisih Pembulatan
8-0000 BEBAN PAJAK PENGHASILAN
```

---

## 8. Alur Jurnal Otomatis (wajib diimplementasikan & diuji)

| # | Transaksi | Debit | Kredit |
|---|---|---|---|
| 1 | Penerimaan bahan baku (GRN) | Persediaan Bahan Baku | Barang Diterima Belum Ditagih |
| 2 | Faktur pembelian | Barang Diterima Belum Ditagih, PPN Masukan | Utang Usaha |
| 3 | Pembayaran utang | Utang Usaha | Kas/Bank |
| 4 | Pemakaian bahan baku ke WO | BDP | Persediaan Bahan Baku |
| 5 | Pemakaian bahan penolong | BOP Aktual – Bahan Penolong | Persediaan Bahan Penolong |
| 6 | Pencatatan payroll | Gaji & Upah (Clearing) | Utang Gaji & Upah, Utang PPh 21 |
| 7 | Distribusi payroll | BDP (BTKL), BOP Aktual – TKTL, Beban Gaji Kantor | Gaji & Upah (Clearing) |
| 8 | BOP aktual lain (listrik, pemeliharaan) | BOP Aktual – sub akun | Kas/Bank/Utang |
| 9 | Penyusutan aset pabrik | BOP Aktual – Penyusutan | Akumulasi Penyusutan |
| 10 | BOP dibebankan ke WO | BDP | BOP Dibebankan |
| 11 | Penyelesaian produksi | Persediaan Barang Jadi | BDP |
| 12 | Produk rusak abnormal | Rugi Produk Rusak Abnormal | BDP |
| 13 | Faktur penjualan | Piutang Usaha | Penjualan, PPN Keluaran |
| 14 | Pengiriman barang (HPP) | Harga Pokok Penjualan | Persediaan Barang Jadi |
| 15 | Penerimaan piutang | Kas/Bank | Piutang Usaha |
| 16 | Tutup BOP akhir periode | BOP Dibebankan | BOP Aktual (Kontrol); selisih ke Selisih BOP |
| 17 | Tutup selisih BOP (tidak material) | HPP / Selisih BOP | Selisih BOP / HPP |
| 18 | Standard costing: selisih bahan/TK | Akun selisih (U) atau BDP | BDP atau akun selisih (F) |

Semua akun diambil dari tabel **Account Mapping**, bukan di-hardcode.

---

## 9. Desain Database (MySQL)

- Semua tabel menggunakan `id BIGINT AUTO_INCREMENT`, `created_at`, `created_by`, `updated_at`, `updated_by`, `version`, dan `deleted_at` (bila perlu)
- Nama tabel & kolom `snake_case`, foreign key + index yang tepat, unique constraint untuk nomor dokumen
- Tabel inti minimal:
  `users, roles, permissions, user_roles, role_permissions, refresh_tokens, password_reset_tokens, companies, fiscal_years, periods, accounts, account_mappings, tax_rates, uoms, uom_conversions, warehouses, departments, customers, suppliers, items, item_categories, boms, bom_lines, work_centers, routings, routing_steps, overhead_rates, journal_entries, journal_lines, document_sequences, purchase_orders, purchase_order_lines, goods_receipts, goods_receipt_lines, purchase_invoices, purchase_invoice_lines, ap_payments, sales_orders, sales_order_lines, delivery_orders, delivery_order_lines, sales_invoices, sales_invoice_lines, ar_receipts, stock_movements, stock_balances, stock_cost_layers (FIFO), stock_opnames, work_orders, work_order_materials, work_order_labors, work_order_overheads, work_order_completions, process_cost_reports, cost_variances, bank_accounts, cash_transactions, bank_reconciliations, fixed_assets, depreciation_schedules, budgets, attachments, audit_logs, notifications`
- Buat dokumen ERD (Mermaid) di `docs/ERD.md`
- Flyway: `V1__init_schema.sql`, `V2__seed_roles_permissions.sql`, `V3__seed_coa_manufaktur.sql`, `V4__seed_account_mappings.sql`, `V5__seed_demo_data.sql` (profil `dev` saja: data demo 1 perusahaan, 3 produk, BOM, 2 WO, transaksi 3 bulan)

---

## 10. Arsitektur Backend

```
backend/src/main/java/com/aji/accounting/
├── config/          # Security, CORS, OpenAPI, Jackson, Auditing
├── common/          # BaseEntity, exception, response wrapper, util Money, pagination
├── auth/            # register, login, refresh, logout, reset password
├── user/
├── company/
├── masterdata/      # uom, warehouse, department, customer, supplier, item
├── coa/             # account, account mapping
├── ledger/          # journal, posting engine, period closing
├── purchasing/
├── sales/
├── inventory/       # stock movement, costing (avg/FIFO)
├── manufacturing/   # bom, routing, work order, costing, variance
├── cashbank/
├── fixedasset/
├── tax/
├── budget/
├── report/          # query laporan & export
├── dashboard/
└── audit/
```

- Pola: Controller → Service (interface + impl) → Repository; DTO request/response terpisah dari entity
- **Posting Engine** terpusat (`JournalPostingService`) yang dipanggil semua modul; tidak ada modul yang menulis langsung ke `journal_lines`
- Global exception handler dengan format **RFC 7807 Problem Details** dan pesan error berbahasa Indonesia
- Response list memakai pagination (`page`, `size`, `sort`) + filter
- REST naming: `/api/v1/{resource}`, aksi khusus `POST /api/v1/journals/{id}/post`, `/reverse`, `/approve`
- Query laporan berat memakai native query/projection, bukan memuat entity satu per satu (hindari N+1)
- Profil `dev`, `test`, `prod` di `application.yml`
- CORS hanya mengizinkan `http://localhost:3000` di dev

---

## 11. Arsitektur Frontend

```
frontend/src/
├── app/
│   ├── (auth)/login/page.tsx
│   ├── (auth)/register/page.tsx
│   ├── (auth)/forgot-password/page.tsx
│   ├── (auth)/reset-password/page.tsx
│   ├── (dashboard)/layout.tsx          # sidebar + topbar + breadcrumb
│   ├── (dashboard)/dashboard/page.tsx
│   ├── (dashboard)/master/...
│   ├── (dashboard)/coa/...
│   ├── (dashboard)/jurnal/...
│   ├── (dashboard)/pembelian/...
│   ├── (dashboard)/penjualan/...
│   ├── (dashboard)/persediaan/...
│   ├── (dashboard)/produksi/...
│   ├── (dashboard)/kas-bank/...
│   ├── (dashboard)/aset-tetap/...
│   ├── (dashboard)/pajak/...
│   ├── (dashboard)/anggaran/...
│   ├── (dashboard)/laporan/...
│   └── (dashboard)/pengaturan/...
├── components/ui/          # shadcn
├── components/shared/      # DataTable, MoneyInput, AccountPicker, PeriodPicker, StatusBadge
├── features/<modul>/       # api hooks, schema zod, komponen per modul
├── lib/                    # api client (fetch wrapper + refresh token), format, auth
├── types/
└── middleware.ts
```

- **UI/UX:**
  - Bahasa Indonesia
  - Desain bersih & profesional untuk aplikasi keuangan, responsif (desktop-first tapi tetap usable di tablet), dengan dark mode
  - Halaman Login & Registrasi: layout split-screen (ilustrasi/branding di kiri, form di kanan), loading state, dan pesan error inline
  - Sidebar collapsible dengan grup menu per modul, menu disaring sesuai permission
  - Tabel: search, filter, sort, pagination server-side, export
  - Form jurnal: baris dinamis, total debit/kredit live, indikator "Balance / Tidak balance", AccountPicker dengan pencarian kode/nama
  - Input uang memakai format ribuan Indonesia (`1.250.000,00`)
  - Konfirmasi sebelum aksi destruktif (posting, void, tutup periode)
  - Skeleton loading, empty state, error boundary, toast notifikasi
- **Aksesibilitas:** HTML semantik, label pada setiap input, navigasi keyboard, kontras cukup
- API client menangani 401 → refresh token otomatis → retry; bila gagal, redirect ke login
- Proxy API via `rewrites` di `next.config.ts` (`/api/:path*` → `http://localhost:8080/api/:path*`)

---

## 12. Tahapan Pengerjaan (berhenti & laporkan di akhir tiap fase)

1. **Fase 1 – Fondasi:** struktur monorepo, setup backend + frontend, koneksi MySQL, Flyway, BaseEntity, exception handler, OpenAPI, layout dashboard
2. **Fase 2 – Auth:** registrasi, login, refresh, logout, lupa/reset password, RBAC, manajemen user, middleware frontend, halaman Login & Registrasi lengkap
3. **Fase 3 – Master Data & COA:** perusahaan, periode, COA + seed, account mapping, pajak, UoM, gudang, departemen, pelanggan, pemasok, item
4. **Fase 4 – Buku Besar:** jurnal manual, posting engine, reversal, buku besar, neraca saldo
5. **Fase 5 – Persediaan, Pembelian & AP**
6. **Fase 6 – Produksi & Akuntansi Biaya:** BOM, routing, tarif BOP, work order, BBB, BTKL, BOP, penyelesaian, job order & process costing, standard costing & variance
7. **Fase 7 – Penjualan & AR**
8. **Fase 8 – Kas & Bank, Aset Tetap, Pajak, Anggaran**
9. **Fase 9 – Laporan Keuangan & Laporan Biaya + export PDF/Excel + Dashboard**
10. **Fase 10 – Tutup buku, audit trail, hardening keamanan, test, dokumentasi, docker-compose**

---

## 13. Testing & Kriteria Selesai

- Unit test untuk: posting engine (balance check), kalkulasi moving average & FIFO, EUP rata-rata & FIFO, alokasi BOP, analisis selisih, penyusutan
- Integration test (Testcontainers MySQL) untuk satu **skenario end-to-end satu siklus produksi**: beli bahan → issue ke WO → catat upah → BOP dibebankan → selesai produksi → jual → terima pembayaran → tutup periode. Hasil yang diharapkan:
  - Neraca saldo balance
  - Neraca balance
  - Nilai di Laporan HPP, Laba Rugi & Neraca konsisten satu sama lain
- Minimal 1 contoh soal process costing dengan angka yang diverifikasi manual di `docs/contoh-perhitungan.md`
- Frontend: lint & type-check tanpa error (`npm run lint`, `tsc --noEmit`)
- Backend: `./mvnw verify` hijau

---

## 14. Dokumentasi yang Harus Dihasilkan

- `README.md`: prasyarat, setup `.env`, cara run backend & frontend, akun demo (`admin@demo.local`), dan troubleshooting
- `docs/ERD.md` (Mermaid)
- `docs/alur-akuntansi.md`: penjelasan setiap jurnal otomatis
- `docs/contoh-perhitungan.md`
- Swagger UI aktif di dev

---

## 15. Aturan Kerja untuk AI

- Tulis kode lengkap & bisa dijalankan. **Jangan** pakai placeholder seperti `// TODO implement` atau `...` untuk logic inti.
- Nama class/variabel/kode dalam bahasa Inggris; label UI, pesan error, dan dokumentasi dalam bahasa Indonesia
- Bila ada keputusan desain yang ambigu, pilih default yang paling umum di praktik akuntansi Indonesia, catat asumsinya di `docs/asumsi.md`, lalu lanjutkan
- Sebelum membuat file baru, cek apakah sudah ada agar tidak terjadi duplikasi
- Di akhir tiap fase, berikan: daftar file yang dibuat/diubah, cara menguji fase tersebut, dan perintah untuk menjalankan
