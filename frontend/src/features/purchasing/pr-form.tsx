"use client";

import { useRouter } from "next/navigation";
import { Controller, useFieldArray, useForm } from "react-hook-form";
import { Plus, Trash2 } from "lucide-react";
import { toast } from "sonner";
import { useQueryClient } from "@tanstack/react-query";
import { ItemPicker, MasterSelect } from "@/components/shared/pickers";
import { Button } from "@/components/ui/button";
import { Card, CardContent } from "@/components/ui/card";
import { FormField } from "@/components/ui/form-field";
import { Input } from "@/components/ui/input";
import { api, errorMessage } from "@/lib/api";
import { todayIso } from "@/lib/format";
import type { PurchaseRequest } from "./types";

interface Form {
  docDate: string;
  departmentId: string;
  neededDate: string;
  notes: string;
  lines: { itemId: number | null; qty: string; note: string }[];
}

export function PrForm({ pr }: { pr?: PurchaseRequest }) {
  const router = useRouter();
  const qc = useQueryClient();
  const { register, control, handleSubmit, formState } = useForm<Form>({
    defaultValues: pr
      ? {
          docDate: pr.docDate,
          departmentId: pr.department ? String(pr.department.id) : "",
          neededDate: pr.neededDate ?? "",
          notes: pr.notes ?? "",
          lines: pr.lines.map((l) => ({ itemId: l.itemId, qty: l.qty, note: l.note ?? "" })),
        }
      : { docDate: todayIso(), departmentId: "", neededDate: "", notes: "", lines: [{ itemId: null, qty: "", note: "" }] },
  });
  const { fields, append, remove } = useFieldArray({ control, name: "lines" });

  const onSubmit = handleSubmit(async (v) => {
    const lines = v.lines.filter((l) => l.itemId && Number(l.qty) > 0);
    if (!lines.length) return toast.error("Isi minimal satu item");
    const body = { ...v, departmentId: v.departmentId ? Number(v.departmentId) : null, neededDate: v.neededDate || null, lines };
    try {
      const res = pr
        ? await api.put<PurchaseRequest>(`/purchasing/requests/${pr.id}`, body)
        : await api.post<PurchaseRequest>("/purchasing/requests", body);
      toast.success(`PR ${res.docNo} disimpan`);
      qc.invalidateQueries({ queryKey: ["/purchasing/requests"] });
      router.push(`/pembelian/pr/${res.id}`);
    } catch (e) {
      toast.error(errorMessage(e));
    }
  });

  return (
    <form onSubmit={onSubmit} className="space-y-6" noValidate>
      <Card>
        <CardContent className="grid gap-4 pt-5 md:grid-cols-4">
          <FormField label="Tanggal" htmlFor="docDate">
            <Input id="docDate" type="date" {...register("docDate")} />
          </FormField>
          <FormField label="Departemen" htmlFor="dept">
            <MasterSelect id="dept" resource="/departments" {...register("departmentId")} />
          </FormField>
          <FormField label="Dibutuhkan tanggal" htmlFor="needed">
            <Input id="needed" type="date" {...register("neededDate")} />
          </FormField>
          <FormField label="Catatan" htmlFor="notes">
            <Input id="notes" {...register("notes")} />
          </FormField>
        </CardContent>
      </Card>
      <Card>
        <table className="w-full text-sm">
          <thead>
            <tr className="border-b border-border bg-surface-muted/60 text-left text-xs uppercase text-muted">
              <th className="px-3 py-2.5">Item</th>
              <th className="w-36 px-3 py-2.5 text-right">Qty</th>
              <th className="px-3 py-2.5">Catatan</th>
              <th className="w-10" />
            </tr>
          </thead>
          <tbody>
            {fields.map((f, i) => (
              <tr key={f.id} className="border-b border-border">
                <td className="px-3 py-2">
                  <Controller control={control} name={`lines.${i}.itemId`} render={({ field }) => <ItemPicker types={["RAW_MATERIAL", "SUPPORTING_MATERIAL", "SUPPLIES"]} value={field.value} onChange={field.onChange} aria-label={`Item ${i + 1}`} />} />
                </td>
                <td className="px-3 py-2">
                  <Input className="num" inputMode="decimal" aria-label={`Qty ${i + 1}`} {...register(`lines.${i}.qty`)} />
                </td>
                <td className="px-3 py-2">
                  <Input aria-label={`Catatan ${i + 1}`} {...register(`lines.${i}.note`)} />
                </td>
                <td>
                  <Button size="icon" variant="ghost" onClick={() => remove(i)} disabled={fields.length === 1} aria-label="Hapus baris">
                    <Trash2 className="h-4 w-4 text-danger" />
                  </Button>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
        <div className="p-3">
          <Button size="sm" variant="outline" onClick={() => append({ itemId: null, qty: "", note: "" })}>
            <Plus className="h-4 w-4" /> Tambah baris
          </Button>
        </div>
      </Card>
      <div className="flex justify-end gap-2">
        <Button variant="outline" onClick={() => router.back()}>
          Batal
        </Button>
        <Button type="submit" loading={formState.isSubmitting}>
          Simpan draft
        </Button>
      </div>
    </form>
  );
}
