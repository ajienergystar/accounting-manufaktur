"use client";

import { useState } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { Controller, useFieldArray, useForm, useWatch } from "react-hook-form";
import { CalendarClock, Pencil, Play, Plus, Trash2 } from "lucide-react";
import { toast } from "sonner";
import { AccountPicker } from "@/components/shared/account-picker";
import { PageHeader } from "@/components/shared/page-header";
import { EmptyState } from "@/components/shared/states";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { Card } from "@/components/ui/card";
import { ConfirmDialog } from "@/components/ui/confirm-dialog";
import { Dialog } from "@/components/ui/dialog";
import { FormField } from "@/components/ui/form-field";
import { Input } from "@/components/ui/input";
import { MoneyInput } from "@/components/ui/money-input";
import { Skeleton } from "@/components/ui/skeleton";
import { useAuth } from "@/features/auth/auth-context";
import { RequirePermission } from "@/features/auth/require-permission";
import { sumDecimal, type JournalLine } from "@/features/ledger/types";
import { api, errorMessage } from "@/lib/api";
import { formatAmount, formatDate, todayIso } from "@/lib/format";

interface Recurring {
  id: number;
  name: string;
  description: string;
  dayOfMonth: number;
  nextRunDate: string;
  endDate: string | null;
  autoPost: boolean;
  active: boolean;
  lastRunAt: string | null;
  amount: string;
  lines: JournalLine[];
}

interface FormValues {
  name: string;
  description: string;
  dayOfMonth: string;
  startDate: string;
  endDate: string;
  autoPost: boolean;
  active: boolean;
  lines: { accountId: number | null; description: string; debit: string; credit: string }[];
}

