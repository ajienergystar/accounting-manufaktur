"use client";

import { useRouter } from "next/navigation";
import { useState } from "react";
import { useQuery } from "@tanstack/react-query";
import { CheckCircle2, Download, Printer, XCircle } from "lucide-react";
import { PageHeader } from "@/components/shared/page-header";
import { ErrorState } from "@/components/shared/states";
import { Button } from "@/components/ui/button";
import { Card, CardContent } from "@/components/ui/card";
import { FormField } from "@/components/ui/form-field";
import { Input } from "@/components/ui/input";
import { Skeleton } from "@/components/ui/skeleton";
import { useAuth } from "@/features/auth/auth-context";
import { RequirePermission } from "@/features/auth/require-permission";
import { api, errorMessage } from "@/lib/api";
import { monthRange } from "@/lib/dates";
import { csvNumber, exportCsv } from "@/lib/export";
import { formatAmount, formatDate } from "@/lib/format";
import { cn } from "@/lib/utils";

interface Row {
  accountId: number;
  code: string;
  name: string;
  accountTypeLabel: string;
  openingDebit: string;
  openingCredit: string;
  mutationDebit: string;
  mutationCredit: string;
  closingDebit: string;
  closingCredit: string;
}
interface TrialBalance {
  from: string;
  to: string;
  rows: Row[];
  totalOpeningDebit: string;
  totalOpeningCredit: string;
  totalMutationDebit: string;
  totalMutationCredit: string;
  totalClosingDebit: string;
  totalClosingCredit: string;
  balanced: boolean;
}

const amt = (v: string) => (Number(v) ? formatAmount(v) : "");

