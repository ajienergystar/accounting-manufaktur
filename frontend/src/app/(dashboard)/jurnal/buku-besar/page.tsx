"use client";

import Link from "next/link";
import { useSearchParams } from "next/navigation";
import { Suspense, useState } from "react";
import { useQuery } from "@tanstack/react-query";
import { Download, Printer } from "lucide-react";
import { AccountPicker } from "@/components/shared/account-picker";
import { PageHeader } from "@/components/shared/page-header";
import { EmptyState, ErrorState } from "@/components/shared/states";
import { Button } from "@/components/ui/button";
import { Card, CardContent } from "@/components/ui/card";
import { FormField } from "@/components/ui/form-field";
import { Input } from "@/components/ui/input";
import { Select } from "@/components/ui/select";
import { Skeleton } from "@/components/ui/skeleton";
import { RequirePermission } from "@/features/auth/require-permission";
import { useOptions } from "@/features/master/use-options";
import { api, errorMessage } from "@/lib/api";
import { monthRange } from "@/lib/dates";
import { csvNumber, exportCsv } from "@/lib/export";
import { formatAmount, formatDate } from "@/lib/format";

interface LedgerLine {
  journalId: number;
  journalNo: string;
  date: string;
  journalType: string;
  sourceType: string;
  sourceNo: string | null;
  description: string;
  departmentName: string | null;
  debit: string;
  credit: string;
  balance: string;
}
interface GeneralLedger {
  accountId: number;
  accountCode: string;
  accountName: string;
  normalBalance: "DEBIT" | "CREDIT";
  from: string;
  to: string;
  openingBalance: string;
  totalDebit: string;
  totalCredit: string;
  closingBalance: string;
  lines: LedgerLine[];
}

