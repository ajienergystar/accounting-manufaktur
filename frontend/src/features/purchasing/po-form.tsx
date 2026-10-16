"use client";

import { useRouter } from "next/navigation";
import { Controller, useFieldArray, useForm, useWatch } from "react-hook-form";
import { Plus, Trash2 } from "lucide-react";
import { toast } from "sonner";
import { useQueryClient } from "@tanstack/react-query";
import { TotalsPanel } from "@/components/shared/info";
import { ItemPicker, MasterSelect, SupplierPicker, useItemOptions, useTaxRates } from "@/components/shared/pickers";
import { Button } from "@/components/ui/button";
import { Card, CardContent } from "@/components/ui/card";
import { FormField } from "@/components/ui/form-field";
import { Input } from "@/components/ui/input";
import { MoneyInput } from "@/components/ui/money-input";
import { Select } from "@/components/ui/select";
import { api, errorMessage } from "@/lib/api";
import { formatAmount, todayIso } from "@/lib/format";
import type { PurchaseOrder, PurchaseRequest } from "./types";

interface Form {
  docDate: string;
  supplierId: number | null;
  warehouseId: string;
  expectedDate: string;
  taxRateId: string;
  discountAmount: string;
  notes: string;
  lines: { itemId: number | null; description: string; qty: string; unitPrice: string; discountPct: string }[];
}

const emptyLine = { itemId: null, description: "", qty: "", unitPrice: "", discountPct: "" };
const round2 = (n: number) => Math.round(n * 100) / 100;

