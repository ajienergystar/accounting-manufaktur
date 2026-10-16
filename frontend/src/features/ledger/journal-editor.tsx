"use client";

import { useRouter } from "next/navigation";
import { useState } from "react";
import { Controller, useFieldArray, useForm, useWatch } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { z } from "zod";
import { CheckCircle2, Plus, Trash2, XCircle } from "lucide-react";
import { toast } from "sonner";
import { useQueryClient } from "@tanstack/react-query";
import { AccountPicker } from "@/components/shared/account-picker";
import { Button } from "@/components/ui/button";
import { Card, CardContent } from "@/components/ui/card";
import { FormField } from "@/components/ui/form-field";
import { Input } from "@/components/ui/input";
import { MoneyInput } from "@/components/ui/money-input";
import { Select } from "@/components/ui/select";
import { useAuth } from "@/features/auth/auth-context";
import { useOptions } from "@/features/master/use-options";
import { api, errorMessage } from "@/lib/api";
import { formatAmount, todayIso } from "@/lib/format";
import { cn } from "@/lib/utils";
import { sumDecimal, type JournalDetail } from "./types";

const lineSchema = z
  .object({
    accountId: z.number({ message: "Pilih akun" }).nullable().refine((v) => v !== null, "Pilih akun"),
    departmentId: z.string().optional(),
    description: z.string().max(255).optional(),
    debit: z.string().optional(),
    credit: z.string().optional(),
  })
  .refine((l) => (Number(l.debit || 0) > 0) !== (Number(l.credit || 0) > 0), {
    message: "Isi debit ATAU kredit",
    path: ["debit"],
  });

const schema = z
  .object({
    journalDate: z.string().min(1, "Tanggal wajib diisi"),
    journalType: z.enum(["GENERAL", "ADJUSTMENT"]),
    description: z.string().trim().min(1, "Keterangan wajib diisi").max(500),
    lines: z.array(lineSchema).min(2, "Minimal 2 baris"),
  })
  .refine((v) => sumDecimal(v.lines.map((l) => l.debit)) === sumDecimal(v.lines.map((l) => l.credit)), {
    message: "Total debit dan kredit harus sama",
    path: ["lines"],
  });
type Form = z.output<typeof schema>;
type FormInput = z.input<typeof schema>;

const emptyLine = { accountId: null, departmentId: "", description: "", debit: "", credit: "" };

