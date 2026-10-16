"use client";

import { useRouter } from "next/navigation";
import { Controller, useFieldArray, useForm, useWatch } from "react-hook-form";
import { Plus, Trash2 } from "lucide-react";
import { toast } from "sonner";
import { useQueryClient } from "@tanstack/react-query";
import { PageHeader } from "@/components/shared/page-header";
import { ItemPicker, MasterSelect } from "@/components/shared/pickers";
import { Button } from "@/components/ui/button";
import { Card, CardContent } from "@/components/ui/card";
import { FormField } from "@/components/ui/form-field";
import { Input } from "@/components/ui/input";
import { MoneyInput } from "@/components/ui/money-input";
import { Select } from "@/components/ui/select";
import { RequirePermission } from "@/features/auth/require-permission";
import { useAuth } from "@/features/auth/auth-context";
import type { Adjustment } from "@/features/inventory/types";
import { api, errorMessage } from "@/lib/api";
import { todayIso } from "@/lib/format";

interface Form {
  docDate: string;
  warehouseId: string;
  adjustmentType: "ADJUSTMENT" | "OPENING";
  reason: string;
  lines: { itemId: number | null; qtyChange: string; unitCost: string; note: string }[];
}

const emptyLine = { itemId: null, qtyChange: "", unitCost: "", note: "" };

function NewAdjustment() {
  const router = useRouter();
  const qc = useQueryClient();
  const { can } = useAuth();
  const { register, control, handleSubmit, formState } = useForm<Form>({
    defaultValues: { docDate: todayIso(), warehouseId: "", adjustmentType: "ADJUSTMENT", reason: "", lines: [{ ...emptyLine }] },
  });
  const { fields, append, remove } = useFieldArray({ control, name: "lines" });
  const type = useWatch({ control, name: "adjustmentType" });

  const onSubmit = handleSubmit(async (v) => {
    if (!v.warehouseId) return toast.error("Pilih gudang");
    const lines = v.lines.filter((l) => l.itemId && Number(l.qtyChange) !== 0);
    if (!lines.length) return toast.error("Isi minimal satu baris dengan qty");
    try {
      const res = await api.post<Adjustment>("/inventory/adjustments", {
        ...v,
        warehouseId: Number(v.warehouseId),
        lines: lines.map((l) => ({ itemId: l.itemId, qtyChange: l.qtyChange, unitCost: l.unitCost || null, note: l.note || null })),
      });
      toast.success(`${res.docNo} diposting`);
      qc.invalidateQueries({ queryKey: ["/inventory/adjustments"] });
      qc.invalidateQueries({ queryKey: ["inventory"] });
      router.push(`/persediaan/penyesuaian/${res.id}`);
    } catch (e) {
      toast.error(errorMessage(e));
    }
  });

  return (
    <form onSubmit={onSubmit} className="space-y-6" noValidate>
      <PageHeader title="Penyesuaian Stok" description="Qty positif menambah stok, negatif mengurangi. Langsung diposting beserta jurnalnya." />
      <Card>
        <CardContent className="grid gap-4 pt-5 md:grid-cols-4">
          <FormField label="Tanggal" htmlFor="docDate" required>
            <Input id="docDate" type="date" {...register("docDate")} />
          </FormField>
          <FormField label="Gudang" htmlFor="warehouseId" required>
            <MasterSelect id="warehouseId" resource="/warehouses" {...register("warehouseId")} />
          </FormField>
          <FormField label="Jenis" htmlFor="adjustmentType" hint={type === "OPENING" ? "Lawan akun: Ekuitas Saldo Awal" : "Lawan akun: Selisih Penyesuaian Persediaan"}>
            <Select id="adjustmentType" {...register("adjustmentType")}>
              <option value="ADJUSTMENT">Penyesuaian</option>
              {can("INVENTORY:POST") && <option value="OPENING">Saldo awal persediaan</option>}
            </Select>
          </FormField>
          <FormField label="Alasan" htmlFor="reason" required>
            <Input id="reason" {...register("reason", { required: true })} />
          </FormField>
        </CardContent>
      </Card>
      <Card>
        <div className="overflow-x-auto">
          <table className="w-full min-w-[820px] text-sm">
            <thead>
              <tr className="border-b border-border bg-surface-muted/60 text-left text-xs uppercase text-muted">
                <th className="px-3 py-2.5">Item</th>
                <th className="w-40 px-3 py-2.5 text-right">Qty (+/−)</th>
                <th className="w-44 px-3 py-2.5 text-right">Biaya/unit (penambahan)</th>
                <th className="px-3 py-2.5">Catatan</th>
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
                    <Input className="num" inputMode="decimal" aria-label={`Qty ${i + 1}`} placeholder="mis. 10 atau -2" {...register(`lines.${i}.qtyChange`)} />
                  </td>
                  <td className="px-3 py-2">
                    <Controller control={control} name={`lines.${i}.unitCost`} render={({ field }) => <MoneyInput value={field.value} onValueChange={field.onChange} placeholder="otomatis" aria-label={`Biaya ${i + 1}`} />} />
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
        </div>
        <div className="p-3">
          <Button size="sm" variant="outline" onClick={() => append({ ...emptyLine })}>
            <Plus className="h-4 w-4" /> Tambah baris
          </Button>
        </div>
      </Card>
      <p className="text-xs text-muted">Biaya/unit kosong = memakai biaya rata-rata saat ini (atau biaya standar item bila stok kosong).</p>
      <div className="flex justify-end gap-2">
        <Button variant="outline" onClick={() => router.back()}>
          Batal
        </Button>
        <Button type="submit" loading={formState.isSubmitting}>
          Posting penyesuaian
        </Button>
      </div>
    </form>
  );
}

export default function Page() {
  return (
    <RequirePermission permission="INVENTORY:CREATE">
      <NewAdjustment />
    </RequirePermission>
  );
}
