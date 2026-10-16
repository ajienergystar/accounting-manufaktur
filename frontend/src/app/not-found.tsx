import Link from "next/link";

export default function NotFound() {
  return (
    <main className="flex min-h-screen flex-col items-center justify-center gap-3 p-6 text-center">
      <p className="text-5xl font-bold text-primary">404</p>
      <h1 className="text-lg font-semibold">Halaman tidak ditemukan</h1>
      <p className="text-sm text-muted">Halaman yang kamu cari tidak ada atau sudah dipindahkan.</p>
      <Link href="/dashboard" className="mt-2 text-sm font-medium text-primary hover:underline">
        Kembali ke Dashboard
      </Link>
    </main>
  );
}
