# Akuntansi Manufaktur

Aplikasi web full-stack pembukuan akuntansi untuk perusahaan manufaktur. Mencakup buku besar, pembelian/AP, penjualan/AR, persediaan, produksi & akuntansi biaya (job order, process costing, standard costing), kas & bank, aset tetap, pajak, anggaran, dan laporan keuangan.

| Lapisan | Teknologi |
|---|---|
| Frontend | Next.js 16 (App Router), TypeScript, TailwindCSS 4, TanStack Query/Table, React Hook Form + Zod, Recharts |
| Backend | Java 21, Spring Boot 3.5, Spring Security (JWT di cookie httpOnly), Spring Data JPA, Flyway |
| Database | MySQL 8.4 |

Spesifikasi lengkap ada di [`PROMPT-Akuntansi-Manufaktur.md`](PROMPT-Akuntansi-Manufaktur.md).

## Struktur

```
backend/    Spring Boot (Maven)
frontend/   Next.js
docs/       ERD, alur akuntansi, contoh perhitungan, asumsi
```

## Prasyarat

- Java 21, Maven 3.9+
- Node.js 20+ (LTS)
- MySQL 8.4 berjalan di `localhost:3306`

Buat database & user (sekali saja):

```sql
CREATE DATABASE accounting_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE USER 'accounting'@'localhost' IDENTIFIED BY 'GantiPasswordIni!';
GRANT ALL PRIVILEGES ON accounting_db.* TO 'accounting'@'localhost';
FLUSH PRIVILEGES;
```

## Menjalankan

### Backend (port 8080)

```bash
cd backend
cp .env.example .env          # isi DB_PASSWORD dan JWT_SECRET
set -a && source .env && set +a
mvn spring-boot:run
```

- Flyway otomatis membuat tabel saat start.
- Profil default `dev` juga memuat data demo (`db/dev`).
- Swagger UI: http://localhost:8080/swagger-ui.html
- Health check: http://localhost:8080/api/v1/health

Opsional, buat Maven wrapper sekali: `mvn -N wrapper:wrapper`, setelah itu bisa pakai `./mvnw`.

### Frontend (port 3000)

```bash
cd frontend
cp .env.example .env.local
npm install
npm run dev
```

Buka http://localhost:3000. Semua request `/api/*` diteruskan ke backend lewat `rewrites` di `next.config.ts`.

### Test

```bash
cd backend && mvn verify            # integration test butuh Docker (Testcontainers); otomatis di-skip bila Docker tidak ada
cd frontend && npm run lint && npx tsc --noEmit
```

## Progres Pengerjaan

| Fase | Cakupan | Status |
|---|---|---|
| 1 | Fondasi monorepo, konfigurasi, layout | ✅ |
| 2 | Autentikasi & RBAC | ✅ |
| 3 | Master data & COA | ⏳ |
| 4 | Buku besar | ⏳ |
| 5 | Persediaan, pembelian & AP | ⏳ |
| 6 | Produksi & akuntansi biaya | ⏳ |
| 7 | Penjualan & AR | ⏳ |
| 8 | Kas & bank, aset tetap, pajak, anggaran | ⏳ |
| 9 | Laporan & dashboard | ⏳ |
| 10 | Tutup buku, audit, hardening | ⏳ |

## Autentikasi

- User **pertama** yang mendaftar otomatis menjadi **Administrator** dan membuat data perusahaan.
- Registrasi berikutnya berstatus *menunggu persetujuan*; Admin menyetujui di **Pengaturan → Pengguna** sambil memilih role.
- Token JWT (15 menit) + refresh token rotasi (7 hari bila "Ingat saya") disimpan di cookie httpOnly `SameSite=Strict`.
- Akun terkunci 15 menit setelah 5 kali gagal login.
- Lupa password: link reset dicetak di log backend (`[RESET PASSWORD] ...`) selama integrasi email belum dipasang.

Role bawaan: `ADMIN`, `AKUNTAN`, `PRODUKSI`, `GUDANG`, `PEMBELIAN`, `PENJUALAN`, `VIEWER`. Permission berformat `MODUL:AKSI` (contoh `LEDGER:POST`).

## Troubleshooting

- `Can't connect to local MySQL server through socket` → MySQL belum jalan: `brew services start mysql@8.4`.
- `Access denied for user 'accounting'` → cek `DB_PASSWORD` di `backend/.env`.
- Frontend menampilkan "Backend tidak terhubung" → pastikan backend jalan di port 8080.
