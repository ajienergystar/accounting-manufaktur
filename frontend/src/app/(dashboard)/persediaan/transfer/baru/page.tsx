"use client";

import { useRouter } from "next/navigation";
import { Controller, useFieldArray, useForm } from "react-hook-form";
import { Plus, Trash2 } from "lucide-react";
import { toast } from "sonner";
import { useQueryClient } from "@tanstack/react-query";
import { PageHeader } from "@/components/shared/page-header";
import { ItemPicker, MasterSelect } from "@/components/shared/pickers";
import { Button } from "@/components/ui/button";
import { Card, CardContent } from "@/components/ui/card";
import { FormField } from "@/components/ui/form-field";
import { Input } from "@/components/ui/input";
import { RequirePermission } from "@/features/auth/require-permission";
import type { Transfer } from "@/features/inventory/types";
import { api, errorMessage } from "@/lib/api";
import { todayIso } from "@/lib/format";

interface Form {
  docDate: string;
  fromWarehouseId: string;
  toWarehouseId: string;
  notes: string;
  lines: { itemId: number | null; qty: string }[];
}

function NewTransfer() {
  const router = useRouter();
  const qc = useQueryClient();
  const { register, control, handleSubmit, formState } = useForm<Form>({
    defaultValues: { docDate: todayIso(), fromWarehouseId: "", toWarehouseId: "", notes: "", lines: [{ itemId: null, qty: "" }] },
  });
  const { fields, append, remove } = useFieldArray({ control, name: "lines" });

  const onSubmit = handleSubmit(async (v) => {
    if (!v.fromWarehouseId || !v.toWarehouseId) return toast.error("Pilih gudang asal dan tujuan");
    if (v.fromWarehouseId === v.toWarehouseId) return toast.error("Gudang asal dan tujuan tidak boleh sama");
    const lines = v.lines.filter((l) => l.itemId && Number(l.qty) > 0);
    if (!lines.length) return toast.error("Isi minimal satu item");
    try {
      const res = await api.post<Transfer>("/inventory/transfers", {
        ...v,
        fromWarehouseId: Number(v.fromWarehouseId),
        toWarehouseId: Number(v.toWarehouseId),
        lines,
      });
      toast.success(`${res.docNo} diposting`);
      qc.invalidateQueries({ queryKey: ["/inventory/transfers"] });
      qc.invalidateQueries({ queryKey: ["inventory"] });
      router.push(`/persediaan/transfer/${res.id}`);
    } catch (e) {
      toast.error(errorMessage(e));
    }
  });

  return (
    <form onSubmit={onSubmit} className="space-y-6" noValidate>
      <PageHeader title="Transfer Antar Gudang" description="Barang dipindahkan pada nilai persediaan yang sama (tanpa jurnal)." />
      <Card>
        <CardContent className="grid gap-4 pt-5 md:grid-cols-4">
          <FormField label="Tanggal" htmlFor="docDate" required>
            <Input id="docDate" type="date" {...register("docDate")} />
          </FormField>
          <FormField label="Dari gudang" htmlFor="from" required>
            <MasterSelect id="from" resource="/warehouses" {...register("fromWarehouseId")} />
          </FormField>
          <FormField label="Ke gudang" htmlFor="to" required>
            <MasterSelect id="to" resource="/warehouses" {...register("toWarehouseId")} />
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
              <th className="w-48 px-3 py-2.5 text-right">Qty</th>
              <th className="w-10" />
            </tr>
          </thead>
          <tbody>
            {fields.map((f, i) => (
              <tr key={f.id} className="border-b border-border">
                <td className="px-3 py-2">
                  <Controller control={control} name={`lines.${i}.itemId`} render={({ field }) => <ItemPicker value={field.value} onChange={field.onChange} aria-label={`Item ${i + 1}`} />} />
                </td>
                <td className="px-3 py-2">
                  <Input className="num" inputMode="decimal" aria-label={`Qty ${i + 1}`} {...register(`lines.${i}.qty`)} />
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
          <Button size="sm" variant="outline" onClick={() => append({ itemId: null, qty: "" })}>
            <Plus className="h-4 w-4" /> Tambah baris
          </Button>
        </div>
      </Card>
      <div className="flex justify-end gap-2">
        <Button variant="outline" onClick={() => router.back()}>
          Batal
        </Button>
        <Button type="submit" loading={formState.isSubmitting}>
          Posting transfer
        </Button>
      </div>
    </form>
  );
}

export default function Page() {
  return (
    <RequirePermission permission="INVENTORY:CREATE">
      <NewTransfer />
    </RequirePermission>
  );
}