export function JournalEditor({ journal }: { journal?: JournalDetail }) {
  const router = useRouter();
  const qc = useQueryClient();
  const { can } = useAuth();
  const { data: departments } = useOptions("/departments");
  const [submitting, setSubmitting] = useState<"draft" | "post" | null>(null);

  const {
    register,
    control,
    handleSubmit,
    formState: { errors },
  } = useForm<FormInput, unknown, Form>({
    resolver: zodResolver(schema),
    defaultValues: journal
      ? {
          journalDate: journal.journalDate,
          journalType: journal.journalType === "ADJUSTMENT" ? "ADJUSTMENT" : "GENERAL",
          description: journal.description,
          lines: journal.lines.map((l) => ({
            accountId: l.accountId,
            departmentId: l.departmentId ? String(l.departmentId) : "",
            description: l.description ?? "",
            debit: Number(l.debit) > 0 ? l.debit : "",
            credit: Number(l.credit) > 0 ? l.credit : "",
          })),
        }
      : { journalDate: todayIso(), journalType: "GENERAL", description: "", lines: [{ ...emptyLine }, { ...emptyLine }] },
  });
  const { fields, append, remove } = useFieldArray({ control, name: "lines" });
  const lines = useWatch({ control, name: "lines" });
  const totalDebit = sumDecimal(lines?.map((l) => l.debit) ?? []);
  const totalCredit = sumDecimal(lines?.map((l) => l.credit) ?? []);
  const diff = Math.round((totalDebit - totalCredit) * 100) / 100;
  const balanced = diff === 0 && totalDebit > 0;

  const submit = (post: boolean) =>
    handleSubmit(async (v) => {
      setSubmitting(post ? "post" : "draft");
      const body = {
        ...v,
        post,
        lines: v.lines.map((l) => ({
          accountId: l.accountId,
          departmentId: l.departmentId ? Number(l.departmentId) : null,
          description: l.description || null,
          debit: l.debit || "0",
          credit: l.credit || "0",
        })),
      };
      try {
        const saved = journal
          ? await api.put<JournalDetail>(`/journals/${journal.id}`, body)
          : await api.post<JournalDetail>("/journals", body);
        toast.success(post ? `Jurnal ${saved.journalNo} diposting` : "Draft jurnal disimpan");
        qc.invalidateQueries({ queryKey: ["journals"] });
        qc.invalidateQueries({ queryKey: ["journal", saved.id] });
        router.push(`/jurnal/${saved.id}`);
      } catch (err) {
        toast.error(errorMessage(err));
      } finally {
        setSubmitting(null);
      }
    })();

  const lineErrors = errors.lines as unknown as
    | (Record<string, { message?: string }> & { message?: string; root?: { message?: string } })
    | undefined;

  return (
    <form onSubmit={(e) => e.preventDefault()} noValidate className="space-y-6">
      <Card>
        <CardContent className="grid gap-4 pt-5 md:grid-cols-[180px_220px_1fr]">
          <FormField label="Tanggal" htmlFor="journalDate" required error={errors.journalDate?.message}>
            <Input id="journalDate" type="date" {...register("journalDate")} />
          </FormField>
          <FormField label="Tipe jurnal" htmlFor="journalType">
            <Select id="journalType" {...register("journalType")}>
              <option value="GENERAL">Jurnal Umum</option>
              <option value="ADJUSTMENT">Jurnal Penyesuaian</option>
            </Select>
          </FormField>
          <FormField label="Keterangan" htmlFor="description" required error={errors.description?.message}>
            <Input id="description" placeholder="Contoh: Pembayaran sewa gudang Oktober" {...register("description")} />
          </FormField>
        </CardContent>
      </Card>

      <Card>
        <div className="overflow-x-auto">
          <table className="w-full min-w-[900px] text-sm">
            <thead>
              <tr className="border-b border-border bg-surface-muted/60 text-left text-xs uppercase tracking-wide text-muted">
                <th className="w-10 px-3 py-2.5">#</th>
                <th className="px-3 py-2.5">Akun</th>
                <th className="w-44 px-3 py-2.5">Departemen</th>
                <th className="px-3 py-2.5">Keterangan baris</th>
                <th className="w-40 px-3 py-2.5 text-right">Debit</th>
                <th className="w-40 px-3 py-2.5 text-right">Kredit</th>
                <th className="w-10 px-3 py-2.5" />
              </tr>
            </thead>
            <tbody>
              {fields.map((f, i) => {
                const le = lineErrors?.[i] as unknown as
                  | { accountId?: { message?: string }; debit?: { message?: string } }
                  | undefined;
                return (
                  <tr key={f.id} className="border-b border-border align-top">
                    <td className="px-3 py-2 pt-4 text-muted">{i + 1}</td>
                    <td className="px-3 py-2">
                      <Controller
                        control={control}
                        name={`lines.${i}.accountId`}
                        render={({ field }) => (
                          <AccountPicker
                            value={field.value}
                            onChange={field.onChange}
                            invalid={!!le?.accountId}
                            aria-label={`Akun baris ${i + 1}`}
                          />
                        )}
                      />
                      {le?.accountId?.message && <p className="mt-1 text-xs text-danger">{le.accountId.message}</p>}
                    </td>
                    <td className="px-3 py-2">
                      <Select aria-label={`Departemen baris ${i + 1}`} {...register(`lines.${i}.departmentId`)}>
                        <option value="">—</option>
                        {departments?.map((d) => (
                          <option key={d.id} value={d.id}>
                            {d.code} {d.name}
                          </option>
                        ))}
                      </Select>
                    </td>
                    <td className="px-3 py-2">
                      <Input aria-label={`Keterangan baris ${i + 1}`} {...register(`lines.${i}.description`)} />
                    </td>
                    <td className="px-3 py-2">
                      <Controller
                        control={control}
                        name={`lines.${i}.debit`}
                        render={({ field }) => (
                          <MoneyInput
                            aria-label={`Debit baris ${i + 1}`}
                            value={field.value}
                            onValueChange={field.onChange}
                            invalid={!!le?.debit}
                          />
                        )}
                      />
                      {le?.debit?.message && <p className="mt-1 text-xs text-danger">{le.debit.message}</p>}
                    </td>
                    <td className="px-3 py-2">
                      <Controller
                        control={control}
                        name={`lines.${i}.credit`}
                        render={({ field }) => (
                          <MoneyInput aria-label={`Kredit baris ${i + 1}`} value={field.value} onValueChange={field.onChange} />
                        )}
                      />
                    </td>
                    <td className="px-3 py-2 pt-3">
                      <Button
                        size="icon"
                        variant="ghost"
                        onClick={() => remove(i)}
                        disabled={fields.length <= 2}
                        aria-label={`Hapus baris ${i + 1}`}
                      >
                        <Trash2 className="h-4 w-4 text-danger" />
                      </Button>
                    </td>
                  </tr>
                );
              })}
            </tbody>
            <tfoot>
              <tr className="bg-surface-muted/40 font-semibold">
                <td colSpan={4} className="px-3 py-3">
                  <Button size="sm" variant="outline" onClick={() => append({ ...emptyLine })}>
                    <Plus className="h-4 w-4" /> Tambah baris
                  </Button>
                </td>
                <td className="num px-3 py-3">{formatAmount(totalDebit)}</td>
                <td className="num px-3 py-3">{formatAmount(totalCredit)}</td>
                <td />
              </tr>
            </tfoot>
          </table>
        </div>
        <div
          role="status"
          className={cn(
            "flex items-center gap-2 border-t border-border px-4 py-3 text-sm font-medium",
            balanced ? "text-success" : "text-danger",
          )}
        >
          {balanced ? <CheckCircle2 className="h-4 w-4" /> : <XCircle className="h-4 w-4" />}
          {balanced
            ? "Balance — total debit sama dengan total kredit"
            : totalDebit === 0 && totalCredit === 0
              ? "Isi nilai debit dan kredit"
              : `Tidak balance — selisih ${formatAmount(Math.abs(diff))} di sisi ${diff > 0 ? "debit" : "kredit"}`}
        </div>
        {(lineErrors?.message || lineErrors?.root?.message) && (
          <p className="px-4 pb-3 text-sm text-danger">{lineErrors?.message ?? lineErrors?.root?.message}</p>
        )}
      </Card>

      <div className="flex flex-wrap justify-end gap-2">
        <Button variant="outline" onClick={() => router.back()}>
          Batal
        </Button>
        <Button variant="secondary" onClick={() => submit(false)} loading={submitting === "draft"} disabled={submitting !== null}>
          Simpan draft
        </Button>
        {can("LEDGER:POST") && (
          <Button onClick={() => submit(true)} loading={submitting === "post"} disabled={!balanced || submitting !== null}>
            Simpan & posting
          </Button>
        )}
      </div>
    </form>
  );
}
