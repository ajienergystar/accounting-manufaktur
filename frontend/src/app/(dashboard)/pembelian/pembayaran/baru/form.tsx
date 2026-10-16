"use client";

import { useRouter, useSearchParams } from "next/navigation";
import { useState } from "react";
import { useQuery, useQueryClient } from "@tanstack/react-query";
import { toast } from "sonner";
import { AccountPicker } from "@/components/shared/account-picker";
import { TotalsPanel } from "@/components/shared/info";
import { PageHeader } from "@/components/shared/page-header";
import { SupplierPicker } from "@/components/shared/pickers";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { Card, CardContent } from "@/components/ui/card";
import { FormField } from "@/components/ui/form-field";
import { Input } from "@/components/ui/input";
import { MoneyInput } from "@/components/ui/money-input";
import { RequirePermission } from "@/features/auth/require-permission";
import { PAYMENT_TYPE_LABEL, type ApPayment, type PurchaseInvoice } from "@/features/purchasing/types";
import { sumDecimal } from "@/features/ledger/types";
import { api, errorMessage } from "@/lib/api";
import { formatAmount, formatDate, formatRupiah, todayIso } from "@/lib/format";
import { cn } from "@/lib/utils";

type PayType = ApPayment["paymentType"];

function Form() {
  const router = useRouter();
  const params = useSearchParams();
  const qc = useQueryClient();
  const preInvoice = params.get("invoiceId");
  const [supplierId, setSupplierId] = useState<number | null>(params.get("supplierId") ? Number(params.get("supplierId")) : null);
  const [type, setType] = useState<PayType>("INVOICE");
  const [docDate, setDocDate] = useState(todayIso());
  const [cashAccountId, setCashAccountId] = useState<number | null>(null);
  const [amount, setAmount] = useState("");
  const [bankCharges, setBankCharges] = useState("");
  const [referenceNo, setReferenceNo] = useState("");
  const [notes, setNotes] = useState("");
  const [alloc, setAlloc] = useState<Record<number, string>>({});
  const [initialized, setInitialized] = useState(false);
  const [saving, setSaving] = useState(false);

  const invoices = useQuery({
    queryKey: ["/purchasing/invoices", "outstanding", supplierId],
    queryFn: () => api.get<PurchaseInvoice[]>("/purchasing/invoices/outstanding", { supplierId }),
    enabled: supplierId !== null,
  });
  const advance = useQuery({
    queryKey: ["/purchasing/payments", "advance", supplierId],
    queryFn: () => api.get<string>(`/purchasing/suppliers/${supplierId}/advance-balance`),
    enabled: supplierId !== null,
  });

  if (!initialized && preInvoice && invoices.data) {
    const inv = invoices.data.find((i) => String(i.id) === preInvoice);
    if (inv) setAlloc({ [inv.id]: inv.outstanding });
    setInitialized(true);
  }

  const allocTotal = sumDecimal(Object.values(alloc));
  const total = type === "ADVANCE" ? Number(amount || 0) : allocTotal;

  const submit = async () => {
    if (!supplierId) return toast.error("Pilih pemasok");
    if (type !== "ADVANCE_OFFSET" && !cashAccountId) return toast.error("Pilih akun kas/bank");
    const allocations = Object.entries(alloc)
      .filter(([, v]) => Number(v) > 0)
      .map(([invoiceId, v]) => ({ invoiceId: Number(invoiceId), amount: v }));
    if (type !== "ADVANCE" && !allocations.length) return toast.error("Isi nominal pembayaran untuk minimal satu faktur");
    setSaving(true);
    try {
      const res = await api.post<ApPayment>("/purchasing/payments", {
        docDate,
        supplierId,
        paymentType: type,
        cashAccountId: type === "ADVANCE_OFFSET" ? null : cashAccountId,
        amount: type === "ADVANCE" ? amount : null,
        bankCharges: type === "ADVANCE_OFFSET" ? "0" : bankCharges || "0",
        referenceNo: referenceNo || null,
        notes: notes || null,
        allocations: type === "ADVANCE" ? [] : allocations,
      });
      toast.success(`${res.docNo} diposting`);
      qc.invalidateQueries({ queryKey: ["/purchasing/payments"] });
      qc.invalidateQueries({ queryKey: ["/purchasing/invoices"] });
      router.push(`/pembelian/pembayaran/${res.id}`);
    } catch (e) {
      toast.error(errorMessage(e));
    } finally {
      setSaving(false);
    }
  };

  return (
    <div className="space-y-6">
      <PageHeader title="Pembayaran Utang" />
      <div role="radiogroup" aria-label="Jenis pembayaran" className="grid gap-3 sm:grid-cols-3">
        {(Object.keys(PAYMENT_TYPE_LABEL) as PayType[]).map((t) => (
          <button
            key={t}
            type="button"
            role="radio"
            aria-checked={type === t}
            onClick={() => setType(t)}
            className={cn("rounded-xl border p-4 text-left text-sm", type === t ? "border-primary bg-primary-soft" : "border-border bg-surface hover:bg-surface-muted")}
          >
            <span className="font-medium">{PAYMENT_TYPE_LABEL[t]}</span>
            <span className="mt-0.5 block text-xs text-muted">
              {t === "INVOICE" ? "Bayar faktur dari kas/bank" : t === "ADVANCE" ? "Bayar di muka sebelum ada faktur" : "Potong faktur dengan saldo uang muka"}
            </span>
          </button>
        ))}
      </div>
      <Card>
        <CardContent className="grid gap-4 pt-5 md:grid-cols-3">
          <FormField label="Pemasok" htmlFor="supplier" required className="md:col-span-2">
            <SupplierPicker id="supplier" value={supplierId} onChange={(v) => (setSupplierId(v), setAlloc({}))} />
          </FormField>
          <FormField label="Tanggal" htmlFor="docDate" required>
            <Input id="docDate" type="date" value={docDate} onChange={(e) => setDocDate(e.target.value)} />
          </FormField>
          {type !== "ADVANCE_OFFSET" && (
            <FormField label="Dari akun kas/bank" htmlFor="cash" required>
              <AccountPicker id="cash" cashBank value={cashAccountId} onChange={setCashAccountId} />
            </FormField>
          )}
          {type === "ADVANCE" && (
            <FormField label="Nominal uang muka" htmlFor="amount" required>
              <MoneyInput id="amount" value={amount} onValueChange={setAmount} />
            </FormField>
          )}
          {type !== "ADVANCE_OFFSET" && (
            <FormField label="Biaya transfer (Rp)" htmlFor="charges">
              <MoneyInput id="charges" value={bankCharges} onValueChange={setBankCharges} />
            </FormField>
          )}
          <FormField label="No. referensi / bukti transfer" htmlFor="ref">
            <Input id="ref" value={referenceNo} onChange={(e) => setReferenceNo(e.target.value)} />
          </FormField>
          <FormField label="Catatan" htmlFor="notes" className="md:col-span-2">
            <Input id="notes" value={notes} onChange={(e) => setNotes(e.target.value)} />
          </FormField>
          {supplierId && (
            <p className="text-sm text-muted md:col-span-3">
              Saldo uang muka pemasok: <b className="text-foreground">{formatRupiah(advance.data ?? 0)}</b>
            </p>
          )}
        </CardContent>
      </Card>

      {type !== "ADVANCE" && (
        <Card>
          {!supplierId ? (
            <p className="px-5 py-4 text-sm text-muted">Pilih pemasok untuk menampilkan faktur terbuka.</p>
          ) : !invoices.data?.length ? (
            <p className="px-5 py-4 text-sm text-muted">Tidak ada faktur yang belum lunas.</p>
          ) : (
            <table className="w-full text-sm">
              <thead>
                <tr className="border-b border-border bg-surface-muted/60 text-left text-xs uppercase text-muted">
                  <th className="px-4 py-2.5">Faktur</th>
                  <th className="px-4 py-2.5">Jatuh tempo</th>
                  <th className="px-4 py-2.5 text-right">Sisa utang</th>
                  <th className="w-52 px-4 py-2.5 text-right">Dibayar</th>
                </tr>
              </thead>
              <tbody>
                {invoices.data.map((i) => (
                  <tr key={i.id} className="border-b border-border">
                    <td className="px-4 py-2">
                      <span className="font-mono text-xs">{i.docNo}</span>
                      <span className="block text-xs text-muted">{i.supplierInvoiceNo}</span>
                    </td>
                    <td className="px-4 py-2">
                      {formatDate(i.dueDate)} {i.daysOverdue > 0 && <Badge tone="danger">{i.daysOverdue} hr</Badge>}
                    </td>
                    <td className="num px-4 py-2">{formatAmount(i.outstanding)}</td>
                    <td className="px-4 py-1.5">
                      <div className="flex gap-1">
                        <MoneyInput value={alloc[i.id] ?? ""} onValueChange={(v) => setAlloc({ ...alloc, [i.id]: v })} aria-label={`Bayar ${i.docNo}`} />
                        <Button size="sm" variant="ghost" onClick={() => setAlloc({ ...alloc, [i.id]: i.outstanding })}>
                          Lunas
                        </Button>
                      </div>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          )}
        </Card>
      )}

      <Card>
        <CardContent className="pt-5">
          <TotalsPanel
            rows={[
              { label: "Jumlah dibayar", value: formatAmount(total) },
              ...(type !== "ADVANCE_OFFSET" ? [{ label: "Biaya transfer", value: formatAmount(bankCharges || 0), muted: true }] : []),
              {
                label: type === "ADVANCE_OFFSET" ? "Dipotong dari uang muka" : "Total keluar kas/bank",
                value: formatAmount(total + (type === "ADVANCE_OFFSET" ? 0 : Number(bankCharges || 0))),
                strong: true,
              },
            ]}
          />
        </CardContent>
      </Card>
      <div className="flex justify-end gap-2">
        <Button variant="outline" onClick={() => router.back()}>
          Batal
        </Button>
        <Button onClick={submit} loading={saving}>
          Posting pembayaran
        </Button>
      </div>
    </div>
  );
}

export function PaymentForm() {
  return (
    <RequirePermission permission="PURCHASE:VIEW">
      <Form />
    </RequirePermission>
  );
}