function GeneralLedgerPage() {
  const params = useSearchParams();
  const initial = monthRange();
  const [accountId, setAccountId] = useState<number | null>(params.get("accountId") ? Number(params.get("accountId")) : null);
  const [from, setFrom] = useState(params.get("from") ?? initial.from);
  const [to, setTo] = useState(params.get("to") ?? initial.to);
  const [dept, setDept] = useState("");
  const { data: departments } = useOptions("/departments");

  const { data, isFetching, error, refetch } = useQuery({
    queryKey: ["ledger", "gl", { accountId, from, to, dept }],
    queryFn: () =>
      api.get<GeneralLedger>("/ledger/general-ledger", { accountId, from, to, departmentId: dept || undefined }),
    enabled: accountId !== null && !!from && !!to,
  });

  const exportIt = () =>
    data &&
    exportCsv(
      `buku-besar-${data.accountCode}-${from}-${to}`,
      ["Tanggal", "No. Jurnal", "Keterangan", "Departemen", "Debit", "Kredit", "Saldo"],
      [
        ["", "", "Saldo awal", "", "", "", csvNumber(data.openingBalance)],
        ...data.lines.map((l) => [l.date, l.journalNo, l.description, l.departmentName ?? "", csvNumber(l.debit), csvNumber(l.credit), csvNumber(l.balance)]),
      ],
    );

  return (
    <>
      <PageHeader
        title="Buku Besar"
        description="Mutasi dan saldo berjalan per akun."
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
        <CardContent className="grid gap-4 pt-5 md:grid-cols-[1fr_160px_160px_200px]">
          <FormField label="Akun" htmlFor="gl-account">
            <AccountPicker id="gl-account" value={accountId} onChange={setAccountId} />
          </FormField>
          <FormField label="Dari" htmlFor="gl-from">
            <Input id="gl-from" type="date" value={from} onChange={(e) => setFrom(e.target.value)} />
          </FormField>
          <FormField label="Sampai" htmlFor="gl-to">
            <Input id="gl-to" type="date" value={to} onChange={(e) => setTo(e.target.value)} />
          </FormField>
          <FormField label="Departemen" htmlFor="gl-dept">
            <Select id="gl-dept" value={dept} onChange={(e) => setDept(e.target.value)}>
              <option value="">Semua</option>
              {departments?.map((d) => (
                <option key={d.id} value={d.id}>
                  {d.code} {d.name}
                </option>
              ))}
            </Select>
          </FormField>
        </CardContent>
      </Card>

      {accountId === null ? (
        <Card>
          <EmptyState title="Pilih akun" description="Pilih akun dan rentang tanggal untuk menampilkan buku besar." />
        </Card>
      ) : isFetching && !data ? (
        <Skeleton className="h-80 w-full" />
      ) : error ? (
        <ErrorState message={errorMessage(error)} onRetry={refetch} />
      ) : data ? (
        <Card>
          <div className="border-b border-border px-5 py-4">
            <h2 className="text-lg font-semibold">
              <span className="font-mono">{data.accountCode}</span> {data.accountName}
            </h2>
            <p className="text-sm text-muted">
              {formatDate(data.from)} – {formatDate(data.to)} · Saldo normal {data.normalBalance === "DEBIT" ? "debit" : "kredit"}
            </p>
          </div>
          <div className="overflow-x-auto">
            <table className="w-full text-sm">
              <thead>
                <tr className="border-b border-border bg-surface-muted/60 text-left text-xs uppercase tracking-wide text-muted">
                  <th className="px-4 py-2.5">Tanggal</th>
                  <th className="px-4 py-2.5">No. Jurnal</th>
                  <th className="px-4 py-2.5">Keterangan</th>
                  <th className="px-4 py-2.5 text-right">Debit</th>
                  <th className="px-4 py-2.5 text-right">Kredit</th>
                  <th className="px-4 py-2.5 text-right">Saldo</th>
                </tr>
              </thead>
              <tbody>
                <tr className="border-b border-border bg-surface-muted/30 font-medium">
                  <td className="px-4 py-2" colSpan={5}>
                    Saldo awal
                  </td>
                  <td className="num px-4 py-2">{formatAmount(data.openingBalance, { parentheses: true })}</td>
                </tr>
                {data.lines.map((l, i) => (
                  <tr key={`${l.journalId}-${i}`} className="border-b border-border hover:bg-surface-muted/50">
                    <td className="whitespace-nowrap px-4 py-2">{formatDate(l.date)}</td>
                    <td className="px-4 py-2">
                      <Link href={`/jurnal/${l.journalId}`} className="font-mono text-xs text-primary hover:underline">
                        {l.journalNo}
                      </Link>
                    </td>
                    <td className="px-4 py-2">
                      {l.description}
                      {(l.sourceNo || l.departmentName) && (
                        <span className="block text-xs text-muted">
                          {[l.sourceNo, l.departmentName].filter(Boolean).join(" · ")}
                        </span>
                      )}
                    </td>
                    <td className="num px-4 py-2">{Number(l.debit) ? formatAmount(l.debit) : ""}</td>
                    <td className="num px-4 py-2">{Number(l.credit) ? formatAmount(l.credit) : ""}</td>
                    <td className="num px-4 py-2">{formatAmount(l.balance, { parentheses: true })}</td>
                  </tr>
                ))}
              </tbody>
              <tfoot>
                <tr className="font-semibold">
                  <td className="px-4 py-3" colSpan={3}>
                    Total mutasi & saldo akhir
                  </td>
                  <td className="num px-4 py-3">{formatAmount(data.totalDebit)}</td>
                  <td className="num px-4 py-3">{formatAmount(data.totalCredit)}</td>
                  <td className="num px-4 py-3">{formatAmount(data.closingBalance, { parentheses: true })}</td>
                </tr>
              </tfoot>
            </table>
          </div>
          {data.lines.length === 0 && <p className="px-5 py-4 text-sm text-muted">Tidak ada mutasi pada rentang ini.</p>}
        </Card>
      ) : null}
    </>
  );
}

export default function Page() {
  return (
    <RequirePermission permission="LEDGER:VIEW">
      <Suspense fallback={<Skeleton className="h-80 w-full" />}>
        <GeneralLedgerPage />
      </Suspense>
    </RequirePermission>
  );
}