function RecurringForm({ editing, onClose }: { editing: Recurring | null; onClose: () => void }) {
  const qc = useQueryClient();
  const { register, control, handleSubmit, formState } = useForm<FormValues>({
    defaultValues: editing
      ? {
          name: editing.name,
          description: editing.description,
          dayOfMonth: String(editing.dayOfMonth),
          startDate: editing.nextRunDate,
          endDate: editing.endDate ?? "",
          autoPost: editing.autoPost,
          active: editing.active,
          lines: editing.lines.map((l) => ({
            accountId: l.accountId,
            description: l.description ?? "",
            debit: Number(l.debit) ? l.debit : "",
            credit: Number(l.credit) ? l.credit : "",
          })),
        }
      : {
          name: "",
          description: "",
          dayOfMonth: "1",
          startDate: todayIso(),
          endDate: "",
          autoPost: false,
          active: true,
          lines: [
            { accountId: null, description: "", debit: "", credit: "" },
            { accountId: null, description: "", debit: "", credit: "" },
          ],
        },
  });
  const { fields, append, remove } = useFieldArray({ control, name: "lines" });
  const lines = useWatch({ control, name: "lines" });
  const d = sumDecimal(lines.map((l) => l.debit));
  const c = sumDecimal(lines.map((l) => l.credit));

  const onSubmit = handleSubmit(async (v) => {
    const body = {
      ...v,
      dayOfMonth: Number(v.dayOfMonth),
      endDate: v.endDate || null,
      lines: v.lines.map((l) => ({ ...l, debit: l.debit || "0", credit: l.credit || "0", departmentId: null })),
    };
    try {
      if (editing) await api.put(`/recurring-journals/${editing.id}`, body);
      else await api.post("/recurring-journals", body);
      toast.success("Template disimpan");
      qc.invalidateQueries({ queryKey: ["recurring"] });
      onClose();
    } catch (e) {
      toast.error(errorMessage(e));
    }
  });

  return (
    <Dialog
      open
      onClose={onClose}
      size="xl"
      title={editing ? "Ubah template jurnal berulang" : "Template jurnal berulang baru"}
      footer={
        <>
          <Button variant="outline" onClick={onClose}>
            Batal
          </Button>
          <Button type="submit" form="rec-form" loading={formState.isSubmitting} disabled={d !== c || d === 0}>
            Simpan
          </Button>
        </>
      }
    >
      <form id="rec-form" onSubmit={onSubmit} className="space-y-4" noValidate>
        <div className="grid gap-4 md:grid-cols-2">
          <FormField label="Nama template" htmlFor="r-name" required>
            <Input id="r-name" {...register("name", { required: true })} />
          </FormField>
          <FormField label="Keterangan jurnal" htmlFor="r-desc" required>
            <Input id="r-desc" {...register("description", { required: true })} />
          </FormField>
          <FormField label="Tanggal setiap bulan (1-28)" htmlFor="r-day">
            <Input id="r-day" type="number" min={1} max={28} {...register("dayOfMonth")} />
          </FormField>
          <div className="grid grid-cols-2 gap-4">
            <FormField label="Mulai" htmlFor="r-start">
              <Input id="r-start" type="date" {...register("startDate")} />
            </FormField>
            <FormField label="Berakhir (opsional)" htmlFor="r-end">
              <Input id="r-end" type="date" {...register("endDate")} />
            </FormField>
          </div>
        </div>
        <div className="flex gap-6 text-sm">
          <label className="flex items-center gap-2">
            <input type="checkbox" className="h-4 w-4 accent-primary" {...register("autoPost")} /> Langsung posting
          </label>
          <label className="flex items-center gap-2">
            <input type="checkbox" className="h-4 w-4 accent-primary" {...register("active")} /> Aktif
          </label>
        </div>
        <table className="w-full text-sm">
          <thead>
            <tr className="text-left text-xs uppercase text-muted">
              <th className="py-2">Akun</th>
              <th className="py-2">Keterangan</th>
              <th className="w-36 py-2 text-right">Debit</th>
              <th className="w-36 py-2 text-right">Kredit</th>
              <th className="w-10" />
            </tr>
          </thead>
          <tbody>
            {fields.map((f, i) => (
              <tr key={f.id}>
                <td className="py-1 pr-2">
                  <Controller
                    control={control}
                    name={`lines.${i}.accountId`}
                    render={({ field }) => <AccountPicker value={field.value} onChange={field.onChange} aria-label={`Akun ${i + 1}`} />}
                  />
                </td>
                <td className="py-1 pr-2">
                  <Input aria-label={`Keterangan ${i + 1}`} {...register(`lines.${i}.description`)} />
                </td>
                <td className="py-1 pr-2">
                  <Controller
                    control={control}
                    name={`lines.${i}.debit`}
                    render={({ field }) => <MoneyInput value={field.value} onValueChange={field.onChange} aria-label={`Debit ${i + 1}`} />}
                  />
                </td>
                <td className="py-1 pr-2">
                  <Controller
                    control={control}
                    name={`lines.${i}.credit`}
                    render={({ field }) => <MoneyInput value={field.value} onValueChange={field.onChange} aria-label={`Kredit ${i + 1}`} />}
                  />
                </td>
                <td>
                  <Button size="icon" variant="ghost" onClick={() => remove(i)} disabled={fields.length <= 2} aria-label="Hapus baris">
                    <Trash2 className="h-4 w-4 text-danger" />
                  </Button>
                </td>
              </tr>
            ))}
          </tbody>
          <tfoot>
            <tr className="font-semibold">
              <td colSpan={2} className="pt-2">
                <Button size="sm" variant="outline" onClick={() => append({ accountId: null, description: "", debit: "", credit: "" })}>
                  <Plus className="h-4 w-4" /> Baris
                </Button>
              </td>
              <td className="num pt-2">{formatAmount(d)}</td>
              <td className="num pt-2">{formatAmount(c)}</td>
              <td />
            </tr>
          </tfoot>
        </table>
        {d !== c && <p className="text-sm text-danger">Total debit dan kredit harus sama.</p>}
      </form>
    </Dialog>
  );
}

