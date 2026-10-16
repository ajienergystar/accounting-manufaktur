"use client";

import { useRouter, useSearchParams } from "next/navigation";
import { useState } from "react";
import { useQuery, useQueryClient } from "@tanstack/react-query";
import { Plus, Trash2 } from "lucide-react";
import { toast } from "sonner";
import { AccountPicker } from "@/components/shared/account-picker";
import { TotalsPanel } from "@/components/shared/info";
import { PageHeader } from "@/components/shared/page-header";
import { MasterSelect, SupplierPicker, useTaxRates } from "@/components/shared/pickers";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { FormField } from "@/components/ui/form-field";
import { Input } from "@/components/ui/input";
import { MoneyInput } from "@/components/ui/money-input";
import { Select } from "@/components/ui/select";
import { useAuth } from "@/features/auth/auth-context";
import { RequirePermission } from "@/features/auth/require-permission";
import type { PurchaseInvoice, UninvoicedLine } from "@/features/purchasing/types";
import { api, errorMessage } from "@/lib/api";
import { formatAmount, formatDate, formatQty, todayIso } from "@/lib/format";

interface Sel {
  on: boolean;
  qty: string;
  price: string;
}
interface Exp {
  accountId: number | null;
  departmentId: string;
  description: string;
  qty: string;
  unitPrice: string;
}

const r2 = (n: number) => Math.round(n * 100) / 100;

