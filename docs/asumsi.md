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
