import type { ReactNode } from "react";
import { cn } from "@/lib/utils";

export function Info({ label, value, className }: { label: string; value: ReactNode; className?: string }) {
  return (
    <div className={className}>
      <p className="text-xs uppercase tracking-wide text-muted">{label}</p>
      <div className="mt-0.5 text-sm">{value ?? "-"}</div>
    </div>
  );
}

export function InfoGrid({ children, className }: { children: ReactNode; className?: string }) {
  return <div className={cn("grid gap-x-8 gap-y-3 sm:grid-cols-2 lg:grid-cols-4", className)}>{children}</div>;
}

/** Ringkasan total dokumen di kanan bawah tabel baris. */
export function TotalsPanel({ rows }: { rows: { label: string; value: string; strong?: boolean; muted?: boolean }[] }) {
  return (
    <dl className="ml-auto w-full max-w-sm space-y-1.5 text-sm">
      {rows.map((r) => (
        <div key={r.label} className={cn("flex justify-between gap-4", r.strong && "border-t border-border pt-2 text-base font-semibold", r.muted && "text-muted")}>
          <dt>{r.label}</dt>
          <dd className="num">{r.value}</dd>
        </div>
      ))}
    </dl>
  );
}
