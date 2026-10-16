import { Factory, ShieldCheck, LineChart, Layers } from "lucide-react";
import { ThemeToggle } from "@/components/layout/theme-toggle";

const FEATURES = [
  { icon: Layers, text: "Job order, process costing & standard costing dalam satu sistem" },
  { icon: LineChart, text: "Laporan HPP, laba rugi, dan neraca otomatis dari jurnal" },
  { icon: ShieldCheck, text: "Jurnal double-entry tervalidasi, periode terkunci, audit trail lengkap" },
];

export default function AuthLayout({ children }: LayoutProps<"/">) {
  return (
    <div className="grid min-h-screen lg:grid-cols-[1.05fr_1fr]">
      <aside className="relative hidden overflow-hidden bg-sidebar p-12 text-sidebar-foreground lg:flex lg:flex-col">
        <div
          aria-hidden
          className="absolute inset-0 opacity-[0.07]"
          style={{
            backgroundImage:
              "linear-gradient(to right, #fff 1px, transparent 1px), linear-gradient(to bottom, #fff 1px, transparent 1px)",
            backgroundSize: "36px 36px",
          }}
        />
        <div className="relative flex items-center gap-3">
          <span className="flex h-10 w-10 items-center justify-center rounded-xl bg-primary text-primary-foreground">
            <Factory className="h-5 w-5" />
          </span>
          <span className="text-lg font-semibold text-white">Akuntansi Manufaktur</span>
        </div>

        <div className="relative mt-auto max-w-md">
          <h2 className="text-3xl font-semibold leading-tight text-white">
            Pembukuan pabrik yang rapi, dari bahan baku sampai laporan keuangan.
          </h2>
          <ul className="mt-8 space-y-4">
            {FEATURES.map(({ icon: Icon, text }) => (
              <li key={text} className="flex items-start gap-3">
                <span className="mt-0.5 flex h-7 w-7 shrink-0 items-center justify-center rounded-lg bg-white/10">
                  <Icon className="h-4 w-4 text-white" />
                </span>
                <span className="text-sm leading-relaxed">{text}</span>
              </li>
            ))}
          </ul>
        </div>

        <figure className="relative mt-12 rounded-xl border border-white/10 bg-white/5 p-5 font-mono text-xs">
          <figcaption className="mb-3 font-sans text-xs uppercase tracking-wider text-sidebar-foreground/70">
            Contoh jurnal otomatis — penyelesaian produksi
          </figcaption>
          <div className="grid grid-cols-[1fr_auto_auto] gap-x-6 gap-y-1 text-white/90">
            <span>Persediaan Barang Jadi</span>
            <span className="text-right">48.500.000</span>
            <span />
            <span className="pl-6">Barang Dalam Proses</span>
            <span />
            <span className="text-right">48.500.000</span>
          </div>
        </figure>
      </aside>

      <main className="relative flex items-center justify-center px-4 py-10 sm:px-8">
        <div className="absolute right-4 top-4">
          <ThemeToggle />
        </div>
        <div className="w-full max-w-md">{children}</div>
      </main>
    </div>
  );
}