function TrialBalancePage() {
  const router = useRouter();
  const { me } = useAuth();
  const initial = monthRange();
  const [from, setFrom] = useState(initial.from);
  const [to, setTo] = useState(initial.to);
  const [includeZero, setIncludeZero] = useState(false);

  const { data, isLoading, error, refetch } = useQuery({
    queryKey: ["ledger", "tb", { from, to, includeZero }],
    queryFn: () => api.get<TrialBalance>("/ledger/trial-balance", { from, to, includeZero }),
    enabled: !!from && !!to,
  });

  const exportIt = () =>
    data &&
    exportCsv(
      `neraca-saldo-${from}-${to}`,
      ["Kode", "Nama Akun", "Saldo Awal Debit", "Saldo Awal Kredit", "Mutasi Debit", "Mutasi Kredit", "Saldo Akhir Debit", "Saldo Akhir Kredit"],
      data.rows.map((r) => [
        r.code,
        r.name,
        csvNumber(r.openingDebit),
        csvNumber(r.openingCredit),
        csvNumber(r.mutationDebit),
        csvNumber(r.mutationCredit),
        csvNumber(r.closingDebit),
        csvNumber(r.closingCredit),
      ]),
    );

  return (
    <>
      <PageHeader
        title="Neraca Saldo"
        description="Trial balance: saldo awal, mutasi, dan saldo akhir seluruh akun."
        actions={
          data && (
            <>
              <Button variant="outline" onClick={exportIt}>
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
        <CardContent className="flex flex-wrap items-end gap-4 pt-5">
          <FormField label="Dari" htmlFor="tb-from" className="w-44">
            <Input id="tb-from" type="date" value={from} onChange={(e) => setFrom(e.target.value)} />
          </FormField>
          <FormField label="Sampai" htmlFor="tb-to" className="w-44">
            <Input id="tb-to" type="date" value={to} onChange={(e) => setTo(e.target.value)} />
          </FormField>
          <label className="flex h-10 items-center gap-2 text-sm">
            <input type="checkbox" className="h-4 w-4 accent-primary" checked={includeZero} onChange={(e) => setIncludeZero(e.target.checked)} />
            Tampilkan akun bersaldo nol
          </label>
        </CardContent>
      </Card>

      {isLoading ? (
        <Skeleton className="h-96 w-full" />
      ) : error ? (
        <ErrorState message={errorMessage(error)} onRetry={refetch} />
      ) : data ? (
        <Card>
          <div className="border-b border-border px-5 py-4 text-center">
            <p className="font-semibold">{me?.company.name}</p>
            <h2 className="text-lg font-semibold">Neraca Saldo</h2>
            <p className="text-sm text-muted">
              Periode {formatDate(data.from)} s.d. {formatDate(data.to)}
            </p>
          </div>
          <div className="overflow-x-auto">
            <table className="w-full min-w-[960px] text-sm">
              <thead>
                <tr className="border-b border-border bg-surface-muted/60 text-xs uppercase tracking-wide text-muted">
                  <th rowSpan={2} className="px-4 py-2 text-left">
                    Akun
                  </th>
                  <th colSpan={2} className="border-l border-border px-4 py-2 text-center">
                    Saldo Awal
                  </th>
                  <th colSpan={2} className="border-l border-border px-4 py-2 text-center">
                    Mutasi
                  </th>
                  <th colSpan={2} className="border-l border-border px-4 py-2 text-center">
                    Saldo Akhir
                  </th>
                </tr>
                <tr className="border-b border-border bg-surface-muted/60 text-xs uppercase tracking-wide text-muted">
                  {["Debit", "Kredit", "Debit", "Kredit", "Debit", "Kredit"].map((h, i) => (
                    <th key={i} className={cn("px-4 py-2 text-right", i % 2 === 0 && "border-l border-border")}>
                      {h}
                    </th>
                  ))}
                </tr>
              </thead>
              <tbody>
                {data.rows.map((r) => (
                  <tr
                    key={r.accountId}
                    className="cursor-pointer border-b border-border hover:bg-surface-muted/50"
                    onClick={() => router.push(`/jurnal/buku-besar?accountId=${r.accountId}&from=${from}&to=${to}`)}
                  >
                    <td className="px-4 py-2">
                      <span className="font-mono text-xs">{r.code}</span> {r.name}
                    </td>
                    <td className="num border-l border-border px-4 py-2">{amt(r.openingDebit)}</td>
                    <td className="num px-4 py-2">{amt(r.openingCredit)}</td>
                    <td className="num border-l border-border px-4 py-2">{amt(r.mutationDebit)}</td>
                    <td className="num px-4 py-2">{amt(r.mutationCredit)}</td>
                    <td className="num border-l border-border px-4 py-2">{amt(r.closingDebit)}</td>
                    <td className="num px-4 py-2">{amt(r.closingCredit)}</td>
                  </tr>
                ))}
              </tbody>
              <tfoot>
                <tr className="border-t-2 border-foreground/30 font-semibold">
                  <td className="px-4 py-3">Total</td>
                  <td className="num border-l border-border px-4 py-3">{formatAmount(data.totalOpeningDebit)}</td>
                  <td className="num px-4 py-3">{formatAmount(data.totalOpeningCredit)}</td>
                  <td className="num border-l border-border px-4 py-3">{formatAmount(data.totalMutationDebit)}</td>
                  <td className="num px-4 py-3">{formatAmount(data.totalMutationCredit)}</td>
                  <td className="num border-l border-border px-4 py-3">{formatAmount(data.totalClosingDebit)}</td>
                  <td className="num px-4 py-3">{formatAmount(data.totalClosingCredit)}</td>
                </tr>
              </tfoot>
            </table>
          </div>
          <div
            role="status"
            className={cn(
              "flex items-center gap-2 border-t border-border px-5 py-3 text-sm font-medium",
              data.balanced ? "text-success" : "text-danger",
            )}
          >
            {data.balanced ? <CheckCircle2 className="h-4 w-4" /> : <XCircle className="h-4 w-4" />}
            {data.balanced ? "Neraca saldo seimbang" : "Neraca saldo TIDAK seimbang — periksa integritas data"}
          </div>
        </Card>
      ) : null}
    </>
  );
}

export default function Page() {
  return (
    <RequirePermission permission="LEDGER:VIEW">
      <TrialBalancePage />
    </RequirePermission>
  );
}