export function PoForm({ po, fromRequest }: { po?: PurchaseOrder; fromRequest?: PurchaseRequest }) {
  const router = useRouter();
  const qc = useQueryClient();
  const { data: taxes } = useTaxRates();
  const { data: items } = useItemOptions(["RAW_MATERIAL", "SUPPORTING_MATERIAL", "SUPPLIES"]);
  const { register, control, handleSubmit, setValue, formState } = useForm<Form>({
    defaultValues: po
      ? {
          docDate: po.docDate,
          supplierId: po.supplier.id,
          warehouseId: String(po.warehouse.id),
          expectedDate: po.expectedDate ?? "",
          taxRateId: po.taxRateId ? String(po.taxRateId) : "",
          discountAmount: po.discountAmount,
          notes: po.notes ?? "",
          lines: po.lines.map((l) => ({ itemId: l.itemId, description: l.description ?? "", qty: l.qty, unitPrice: l.unitPrice, discountPct: l.discountPct })),
        }
      : {
          docDate: todayIso(),
          supplierId: null,
          warehouseId: "",
          expectedDate: fromRequest?.neededDate ?? "",
          taxRateId: "",
          discountAmount: "",
          notes: fromRequest ? `Dari ${fromRequest.docNo}` : "",
          lines: fromRequest
            ? fromRequest.lines.map((l) => ({ itemId: l.itemId, description: l.note ?? "", qty: l.qty, unitPrice: "", discountPct: "" }))
            : [{ ...emptyLine }],
        },
  });
  const { fields, append, remove } = useFieldArray({ control, name: "lines" });
  const [lines, taxRateId, discountAmount] = useWatch({ control, name: ["lines", "taxRateId", "discountAmount"] });

  const lineTotals = lines.map((l) => round2(Number(l.qty || 0) * Number(l.unitPrice || 0) * (1 - Number(l.discountPct || 0) / 100)));
  const subtotal = round2(lineTotals.reduce((a, b) => a + b, 0));
  const discount = Number(discountAmount || 0);
  const rate = Number(taxes?.find((t) => String(t.id) === taxRateId)?.rate ?? 0);
  const tax = round2(((subtotal - discount) * rate) / 100);
  const total = round2(subtotal - discount + tax);

  const onSubmit = handleSubmit(async (v) => {
    if (!v.supplierId) return toast.error("Pilih pemasok");
    if (!v.warehouseId) return toast.error("Pilih gudang tujuan");
    const ls = v.lines.filter((l) => l.itemId);
    if (!ls.length) return toast.error("Isi minimal satu item");
    const body = {
      ...v,
      requestId: po ? po.requestId : (fromRequest?.id ?? null),
      warehouseId: Number(v.warehouseId),
      taxRateId: v.taxRateId ? Number(v.taxRateId) : null,
      expectedDate: v.expectedDate || null,
      discountAmount: v.discountAmount || "0",
      lines: ls.map((l) => ({ ...l, discountPct: l.discountPct || "0" })),
    };
    try {
      const res = po
        ? await api.put<PurchaseOrder>(`/purchasing/orders/${po.id}`, body)
        : await api.post<PurchaseOrder>("/purchasing/orders", body);
      toast.success(`PO ${res.docNo} disimpan`);
      qc.invalidateQueries({ queryKey: ["/purchasing/orders"] });
      router.push(`/pembelian/po/${res.id}`);
    } catch (e) {
      toast.error(errorMessage(e));
    }
  });

  return (
    <form onSubmit={onSubmit} className="space-y-6" noValidate>
      <Card>
        <CardContent className="grid gap-4 pt-5 md:grid-cols-3">
          <FormField label="Pemasok" htmlFor="supplierId" required className="md:col-span-2">
            <Controller control={control} name="supplierId" render={({ field }) => <SupplierPicker id="supplierId" value={field.value} onChange={field.onChange} />} />
          </FormField>
          <FormField label="Tanggal PO" htmlFor="docDate" required>
            <Input id="docDate" type="date" {...register("docDate")} />
          </FormField>
          <FormField label="Gudang tujuan" htmlFor="warehouseId" required>
            <MasterSelect id="warehouseId" resource="/warehouses" {...register("warehouseId")} />
          </FormField>
          <FormField label="Perkiraan tiba" htmlFor="expectedDate">
            <Input id="expectedDate" type="date" {...register("expectedDate")} />
          </FormField>
          <FormField label="PPN" htmlFor="taxRateId">
            <Select id="taxRateId" {...register("taxRateId")}>
              <option value="">Tanpa PPN</option>
              {taxes?.filter((t) => t.taxType === "PPN").map((t) => (
                <option key={t.id} value={t.id}>
                  {t.name}
                </option>
              ))}
            </Select>
          </FormField>
          <FormField label="Catatan" htmlFor="notes" className="md:col-span-3">
            <Input id="notes" {...register("notes")} />
          </FormField>
        </CardContent>
      </Card>
      <Card>
        <div className="overflow-x-auto">
          <table className="w-full min-w-[960px] text-sm">
            <thead>
              <tr className="border-b border-border bg-surface-muted/60 text-left text-xs uppercase text-muted">
                <th className="px-3 py-2.5">Item</th>
                <th className="px-3 py-2.5">Keterangan</th>
                <th className="w-28 px-3 py-2.5 text-right">Qty</th>
                <th className="w-40 px-3 py-2.5 text-right">Harga</th>
                <th className="w-24 px-3 py-2.5 text-right">Disk %</th>
                <th className="w-40 px-3 py-2.5 text-right">Jumlah</th>
                <th className="w-10" />
              </tr>
            </thead>
            <tbody>
              {fields.map((f, i) => (
                <tr key={f.id} className="border-b border-border">
                  <td className="px-3 py-2">
                    <Controller
                      control={control}
                      name={`lines.${i}.itemId`}
                      render={({ field }) => (
                        <ItemPicker
                          types={["RAW_MATERIAL", "SUPPORTING_MATERIAL", "SUPPLIES"]}
                          value={field.value}
                          onChange={(id) => {
                            field.onChange(id);
                            const it = items?.find((x) => x.id === id);
                            if (it && !lines[i]?.unitPrice) setValue(`lines.${i}.unitPrice`, it.standardCost);
                          }}
                          aria-label={`Item ${i + 1}`}
                        />
                      )}
                    />
                  </td>
                  <td className="px-3 py-2">
                    <Input aria-label={`Keterangan ${i + 1}`} {...register(`lines.${i}.description`)} />
                  </td>
                  <td className="px-3 py-2">
                    <Input className="num" inputMode="decimal" aria-label={`Qty ${i + 1}`} {...register(`lines.${i}.qty`)} />
                  </td>
                  <td className="px-3 py-2">
                    <Controller control={control} name={`lines.${i}.unitPrice`} render={({ field }) => <MoneyInput value={field.value} onValueChange={field.onChange} aria-label={`Harga ${i + 1}`} />} />
                  </td>
                  <td className="px-3 py-2">
                    <Input className="num" inputMode="decimal" aria-label={`Diskon ${i + 1}`} {...register(`lines.${i}.discountPct`)} />
                  </td>
                  <td className="num px-3 py-2">{formatAmount(lineTotals[i] ?? 0)}</td>
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
        <div className="flex flex-col gap-4 p-4 md:flex-row md:items-start">
          <Button size="sm" variant="outline" onClick={() => append({ ...emptyLine })}>
            <Plus className="h-4 w-4" /> Tambah baris
          </Button>
          <div className="ml-auto w-full max-w-sm space-y-3">
            <FormField label="Diskon (Rp)" htmlFor="discountAmount">
              <Controller control={control} name="discountAmount" render={({ field }) => <MoneyInput id="discountAmount" value={field.value} onValueChange={field.onChange} />} />
            </FormField>
            <TotalsPanel
              rows={[
                { label: "Subtotal", value: formatAmount(subtotal) },
                { label: "Diskon", value: `(${formatAmount(discount)})`, muted: true },
                { label: `PPN ${rate ? `${rate}%` : ""}`, value: formatAmount(tax) },
                { label: "Total", value: formatAmount(total), strong: true },
              ]}
            />
          </div>
        </div>
      </Card>
      <div className="flex justify-end gap-2">
        <Button variant="outline" onClick={() => router.back()}>
          Batal
        </Button>
        <Button type="submit" loading={formState.isSubmitting}>
          Simpan PO (draft)
        </Button>
      </div>
    </form>
  );
}
