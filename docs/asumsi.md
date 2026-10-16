# Asumsi Desain

Keputusan yang diambil ketika spesifikasi tidak menyebutkan secara eksplisit.

| # | Asumsi | Alasan |
|---|---|---|
| 1 | Satu perusahaan per instalasi (single-tenant). | Sesuai kebutuhan awal; tabel `companies` sudah ada sehingga multi-tenant bisa ditambahkan nanti. |
| 2 | Spring Boot 3.5.x + Java 21. | Versi LTS stabil dengan ekosistem library (springdoc, jjwt) yang matang. |
| 3 | Next.js 16 (App Router) dengan `proxy.ts` (pengganti `middleware.ts`). | Versi terbaru saat proyek dibuat. |
| 4 | Komponen UI dibuat sendiri di `components/ui` (gaya shadcn) tanpa CLI shadcn. | Mengurangi dependensi eksternal; tampilan tetap konsisten. |
| 5 | BigDecimal dikirim ke frontend sebagai string. | Mencegah hilangnya presisi angka di JavaScript. |
| 6 | Metode penilaian persediaan default: Moving Average; FIFO opsional per perusahaan. | Paling umum di manufaktur Indonesia. |
| 7 | PPN default 11% dan dapat diubah di master pajak. | Tarif efektif umum yang berlaku. |
| 8 | Biaya persediaan dihitung berdasarkan urutan posting (perpetual). Transaksi bertanggal mundur tidak menghitung ulang biaya transaksi setelahnya. | Menjaga konsistensi kartu stok & jurnal tanpa re-costing massal. |
| 9 | Ongkos angkut pembelian dikapitalisasi saat penerimaan barang (dialokasikan proporsional nilai). | Sesuai PSAK 14: biaya angkut bagian dari biaya perolehan persediaan. |
| 10 | Selisih harga faktur vs penerimaan dibukukan ke akun Selisih Harga Bahan (mapping PURCHASE_PRICE_VARIANCE), toleransi 5% tanpa persetujuan. | Sederhana & dapat dianalisis; stok yang sudah keluar tidak di-revaluasi. |
| 11 | Transfer antar gudang tidak membuat jurnal karena akun persediaan ditentukan oleh tipe item, bukan gudang. | Nilai keluar = nilai masuk. |
| 12 | PPh 23 dipotong dari baris jasa (dasar = nilai jasa setelah porsi diskon). | Praktik umum pemotongan PPh 23 atas jasa. |