function Form() {
  const router = useRouter();
  const params = useSearchParams();
  const qc = useQueryClient();
  const { can } = useAuth();
  const { data: taxes } = useTaxRates();
  const [supplierId, setSupplierId] = useState<number | null>(params.get("supplierId") ? Number(params.get("supplierId")) : null);
  const [docDate, setDocDate] = useState(todayIso());
  const [dueDate, setDueDate] = useState("");
  const [supplierInvoiceNo, setSupplierInvoiceNo] = useState("");
  const [taxInvoiceNo, setTaxInvoiceNo] = useState("");
  const [taxRateId, setTaxRateId] = useState("");
  const [pphRateId, setPphRateId] = useState("");
  const [discount, setDiscount] = useState("");
  const [freight, setFreight] = useState("");
  const [notes, setNotes] = useState("");
  const [override, setOverride] = useState(false);
  const [sel, setSel] = useState<Record<number, Sel>>({});
  const [exp, setExp] = useState<Exp[]>([]);
  const [saving, setSaving] = useState(false);

  const uninvoiced = useQuery({
    queryKey: ["/purchasing/receipts", "uninvoiced", supplierId],
    queryFn: () => api.get<UninvoicedLine[]>("/purchasing/receipts/uninvoiced", { supplierId }),
    enabled: supplierId !== null,
  });

  const get = (l: UninvoicedLine): Sel => sel[l.receiptLineId] ?? { on: false, qty: l.qtyOpen, price: l.unitPrice };
  const stockTotal = r2((uninvoiced.data ?? []).reduce((a, l) => (get(l).on ? a + r2(Number(get(l).qty || 0) * Number(get(l).price || 0)) : a), 0));
  const expTotal = r2(exp.reduce((a, e) => a + r2(Number(e.qty || 0) * Number(e.unitPrice || 0)), 0));
  const subtotal = r2(stockTotal + expTotal);
  const dpp = r2(subtotal - Number(discount || 0) + Number(freight || 0));
  const ppnRate = Number(taxes?.find((t) => String(t.id) === taxRateId)?.rate ?? 0);
  const pphRate = Number(taxes?.find((t) => String(t.id) === pphRateId)?.rate ?? 0);
  const ppn = r2((dpp * ppnRate) / 100);
  const expNet = subtotal === 0 ? 0 : expTotal - (Number(discount || 0) * expTotal) / subtotal;
  const pph = r2((expNet * pphRate) / 100);
  const total = r2(dpp + ppn);

  const submit = async () => {
    if (!supplierId) return toast.error("Pilih pemasok");
    if (!supplierInvoiceNo.trim()) return toast.error("Isi nomor faktur pemasok");
    const stockLines = (uninvoiced.data ?? [])
      .filter((l) => get(l).on && Number(get(l).qty) > 0)
      .map((l) => ({ receiptLineId: l.receiptLineId, qty: get(l).qty, unitPrice: get(l).price }));
    const expLines = exp
      .filter((e) => e.accountId && Number(e.qty) > 0)
      .map((e) => ({ accountId: e.accountId, departmentId: e.departmentId ? Number(e.departmentId) : null, description: e.description, qty: e.qty, unitPrice: e.unitPrice || "0" }));
    if (!stockLines.length && !expLines.length) return toast.error("Pilih baris penerimaan atau tambah baris biaya");
    setSaving(true);
    try {
      const res = await api.post<PurchaseInvoice>("/purchasing/invoices", {
        docDate,
        dueDate: dueDate || null,
        supplierId,
        supplierInvoiceNo,
        taxInvoiceNo: taxInvoiceNo || null,
        taxRateId: taxRateId ? Number(taxRateId) : null,
        pphRateId: pphRateId ? Number(pphRateId) : null,
        discountAmount: discount || "0",
        freightAmount: freight || "0",
        notes: notes || null,
        overridePriceCheck: override,
        lines: [...stockLines, ...expLines],
      });
      toast.success(`Faktur ${res.docNo} diposting`);
      qc.invalidateQueries({ queryKey: ["/purchasing/invoices"] });
      qc.invalidateQueries({ queryKey: ["/purchasing/receipts"] });
      router.push(`/pembelian/faktur/${res.id}`);
    } catch (e) {
      toast.error(errorMessage(e));
    } finally {
      setSaving(false);
    }
  };

  return (
    <div className="space-y-6">
      <PageHeader title="Faktur Pembelian" description="Cocokkan tagihan pemasok dengan barang yang sudah diterima, lalu posting ke utang usaha." />
      <Card>
        <CardContent className="grid gap-4 pt-5 md:grid-cols-4">
          <FormField label="Pemasok" htmlFor="supplier" required className="md:col-span-2">
            <SupplierPicker id="supplier" value={supplierId} onChange={(v) => (setSupplierId(v), setSel({}))} />
          </FormField>
          <FormField label="Tanggal faktur" htmlFor="docDate" required>
            <Input id="docDate" type="date" value={docDate} onChange={(e) => setDocDate(e.target.value)} />
          </FormField>
          <FormField label="Jatuh tempo" htmlFor="dueDate" hint="Kosong = sesuai termin pemasok">
            <Input id="dueDate" type="date" value={dueDate} onChange={(e) => setDueDate(e.target.value)} />
          </FormField>
          <FormField label="No. faktur pemasok" htmlFor="supInv" required>
            <Input id="supInv" value={supplierInvoiceNo} onChange={(e) => setSupplierInvoiceNo(e.target.value)} />
          </FormField>
          <FormField label="No. faktur pajak" htmlFor="taxInv">
            <Input id="taxInv" placeholder="010.000-26.00000000" value={taxInvoiceNo} onChange={(e) => setTaxInvoiceNo(e.target.value)} />
          </FormField>
          <FormField label="PPN" htmlFor="ppn">
            <Select id="ppn" value={taxRateId} onChange={(e) => setTaxRateId(e.target.value)}>
              <option value="">Tanpa PPN</option>
              {taxes?.filter((t) => t.taxType === "PPN").map((t) => (
                <option key={t.id} value={t.id}>
                  {t.name}
                </option>
              ))}
            </Select>
          </FormField>
          <FormField label="PPh dipotong" htmlFor="pph" hint="Untuk baris jasa (mis. PPh 23)">
            <Select id="pph" value={pphRateId} onChange={(e) => setPphRateId(e.target.value)}>
              <option value="">Tidak ada</option>
              {taxes?.filter((t) => t.taxType !== "PPN").map((t) => (
                <option key={t.id} value={t.id}>
                  {t.name}
                </option>
              ))}
            </Select>
          </FormField>
        </CardContent>
      </Card>

      <Card>
        <CardHeader>
          <CardTitle>Barang yang sudah diterima (belum difakturkan)</CardTitle>
        </CardHeader>
        {!supplierId ? (
          <p className="px-5 py-4 text-sm text-muted">Pilih pemasok terlebih dahulu.</p>
        ) : !uninvoiced.data?.length ? (
          <p className="px-5 py-4 text-sm text-muted">Tidak ada penerimaan yang belum difakturkan.</p>
        ) : (
          <div className="overflow-x-auto">
            <table className="w-full min-w-[900px] text-sm">
              <thead>
                <tr className="border-b border-border bg-surface-muted/60 text-left text-xs uppercase text-muted">
                  <th className="w-10 px-3 py-2.5" />
                  <th className="px-3 py-2.5">Penerimaan</th>
                  <th className="px-3 py-2.5">Item</th>
                  <th className="px-3 py-2.5 text-right">Sisa qty</th>
                  <th className="px-3 py-2.5 text-right">Harga terima</th>
                  <th className="w-32 px-3 py-2.5 text-right">Qty faktur</th>
                  <th className="w-40 px-3 py-2.5 text-right">Harga faktur</th>
                </tr>
              </thead>
              <tbody>
                {uninvoiced.data.map((l) => {
                  const s = get(l);
                  const set = (patch: Partial<Sel>) => setSel({ ...sel, [l.receiptLineId]: { ...s, ...patch } });
                  return (
                    <tr key={l.receiptLineId} className="border-b border-border">
                      <td className="px-3 py-2">
                        <input type="checkbox" className="h-4 w-4 accent-primary" checked={s.on} onChange={(e) => set({ on: e.target.checked })} aria-label={`Pilih ${l.itemCode} ${l.receiptNo}`} />
                      </td>
                      <td className="px-3 py-2">
                        <span className="font-mono text-xs">{l.receiptNo}</span>
                        <span className="block text-xs text-muted">{formatDate(l.receiptDate)}</span>
                      </td>
                      <td className="px-3 py-2">
                        <span className="font-mono text-xs">{l.itemCode}</span> {l.itemName}
                      </td>
                      <td className="num px-3 py-2">
                        {formatQty(l.qtyOpen)} {l.uomCode}
                      </td>
                      <td className="num px-3 py-2">{formatAmount(l.unitPrice)}</td>
                      <td className="px-3 py-1.5">
                        <Input className="num" inputMode="decimal" disabled={!s.on} value={s.qty} onChange={(e) => set({ qty: e.target.value.replace(",", ".") })} aria-label={`Qty faktur ${l.itemCode}`} />
                      </td>
                      <td className="px-3 py-1.5">
                        <MoneyInput disabled={!s.on} value={s.price} onValueChange={(v) => set({ price: v })} aria-label={`Harga faktur ${l.itemCode}`} />
                      </td>
                    </tr>
                  );
                })}
              </tbody>
            </table>
          </div>
        )}
      </Card>

      <Card>
        <CardHeader>
          <CardTitle>Baris jasa / biaya (tanpa penerimaan barang)</CardTitle>
          <Button size="sm" variant="outline" onClick={() => setExp([...exp, { accountId: null, departmentId: "", description: "", qty: "1", unitPrice: "" }])}>
            <Plus className="h-4 w-4" /> Tambah
          </Button>
        </CardHeader>
        {exp.length === 0 ? (
          <p className="px-5 py-4 text-sm text-muted">Mis. jasa perbaikan mesin (BOP), sewa, atau biaya lain.</p>
        ) : (
          <table className="w-full text-sm">
            <tbody>
              {exp.map((e, i) => {
                const set = (patch: Partial<Exp>) => setExp(exp.map((x, j) => (j === i ? { ...x, ...patch } : x)));
                return (
                  <tr key={i} className="border-b border-border">
                    <td className="w-72 px-3 py-2">
                      <AccountPicker value={e.accountId} onChange={(v) => set({ accountId: v })} aria-label={`Akun biaya ${i + 1}`} />
                    </td>
                    <td className="w-44 px-3 py-2">
                      <MasterSelect resource="/departments" emptyLabel="Departemen" value={e.departmentId} onChange={(ev) => set({ departmentId: ev.target.value })} aria-label={`Departemen ${i + 1}`} />
                    </td>
                    <td className="px-3 py-2">
                      <Input placeholder="Keterangan" value={e.description} onChange={(ev) => set({ description: ev.target.value })} aria-label={`Keterangan ${i + 1}`} />
                    </td>
                    <td className="w-24 px-3 py-2">
                      <Input className="num" value={e.qty} onChange={(ev) => set({ qty: ev.target.value })} aria-label={`Qty ${i + 1}`} />
                    </td>
                    <td className="w-40 px-3 py-2">
                      <MoneyInput value={e.unitPrice} onValueChange={(v) => set({ unitPrice: v })} aria-label={`Harga ${i + 1}`} />
                    </td>
                    <td className="w-10">
                      <Button size="icon" variant="ghost" onClick={() => setExp(exp.filter((_, j) => j !== i))} aria-label="Hapus">
                        <Trash2 className="h-4 w-4 text-danger" />
                      </Button>
                    </td>
                  </tr>
                );
              })}
            </tbody>
          </table>
        )}
      </Card>

      <Card>
        <CardContent className="grid gap-6 pt-5 md:grid-cols-2">
          <div className="space-y-4">
            <div className="grid grid-cols-2 gap-4">
              <FormField label="Diskon (Rp)" htmlFor="disc">
                <MoneyInput id="disc" value={discount} onValueChange={setDiscount} />
              </FormField>
              <FormField label="Ongkos angkut ditagih (Rp)" htmlFor="freight" hint="Ongkir yang sudah dicatat di penerimaan">
                <MoneyInput id="freight" value={freight} onValueChange={setFreight} />
              </FormField>
            </div>
            <FormField label="Catatan" htmlFor="notes">
              <Input id="notes" value={notes} onChange={(e) => setNotes(e.target.value)} />
            </FormField>
            {can("PURCHASE:APPROVE") && (
              <label className="flex items-start gap-2 text-sm">
                <input type="checkbox" className="mt-0.5 h-4 w-4 accent-primary" checked={override} onChange={(e) => setOverride(e.target.checked)} />
                <span>
                  Setujui selisih harga di atas toleransi 5%
                  <span className="block text-xs text-muted">Selisih dibukukan ke akun selisih harga pembelian.</span>
                </span>
              </label>
            )}
          </div>
          <TotalsPanel
            rows={[
              { label: "Subtotal barang", value: formatAmount(stockTotal), muted: true },
              { label: "Subtotal jasa/biaya", value: formatAmount(expTotal), muted: true },
              { label: "Diskon", value: `(${formatAmount(discount || 0)})`, muted: true },
              { label: "Ongkos angkut", value: formatAmount(freight || 0), muted: true },
              { label: "DPP", value: formatAmount(dpp) },
              { label: `PPN ${ppnRate ? `${ppnRate}%` : ""}`, value: formatAmount(ppn) },
              { label: "Total faktur", value: formatAmount(total), strong: true },
              { label: `PPh dipotong ${pphRate ? `${pphRate}%` : ""}`, value: `(${formatAmount(pph)})`, muted: true },
              { label: "Utang ke pemasok", value: formatAmount(total - pph), strong: true },
            ]}
          />
        </CardContent>
      </Card>
      <div className="flex justify-end gap-2">
        <Button variant="outline" onClick={() => router.back()}>
          Batal
        </Button>
        <Button onClick={submit} loading={saving}>
          Posting faktur
        </Button>
      </div>
    </div>
  );
}

export function PiForm() {
  return (
    <RequirePermission permission="PURCHASE:CREATE">
      <Form />
    </RequirePermission>
  );
}
