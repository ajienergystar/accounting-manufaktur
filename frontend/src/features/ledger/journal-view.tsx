"use client";

import Link from "next/link";
import { useRouter } from "next/navigation";
import { useState } from "react";
import { useMutation, useQueryClient } from "@tanstack/react-query";
import { Printer, Undo2 } from "lucide-react";
import { toast } from "sonner";
import { Attachments } from "@/components/shared/attachments";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { Card, CardContent } from "@/components/ui/card";
import { Dialog } from "@/components/ui/dialog";
import { FormField } from "@/components/ui/form-field";
import { Input } from "@/components/ui/input";
import { Textarea } from "@/components/ui/textarea";
import { useAuth } from "@/features/auth/auth-context";
import { api, errorMessage } from "@/lib/api";
import { formatAmount, formatDate, formatDateTime, todayIso } from "@/lib/format";
import { STATUS_META, type JournalDetail } from "./types";

const REVERSIBLE = ["MANUAL", "RECURRING", "OPENING_BALANCE"];

export function JournalView({ journal }: { journal: JournalDetail }) {
  const router = useRouter();
  const qc = useQueryClient();
  const { can } = useAuth();
  const [reversing, setReversing] = useState(false);
  const [date, setDate] = useState(todayIso());
  const [reason, setReason] = useState("");

  const reverse = useMutation({
    mutationFn: () => api.post<JournalDetail>(`/journals/${journal.id}/reverse`, { date, reason }),
    onSuccess: (rev) => {
      toast.success(`Jurnal pembalik ${rev.journalNo} dibuat`);
      qc.invalidateQueries({ queryKey: ["journals"] });
      qc.invalidateQueries({ queryKey: ["journal"] });
      router.push(`/jurnal/${rev.id}`);
    },
    onError: (e) => toast.error(errorMessage(e)),
  });

  const canReverse =
    journal.status === "POSTED" &&
    !journal.reversedById &&
    journal.journalType !== "REVERSAL" &&
    REVERSIBLE.includes(journal.sourceType) &&
    can("LEDGER:POST");

  return (
    <div className="space-y-6">
      <div className="no-print flex flex-wrap justify-end gap-2">
        <Button variant="outline" onClick={() => window.print()}>
          <Printer className="h-4 w-4" /> Cetak
        </Button>
        {canReverse && (
          <Button variant="outline" onClick={() => setReversing(true)}>
            <Undo2 className="h-4 w-4" /> Buat jurnal pembalik
          </Button>
        )}
      </div>

      <Card>
        <CardContent className="grid gap-x-8 gap-y-3 pt-5 text-sm sm:grid-cols-2 lg:grid-cols-4">
          <Info label="No. jurnal" value={<span className="font-mono">{journal.journalNo}</span>} />
          <Info label="Tanggal" value={formatDate(journal.journalDate)} />
          <Info label="Periode" value={journal.periodName} />
          <Info label="Status" value={<Badge tone={STATUS_META[journal.status].tone}>{STATUS_META[journal.status].label}</Badge>} />
          <Info label="Tipe" value={journal.journalTypeLabel} />
          <Info label="Sumber" value={`${journal.sourceTypeLabel}${journal.sourceNo ? ` · ${journal.sourceNo}` : ""}`} />
          <Info label="Diposting" value={journal.postedBy ? `${journal.postedBy} · ${formatDateTime(journal.postedAt)}` : "-"} />
          <Info label="Dibuat" value={`${journal.createdBy ?? "-"} · ${formatDateTime(journal.createdAt)}`} />
          <div className="sm:col-span-2 lg:col-span-4">
            <Info label="Keterangan" value={journal.description} />
          </div>
          {journal.reversalOfId && (
            <p className="text-warning sm:col-span-2 lg:col-span-4">
              Jurnal ini membalik{" "}
              <Link className="font-medium underline" href={`/jurnal/${journal.reversalOfId}`}>
                {journal.reversalOfNo}
              </Link>
            </p>
          )}
          {journal.reversedById && (
            <p className="text-warning sm:col-span-2 lg:col-span-4">
              Jurnal ini sudah dibalik oleh{" "}
              <Link className="font-medium underline" href={`/jurnal/${journal.reversedById}`}>
                {journal.reversedByNo}
              </Link>
            </p>
          )}
        </CardContent>
      </Card>

      <Card>
        <div className="overflow-x-auto">
          <table className="w-full text-sm">
            <thead>
              <tr className="border-b border-border bg-surface-muted/60 text-left text-xs uppercase tracking-wide text-muted">
                <th className="px-4 py-2.5">#</th>
                <th className="px-4 py-2.5">Akun</th>
                <th className="px-4 py-2.5">Departemen</th>
                <th className="px-4 py-2.5">Keterangan</th>
                <th className="px-4 py-2.5 text-right">Debit</th>
                <th className="px-4 py-2.5 text-right">Kredit</th>
              </tr>
            </thead>
            <tbody>
              {journal.lines.map((l) => (
                <tr key={l.id} className="border-b border-border">
                  <td className="px-4 py-2 text-muted">{l.lineNo}</td>
                  <td className={Number(l.credit) > 0 ? "px-4 py-2 pl-10" : "px-4 py-2"}>
                    <span className="font-mono text-xs">{l.accountCode}</span> {l.accountName}
                  </td>
                  <td className="px-4 py-2 text-muted">{l.departmentName ?? "-"}</td>
                  <td className="px-4 py-2 text-muted">{l.description}</td>
                  <td className="num px-4 py-2">{Number(l.debit) ? formatAmount(l.debit) : ""}</td>
                  <td className="num px-4 py-2">{Number(l.credit) ? formatAmount(l.credit) : ""}</td>
                </tr>
              ))}
            </tbody>
            <tfoot>
              <tr className="font-semibold">
                <td colSpan={4} className="px-4 py-3 text-right">
                  Total
                </td>
                <td className="num px-4 py-3">{formatAmount(journal.totalDebit)}</td>
                <td className="num px-4 py-3">{formatAmount(journal.totalCredit)}</td>
              </tr>
            </tfoot>
          </table>
        </div>
      </Card>

      <Attachments entityType="JOURNAL" entityId={journal.id} canEdit={can("LEDGER:EDIT")} />

      <Dialog
        open={reversing}
        onClose={() => setReversing(false)}
        title="Buat jurnal pembalik"
        description="Jurnal asli tetap tersimpan. Sistem membuat jurnal baru dengan debit/kredit dibalik."
        footer={
          <>
            <Button variant="outline" onClick={() => setReversing(false)}>
              Batal
            </Button>
            <Button onClick={() => reverse.mutate()} loading={reverse.isPending} disabled={!reason.trim()}>
              Buat pembalik
            </Button>
          </>
        }
      >
        <div className="space-y-4">
          <FormField label="Tanggal jurnal pembalik" htmlFor="rev-date">
            <Input id="rev-date" type="date" value={date} onChange={(e) => setDate(e.target.value)} />
          </FormField>
          <FormField label="Alasan" htmlFor="rev-reason" required>
            <Textarea id="rev-reason" value={reason} onChange={(e) => setReason(e.target.value)} />
          </FormField>
        </div>
      </Dialog>
    </div>
  );
}

function Info({ label, value }: { label: string; value: React.ReactNode }) {
  return (
    <div>
      <p className="text-xs uppercase tracking-wide text-muted">{label}</p>
      <div className="mt-0.5">{value}</div>
    </div>
  );
}
