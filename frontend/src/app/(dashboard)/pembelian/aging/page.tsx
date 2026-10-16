"use client";

import { useState } from "react";
import { useQuery } from "@tanstack/react-query";
import { Download, Printer } from "lucide-react";
import { PageHeader } from "@/components/shared/page-header";
import { EmptyState, ErrorState } from "@/components/shared/states";
import { Button } from "@/components/ui/button";
import { Card, CardContent } from "@/components/ui/card";
import { FormField } from "@/components/ui/form-field";
import { Input } from "@/components/ui/input";
import { Skeleton } from "@/components/ui/skeleton";
import { RequirePermission } from "@/features/auth/require-permission";
import { api, errorMessage } from "@/lib/api";
import { csvNumber, exportCsv } from "@/lib/export";
import { formatAmount, formatDate, todayIso } from "@/lib/format";

interface AgingRow {
  supplierId?: number | null;
  customerId?: number | null;
  supplierCode?: string | null;
  supplierName?: string;
  customerCode?: string | null;
  customerName?: string;
  notDue: string;
  d1to30: string;
  d31to60: string;
  d61to90: string;
  over90: string;
  total: string;
}

const BUCKETS: { key: keyof AgingRow; label: string }[] = [
  { key: "notDue", label: "Belum jatuh tempo" },
  { key: "d1to30", label: "1–30 hari" },
  { key: "d31to60", label: "31–60 hari" },
  { key: "d61to90", label: "61–90 hari" },
  { key: "over90", label: "> 90 hari" },
];

function Aging() {
  const [asOf, setAsOf] = useState(todayIso());
  const { data, isLoading, error, refetch } = useQuery({
    queryKey: ["/purchasing/aging", asOf],
    queryFn: () => api.get<{ asOf: string; rows: AgingRow[]; totals: AgingRow }>("/purchasing/aging", { asOf }),
  });
  return (
    <>
      <PageHeader
        title="Umur Utang (AP Aging)"
        description="Sisa utang usaha dikelompokkan berdasarkan lama lewat jatuh tempo."
        actions={
          data && (
            <>
              <Button
                variant="outline"
                onClick={() =>
                  exportCsv(`ap-aging-${asOf}`, ["Pemasok", ...BUCKETS.map((b) => b.label), "Total"], data.rows.map((r) => [r.supplierName ?? "", ...BUCKETS.map((b) => csvNumber(r[b.key] as string)), csvNumber(r.total)]))
                }
              >
                <Download className="h-4 w-4" /> CSV
              </Button>
              <Button variant="outline" onClick={() => window.print()}>
                <Printer className="h-4 w-4" /> Cetak
              </Button>
            </>
          )
        }
      />
      <Card className="no-print mb-6">
        <CardContent className="pt-5">
          <FormField label="Per tanggal" htmlFor="asof" className="w-48">
            <Input id="asof" type="date" value={asOf} onChange={(e) => setAsOf(e.target.value)} />
          </FormField>
        </CardContent>
      </Card>
      {isLoading ? (
        <Skeleton className="h-64 w-full" />
      ) : error ? (
        <ErrorState message={errorMessage(error)} onRetry={refetch} />
      ) : !data?.rows.length ? (
        <Card>
          <EmptyState title="Tidak ada utang terbuka" />
        </Card>
      ) : (
        <Card>
          <p className="border-b border-border px-5 py-3 text-sm text-muted">Per {formatDate(data.asOf)}</p>
          <div className="overflow-x-auto">
            <table className="w-full min-w-[900px] text-sm">
              <thead>
                <tr className="border-b border-border bg-surface-muted/60 text-xs uppercase text-muted">
                  <th className="px-4 py-2.5 text-left">Pemasok</th>
                  {BUCKETS.map((b) => (
                    <th key={b.key} className="px-4 py-2.5 text-right">
                      {b.label}
                    </th>
                  ))}
                  <th className="px-4 py-2.5 text-right">Total</th>
                </tr>
              </thead>
              <tbody>
                {data.rows.map((r) => (
                  <tr key={r.supplierId} className="border-b border-border">
                    <td className="px-4 py-2">{r.supplierName}</td>
                    {BUCKETS.map((b) => (
                      <td key={b.key} className={`num px-4 py-2 ${b.key === "over90" && Number(r.over90) > 0 ? "font-semibold text-danger" : ""}`}>
                        {Number(r[b.key]) ? formatAmount(r[b.key] as string) : ""}
                      </td>
                    ))}
                    <td className="num px-4 py-2 font-medium">{formatAmount(r.total)}</td>
                  </tr>
                ))}
              </tbody>
              <tfoot>
                <tr className="font-semibold">
                  <td className="px-4 py-3">Total</td>
                  {BUCKETS.map((b) => (
                    <td key={b.key} className="num px-4 py-3">
                      {formatAmount(data.totals[b.key] as string)}
                    </td>
                  ))}
                  <td className="num px-4 py-3">{formatAmount(data.totals.total)}</td>
                </tr>
              </tfoot>
            </table>
          </div>
        </Card>
      )}
    </>
  );
}

export default function Page() {
  return (
    <RequirePermission permission="PURCHASE:VIEW">
      <Aging />
    </RequirePermission>
  );
}