function RecurringPage() {
  const { can } = useAuth();
  const qc = useQueryClient();
  const { data, isLoading } = useQuery({ queryKey: ["recurring"], queryFn: () => api.get<Recurring[]>("/recurring-journals") });
  const [editing, setEditing] = useState<Recurring | "new" | null>(null);
  const [deleting, setDeleting] = useState<Recurring | null>(null);

  const generate = useMutation({
    mutationFn: () => api.post<{ generated: number; journalNos: string[]; errors: string[] }>("/recurring-journals/generate"),
    onSuccess: (r) => {
      if (r.generated) toast.success(`${r.generated} jurnal dibuat: ${r.journalNos.join(", ")}`);
      else toast.info("Tidak ada template yang jatuh tempo");
      r.errors.forEach((e) => toast.error(e));
      qc.invalidateQueries({ queryKey: ["recurring"] });
      qc.invalidateQueries({ queryKey: ["journals"] });
    },
    onError: (e) => toast.error(errorMessage(e)),
  });
  const remove = useMutation({
    mutationFn: (id: number) => api.delete(`/recurring-journals/${id}`),
    onSuccess: () => {
      setDeleting(null);
      qc.invalidateQueries({ queryKey: ["recurring"] });
    },
    onError: (e) => toast.error(errorMessage(e)),
  });

  return (
    <>
      <PageHeader
        title="Jurnal Berulang"
        description="Template jurnal bulanan (sewa, amortisasi, dll). Dijalankan otomatis setiap hari pukul 01:05."
        actions={
          <>
            {can("LEDGER:POST") && (
              <Button variant="outline" onClick={() => generate.mutate()} loading={generate.isPending}>
                <Play className="h-4 w-4" /> Jalankan sekarang
              </Button>
            )}
            {can("LEDGER:CREATE") && (
              <Button onClick={() => setEditing("new")}>
                <Plus className="h-4 w-4" /> Template baru
              </Button>
            )}
          </>
        }
      />
      {isLoading ? (
        <Skeleton className="h-64 w-full" />
      ) : !data?.length ? (
        <Card>
          <EmptyState title="Belum ada template" description="Buat template untuk jurnal yang terjadi setiap bulan." />
        </Card>
      ) : (
        <div className="grid gap-4 md:grid-cols-2 xl:grid-cols-3">
          {data.map((r) => (
            <Card key={r.id} className="p-5">
              <div className="flex items-start justify-between gap-2">
                <div>
                  <h3 className="font-semibold">{r.name}</h3>
                  <p className="text-sm text-muted">{r.description}</p>
                </div>
                <Badge tone={r.active ? "success" : "neutral"}>{r.active ? "Aktif" : "Nonaktif"}</Badge>
              </div>
              <p className="mt-4 text-2xl font-semibold tabular-nums">{formatAmount(r.amount)}</p>
              <p className="mt-1 flex items-center gap-1.5 text-sm text-muted">
                <CalendarClock className="h-4 w-4" /> Tiap tgl {r.dayOfMonth} · berikutnya {formatDate(r.nextRunDate)}
                {r.autoPost && <Badge tone="info">auto-post</Badge>}
              </p>
              <ul className="mt-3 space-y-0.5 text-xs text-muted">
                {r.lines.map((l) => (
                  <li key={l.id} className={Number(l.credit) ? "pl-4" : ""}>
                    {l.accountCode} {l.accountName}
                  </li>
                ))}
              </ul>
              <div className="mt-4 flex justify-end gap-1">
                {can("LEDGER:EDIT") && (
                  <Button size="sm" variant="ghost" onClick={() => setEditing(r)}>
                    <Pencil className="h-4 w-4" /> Ubah
                  </Button>
                )}
                {can("LEDGER:DELETE") && (
                  <Button size="sm" variant="ghost" onClick={() => setDeleting(r)}>
                    <Trash2 className="h-4 w-4 text-danger" />
                  </Button>
                )}
              </div>
            </Card>
          ))}
        </div>
      )}
      {editing && <RecurringForm editing={editing === "new" ? null : editing} onClose={() => setEditing(null)} />}
      <ConfirmDialog
        open={deleting !== null}
        title="Hapus template?"
        message="Jurnal yang sudah dibuat dari template ini tidak terpengaruh."
        confirmLabel="Hapus"
        tone="danger"
        loading={remove.isPending}
        onConfirm={() => deleting && remove.mutate(deleting.id)}
        onClose={() => setDeleting(null)}
      />
    </>
  );
}

export default function Page() {
  return (
    <RequirePermission permission="LEDGER:VIEW">
      <RecurringPage />
    </RequirePermission>
  );
}
