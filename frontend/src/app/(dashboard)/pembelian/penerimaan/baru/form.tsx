"use client";

import { useRouter, useSearchParams } from "next/navigation";
import { useMemo, useState } from "react";
import { useQuery, useQueryClient } from "@tanstack/react-query";
import { Info as InfoIcon, Plus, Trash2 } from "lucide-react";
import { toast } from "sonner";
import { PageHeader } from "@/components/shared/page-header";
import { ItemPicker, MasterSelect, SupplierPicker } from "@/components/shared/pickers";
import { Button } from "@/components/ui/button";
import { Card, CardContent } from "@/components/ui/card";
import { FormField } from "@/components/ui/form-field";
import { Input } from "@/components/ui/input";
import { MoneyInput } from "@/components/ui/money-input";
import { Select } from "@/components/ui/select";
import { RequirePermission } from "@/features/auth/require-permission";
import type { GoodsReceipt, PurchaseOrder } from "@/features/purchasing/types";
import { api, errorMessage } from "@/lib/api";
import { formatAmount, formatQty, todayIso } from "@/lib/format";

interface DirectLine {
  itemId: number | null;
  qty: string;
  unitPrice: string;
}

function Form() {
  const router = useRouter();
  const params = useSearchParams();
  const qc = useQueryClient();
  const [supplierId, setSupplierId] = useState<number | null>(params.get("supplierId") ? Number(params.get("supplierId")) : null);
  const [orderId, setOrderId] = useState(params.get("orderId") ?? "");
  const [docDate, setDocDate] = useState(todayIso());
  const [warehouseId, setWarehouseId] = useState("");
  const [deliveryNoteNo, setDeliveryNoteNo] = useState("");
  const [freightCost, setFreightCost] = useState("");
  const [notes, setNotes] = useState("");
  const [qtys, setQtys] = useState<Record<number, string>>({});
  const [direct, setDirect] = useState<DirectLine[]>([{ itemId: null, qty: "", unitPrice: "" }]);
  const [saving, setSaving] = useState(false);

  const openPos = useQuery({
    queryKey: ["/purchasing/orders", "open", supplierId],
    queryFn: () => api.get<PurchaseOrder[]>("/purchasing/orders/open", { supplierId }),
    enabled: supplierId !== null,
  });
  const po = useMemo(() => openPos.data?.find((o) => String(o.id) === orderId), [openPos.data, orderId]);
  const effectiveWarehouse = warehouseId || (po ? String(po.warehouse.id) : "");

  const submit = async () => {
    if (!supplierId) return toast.error("Pilih pemasok");
    if (!effectiveWarehouse) return toast.error("Pilih gudang");
    const lines = po
      ? po.lines.filter((l) => Number(qtys[l.id] || 0) > 0).map((l) => ({ orderLineId: l.id, qty: qtys[l.id] }))
      : direct.filter((l) => l.itemId && Number(l.qty) > 0).map((l) => ({ itemId: l.itemId, qty: l.qty, unitPrice: l.unitPrice || "0" }));
    if (!lines.length) return toast.error("Isi qty yang diterima");
    setSaving(true);
    try {
      const res = await api.post<GoodsReceipt>("/purchasing/receipts", {
        docDate,
        supplierId,
        orderId: po ? po.id : null,
        warehouseId: Number(effectiveWarehouse),
        deliveryNoteNo: deliveryNoteNo || null,
        freightCost: freightCost || "0",
        notes: notes || null,
        lines,
      });
      toast.success(`${res.docNo} diposting`);
      qc.invalidateQueries({ queryKey: ["/purchasing/receipts"] });
      qc.invalidateQueries({ queryKey: ["/purchasing/orders"] });
      qc.invalidateQueries({ queryKey: ["inventory"] });
      router.push(`/pembelian/penerimaan/${res.id}`);
    } catch (e) {
      toast.error(errorMessage(e));
    } finally {
      setSaving(false);
    }
  };

  return (
    <div className="space-y-6">
      <PageHeader title="Penerimaan Barang" description="Barang masuk persediaan pada harga PO + alokasi ongkos angkut." />
      <Card>
        <CardContent className="grid gap-4 pt-5 md:grid-cols-3">
          <FormField label="Pemasok" htmlFor="supplier" required className="md:col-span-2">
            <SupplierPicker id="supplier" value={supplierId} onChange={(v) => (setSupplierId(v), setOrderId(""), setQtys({}))} />
          </FormField>
          <FormField label="Tanggal terima" htmlFor="docDate" required>
            <Input id="docDate" type="date" value={docDate} onChange={(e) => setDocDate(e.target.value)} />
          </FormField>
          <FormField label="Purchase order" htmlFor="po" hint="Kosongkan untuk penerimaan tanpa PO">
            <Select id="po" value={orderId} onChange={(e) => (setOrderId(e.target.value), setQtys({}))} disabled={!supplierId}>
              <option value="">— Tanpa PO —</option>
              {openPos.data?.map((o) => (
                <option key={o.id} value={o.id}>
                  {o.docNo} · {o.status}
                </option>
              ))}
            </Select>
          </FormField>
          <FormField label="Gudang" htmlFor="wh" required>
            <MasterSelect id="wh" resource="/warehouses" value={effectiveWarehouse} onChange={(e) => setWarehouseId(e.target.value)} />
          </FormField>
          <FormField label="No. surat jalan" htmlFor="dn">
            <Input id="dn" value={deliveryNoteNo} onChange={(e) => setDeliveryNoteNo(e.target.value)} />
          </FormField>
          <FormField label="Ongkos angkut (Rp)" htmlFor="freight" hint="Dikapitalisasi ke harga perolehan persediaan">
            <MoneyInput id="freight" value={freightCost} onValueChange={setFreightCost} />
          </FormField>
          <FormField label="Catatan" htmlFor="notes" className="md:col-span-2">
            <Input id="notes" value={notes} onChange={(e) => setNotes(e.target.value)} />
          </FormField>
        </CardContent>
      </Card>

      <Card>
        {po ? (
          <table className="w-full text-sm">
            <thead>
              <tr className="border-b border-border bg-surface-muted/60 text-left text-xs uppercase text-muted">
                <th className="px-4 py-2.5">Item</th>
                <th className="px-4 py-2.5 text-right">Dipesan</th>
                <th className="px-4 py-2.5 text-right">Sudah diterima</th>
                <th className="px-4 py-2.5 text-right">Sisa</th>
                <th className="px-4 py-2.5 text-right">Harga bersih</th>
                <th className="w-40 px-4 py-2.5 text-right">Diterima sekarang</th>
              </tr>
            </thead>
            <tbody>
              {po.lines.map((l) => (
                <tr key={l.id} className="border-b border-border">
                  <td className="px-4 py-2">
                    <span className="font-mono text-xs">{l.itemCode}</span> {l.itemName}
                  </td>
                  <td className="num px-4 py-2">
                    {formatQty(l.qty)} {l.uomCode}
                  </td>
                  <td className="num px-4 py-2">{formatQty(l.qtyReceived)}</td>
                  <td className="num px-4 py-2">{formatQty(l.remainingQty)}</td>
                  <td className="num px-4 py-2">{formatAmount(l.netUnitPrice)}</td>
                  <td className="px-4 py-1.5">
                    <div className="flex gap-1">
                      <Input
                        className="num"
                        inputMode="decimal"
                        aria-label={`Qty diterima ${l.itemCode}`}
                        value={qtys[l.id] ?? ""}
                        onChange={(e) => setQtys({ ...qtys, [l.id]: e.target.value.replace(",", ".") })}
                        disabled={Number(l.remainingQty) === 0}
                      />
                      <Button size="sm" variant="ghost" onClick={() => setQtys({ ...qtys, [l.id]: l.remainingQty })} disabled={Number(l.remainingQty) === 0}>
                        Semua
                      </Button>
                    </div>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        ) : (
          <>
            <p className="flex items-center gap-2 border-b border-border px-4 py-3 text-sm text-muted">
              <InfoIcon className="h-4 w-4" /> Penerimaan tanpa PO — isi item, qty, dan harga per unit.
            </p>
            <table className="w-full text-sm">
              <thead>
                <tr className="border-b border-border bg-surface-muted/60 text-left text-xs uppercase text-muted">
                  <th className="px-3 py-2.5">Item</th>
                  <th className="w-36 px-3 py-2.5 text-right">Qty</th>
                  <th className="w-44 px-3 py-2.5 text-right">Harga/unit</th>
                  <th className="w-10" />
                </tr>
              </thead>
              <tbody>
                {direct.map((l, i) => (
                  <tr key={i} className="border-b border-border">
                    <td className="px-3 py-2">
                      <ItemPicker
                        types={["RAW_MATERIAL", "SUPPORTING_MATERIAL", "SUPPLIES"]}
                        value={l.itemId}
                        onChange={(v) => setDirect(direct.map((d, j) => (j === i ? { ...d, itemId: v } : d)))}
                        aria-label={`Item ${i + 1}`}
                      />
                    </td>
                    <td className="px-3 py-2">
                      <Input className="num" inputMode="decimal" aria-label={`Qty ${i + 1}`} value={l.qty} onChange={(e) => setDirect(direct.map((d, j) => (j === i ? { ...d, qty: e.target.value.replace(",", ".") } : d)))} />
                    </td>
                    <td className="px-3 py-2">
                      <MoneyInput aria-label={`Harga ${i + 1}`} value={l.unitPrice} onValueChange={(v) => setDirect(direct.map((d, j) => (j === i ? { ...d, unitPrice: v } : d)))} />
                    </td>
                    <td>
                      <Button size="icon" variant="ghost" onClick={() => setDirect(direct.filter((_, j) => j !== i))} disabled={direct.length === 1} aria-label="Hapus baris">
                        <Trash2 className="h-4 w-4 text-danger" />
                      </Button>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
            <div className="p-3">
              <Button size="sm" variant="outline" onClick={() => setDirect([...direct, { itemId: null, qty: "", unitPrice: "" }])}>
                <Plus className="h-4 w-4" /> Tambah baris
              </Button>
            </div>
          </>
        )}
      </Card>
      <div className="flex justify-end gap-2">
        <Button variant="outline" onClick={() => router.back()}>
          Batal
        </Button>
        <Button onClick={submit} loading={saving}>
          Posting penerimaan
        </Button>
      </div>
    </div>
  );
}

export function GrnForm() {
  return (
    <RequirePermission permission="PURCHASE:VIEW">
      <Form />
    </RequirePermission>
  );
}
