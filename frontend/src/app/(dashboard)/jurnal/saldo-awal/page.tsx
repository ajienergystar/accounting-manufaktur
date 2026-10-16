"use client";

import { useRouter } from "next/navigation";
import { useMemo, useState } from "react";
import { useQuery } from "@tanstack/react-query";
import { Info } from "lucide-react";
import { toast } from "sonner";
import { PageHeader } from "@/components/shared/page-header";
import { Button } from "@/components/ui/button";
import { Card, CardContent } from "@/components/ui/card";
import { ConfirmDialog } from "@/components/ui/confirm-dialog";
import { FormField } from "@/components/ui/form-field";
import { Input } from "@/components/ui/input";
import { MoneyInput } from "@/components/ui/money-input";
import { Skeleton } from "@/components/ui/skeleton";
import { RequirePermission } from "@/features/auth/require-permission";
import type { Account } from "@/features/coa/types";
import type { JournalDetail } from "@/features/ledger/types";
import { sumDecimal } from "@/features/ledger/types";
import { api, errorMessage } from "@/lib/api";
import { formatAmount } from "@/lib/format";

/**
 * Input saldo awal saat mulai memakai aplikasi. Selisih otomatis ke akun Ekuitas Saldo Awal (3-9000).
 */
function OpeningBalance() {
  const router = useRouter();
  const { data: accounts, isLoading } = useQuery({ queryKey: ["accounts", "tree"], queryFn: () => api.get<Account[]>("/accounts") });
  const [date, setDate] = useState(`${new Date().getFullYear()}-01-01`);
  const [values, setValues] = useState<Record<number, { debit: string; credit: string }>>({});
  const [confirm, setConfirm] = useState(false);
  const [saving, setSaving] = useState(false);

  const rows = useMemo(
    () => accounts?.filter((a) => ["ASSET", "LIABILITY", "EQUITY"].includes(a.accountType) && a.code !== "3-9000" && a.active) ?? [],
    [accounts],
  );
  const totalDebit = sumDecimal(Object.values(values).map((v) => v.debit));
  const totalCredit = sumDecimal(Object.values(values).map((v) => v.credit));
  const diff = Math.round((totalDebit - totalCredit) * 100) / 100;

  const set = (id: number, side: "debit" | "credit", v: string) =>
    setValues((s) => {
      const cur = s[id] ?? { debit: "", credit: "" };
      return { ...s, [id]: { ...cur, [side]: v } };
    });

  const save = async () => {
    setSaving(true);
    try {
      const lines = Object.entries(values)
        .filter(([, v]) => Number(v.debit || 0) > 0 || Number(v.credit || 0) > 0)
        .map(([id, v]) => ({ accountId: Number(id), debit: v.debit || "0", credit: v.credit || "0" }));
      const je = await api.post<JournalDetail>("/journals/opening-balance", { date, lines });
      toast.success(`Saldo awal diposting: ${je.journalNo}`);
      router.push(`/jurnal/${je.id}`);
    } catch (e) {
      toast.error(errorMessage(e));
    } finally {
      setSaving(false);
      setConfirm(false);
    }
  };

  if (isLoading) return <Skeleton className="h-96 w-full" />;

  return (
    <>
      <Card className="mb-6">
        <CardContent className="flex flex-wrap items-end gap-6 pt-5">
          <FormField label="Tanggal saldo awal" htmlFor="ob-date" className="w-48">
            <Input id="ob-date" type="date" value={date} onChange={(e) => setDate(e.target.value)} />
          </FormField>
          <p className="flex max-w-xl items-start gap-2 text-sm text-muted">
            <Info className="mt-0.5 h-4 w-4 shrink-0" />
            Isi saldo akun neraca per tanggal mulai. Selisih debit-kredit otomatis dibukukan ke akun 3-9000 Ekuitas Saldo
            Awal. Saldo persediaan per item diinput di modul Persediaan.
          </p>
        </CardContent>
      </Card>
      <Card>
        <div className="overflow-x-auto">
          <table className="w-full min-w-[720px] text-sm">
            <thead>
              <tr className="border-b border-border bg-surface-muted/60 text-left text-xs uppercase tracking-wide text-muted">
                <th className="px-4 py-2.5">Akun</th>
                <th className="w-48 px-4 py-2.5 text-right">Debit</th>
                <th className="w-48 px-4 py-2.5 text-right">Kredit</th>
              </tr>
            </thead>
            <tbody>
              {rows.map((a) =>
                a.header ? (
                  <tr key={a.id} className="border-b border-border bg-surface-muted/30">
                    <td colSpan={3} className="px-4 py-2 font-semibold" style={{ paddingLeft: 16 + (a.level - 1) * 16 }}>
                      {a.code} {a.name}
                    </td>
                  </tr>
                ) : (
                  <tr key={a.id} className="border-b border-border">
                    <td className="px-4 py-1.5" style={{ paddingLeft: 16 + (a.level - 1) * 16 }}>
                      <span className="font-mono text-xs">{a.code}</span> {a.name}
                    </td>
                    <td className="px-4 py-1.5">
                      <MoneyInput
                        aria-label={`Debit ${a.code}`}
                        value={values[a.id]?.debit ?? ""}
                        onValueChange={(v) => set(a.id, "debit", v)}
                      />
                    </td>
                    <td className="px-4 py-1.5">
                      <MoneyInput
                        aria-label={`Kredit ${a.code}`}
                        value={values[a.id]?.credit ?? ""}
                        onValueChange={(v) => set(a.id, "credit", v)}
                      />
                    </td>
                  </tr>
                ),
              )}
            </tbody>
            <tfoot>
              <tr className="font-semibold">
                <td className="px-4 py-3">Total</td>
                <td className="num px-4 py-3">{formatAmount(totalDebit)}</td>
                <td className="num px-4 py-3">{formatAmount(totalCredit)}</td>
              </tr>
              <tr className="text-muted">
                <td className="px-4 pb-3">Penyeimbang ke 3-9000 Ekuitas Saldo Awal</td>
                <td className="num px-4 pb-3">{diff < 0 ? formatAmount(-diff) : ""}</td>
                <td className="num px-4 pb-3">{diff > 0 ? formatAmount(diff) : ""}</td>
              </tr>
            </tfoot>
          </table>
        </div>
      </Card>
      <div className="mt-6 flex justify-end">
        <Button onClick={() => setConfirm(true)} disabled={totalDebit === 0 && totalCredit === 0}>
          Posting saldo awal
        </Button>
      </div>
      <ConfirmDialog
        open={confirm}
        title="Posting saldo awal?"
        message="Jurnal saldo awal akan diposting. Koreksi dilakukan dengan membuat jurnal pembalik lalu input ulang."
        confirmLabel="Posting"
        loading={saving}
        onConfirm={save}
        onClose={() => setConfirm(false)}
      />
    </>
  );
}

export default function Page() {
  return (
    <RequirePermission permission="LEDGER:POST">
      <PageHeader title="Saldo Awal" description="Input saldo awal akun neraca saat mulai memakai aplikasi." />
      <OpeningBalance />
    </RequirePermission>
  );
}
