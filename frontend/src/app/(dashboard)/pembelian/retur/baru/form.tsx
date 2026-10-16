"use client";

import { useRouter, useSearchParams } from "next/navigation";
import { useState } from "react";
import { useQuery, useQueryClient } from "@tanstack/react-query";
import { toast } from "sonner";
import { PageHeader } from "@/components/shared/page-header";
import { Button } from "@/components/ui/button";
import { Card, CardContent } from "@/components/ui/card";
import { FormField } from "@/components/ui/form-field";
import { Input } from "@/components/ui/input";
import { Select } from "@/components/ui/select";
import { RequirePermission } from "@/features/auth/require-permission";
import type { GoodsReceipt, PurchaseInvoice, PurchaseReturn } from "@/features/purchasing/types";
import { api, errorMessage } from "@/lib/api";
import { formatAmount, formatQty, todayIso } from "@/lib/format";
import type { PageResponse } from "@/types/common";

function Form() {
  const router = useRouter();
  const params = useSearchParams();
  const qc = useQueryClient();
  const [receiptNo, setReceiptNo] = useState("");
  const [receiptId, setReceiptId] = useState<string>(params.get("receiptId") ?? "");
  const [invoiceId, setInvoiceId] = useState("");
  const [docDate, setDocDate] = useState(todayIso());
  const [reason, setReason] = useState("");
  const [qtys, setQtys] = useState<Record<number, string>>({});
  const [saving, setSaving] = useState(false);

  const search = useQuery({
    queryKey: ["/purchasing/receipts", "search", receiptNo],
    queryFn: () => api.get<PageResponse<GoodsReceipt>>("/purchasing/receipts", { q: receiptNo, status: "POSTED", size: 10 }),
    enabled: !receiptId && receiptNo.length >= 2,
  });
  const grn = useQuery({
    queryKey: ["/purchasing/receipts", receiptId],
    queryFn: () => api.get<GoodsReceipt>(`/purchasing/receipts/${receiptId}`),
    enabled: !!receiptId,
  });
  const invoices = useQuery({
    queryKey: ["/purchasing/invoices", "outstanding", grn.data?.supplier.id],
    queryFn: () => api.get<PurchaseInvoice[]>("/purchasing/invoices/outstanding", { supplierId: grn.data?.supplier.id }),
    enabled: !!grn.data,
  });

  const submit = async () => {
    if (!grn.data) return toast.error("Pilih penerimaan");
    if (!reason.trim()) return toast.error("Isi alasan retur");
    const lines = grn.data.lines.filter((l) => Number(qtys[l.id] || 0) > 0).map((l) => ({ receiptLineId: l.id, qty: qtys[l.id] }));
    if (!lines.length) return toast.error("Isi qty retur");
    setSaving(true);
    try {
      const res = await api.post<PurchaseReturn>("/purchasing/returns", {
        docDate,
        receiptId: grn.data.id,
        invoiceId: invoiceId ? Number(invoiceId) : null,
        reason,
        lines,
      });
      toast.success(`Retur ${res.docNo} diposting`);
      qc.invalidateQueries({ queryKey: ["/purchasing/returns"] });
      qc.invalidateQueries({ queryKey: ["/purchasing/receipts"] });
      qc.invalidateQueries({ queryKey: ["/purchasing/invoices"] });
      qc.invalidateQueries({ queryKey: ["inventory"] });
      router.push(`/pembelian/retur/${res.id}`);
    } catch (e) {
      toast.error(errorMessage(e));
    } finally {
      setSaving(false);
    }
  };

  return (
    <div className="space-y-6">
      <PageHeader title="Retur Pembelian" description="Pilih penerimaan barang. Bila barang sudah difakturkan, pilih fakturnya agar menjadi nota debit." />
      <Card>
        <CardContent className="grid gap-4 pt-5 md:grid-cols-3">
          {!receiptId ? (
            <FormField label="Cari no. penerimaan" htmlFor="grn-q" className="md:col-span-3">
              <Input id="grn-q" placeholder="GRN/2026/…" value={receiptNo} onChange={(e) => setReceiptNo(e.target.value)} />
              {search.data?.content.length ? (
                <ul className="mt-2 divide-y divide-border rounded-lg border border-border">
                  {search.data.content.map((g) => (
                    <li key={g.id}>
                      <button type="button" className="w-full px-3 py-2 text-left text-sm hover:bg-surface-muted" onClick={() => setReceiptId(String(g.id))}>
                        <span className="font-mono text-xs">{g.docNo}</span> · {g.supplier.name}
                      </button>
                    </li>
                  ))}
                </ul>
              ) : null}
            </FormField>
          ) : (
            <>
              <FormField label="Penerimaan" htmlFor="grn">
                <div className="flex items-center gap-2">
                  <Input id="grn" value={grn.data ? `${grn.data.docNo} · ${grn.data.supplier.name}` : "…"} readOnly />
                  <Button size="sm" variant="ghost" onClick={() => (setReceiptId(""), setQtys({}), setInvoiceId(""))}>
                    Ganti
                  </Button>
                </div>
              </FormField>
              <FormField label="Tanggal retur" htmlFor="docDate">
                <Input id="docDate" type="date" value={docDate} onChange={(e) => setDocDate(e.target.value)} />
              </FormField>
              <FormField label="Nota debit atas faktur" htmlFor="inv" hint="Kosong = retur sebelum difakturkan">
                <Select id="inv" value={invoiceId} onChange={(e) => setInvoiceId(e.target.value)}>
                  <option value="">— Belum difakturkan —</option>
                  {invoices.data?.map((i) => (
                    <option key={i.id} value={i.id}>
                      {i.docNo} ({i.supplierInvoiceNo}) · sisa {formatAmount(i.outstanding)}
                    </option>
                  ))}
                </Select>
              </FormField>
              <FormField label="Alasan" htmlFor="reason" required className="md:col-span-3">
                <Input id="reason" value={reason} onChange={(e) => setReason(e.target.value)} />
              </FormField>
            </>
          )}
        </CardContent>
      </Card>
      {grn.data && (
        <Card>
          <table className="w-full text-sm">
            <thead>
              <tr className="border-b border-border bg-surface-muted/60 text-left text-xs uppercase text-muted">
                <th className="px-4 py-2.5">Item</th>
                <th className="px-4 py-2.5 text-right">Diterima</th>
                <th className="px-4 py-2.5 text-right">Difakturkan</th>
                <th className="px-4 py-2.5 text-right">Sudah diretur</th>
                <th className="px-4 py-2.5 text-right">Biaya/unit</th>
                <th className="w-40 px-4 py-2.5 text-right">Qty retur</th>
              </tr>
            </thead>
            <tbody>
              {grn.data.lines.map((l) => (
                <tr key={l.id} className="border-b border-border">
                  <td className="px-4 py-2">
                    <span className="font-mono text-xs">{l.itemCode}</span> {l.itemName}
                  </td>
                  <td className="num px-4 py-2">
                    {formatQty(l.qty)} {l.uomCode}
                  </td>
                  <td className="num px-4 py-2">{formatQty(l.qtyInvoiced)}</td>
                  <td className="num px-4 py-2">{formatQty(l.qtyReturned)}</td>
                  <td className="num px-4 py-2">{formatAmount(l.unitCost)}</td>
                  <td className="px-4 py-1.5">
                    <Input className="num" inputMode="decimal" value={qtys[l.id] ?? ""} onChange={(e) => setQtys({ ...qtys, [l.id]: e.target.value.replace(",", ".") })} aria-label={`Qty retur ${l.itemCode}`} />
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </Card>
      )}
      <div className="flex justify-end gap-2">
        <Button variant="outline" onClick={() => router.back()}>
          Batal
        </Button>
        <Button onClick={submit} loading={saving} disabled={!grn.data}>
          Posting retur
        </Button>
      </div>
    </div>
  );
}

export function ReturnForm() {
  return (
    <RequirePermission permission="PURCHASE:CREATE">
      <Form />
    </RequirePermission>
  );
}
