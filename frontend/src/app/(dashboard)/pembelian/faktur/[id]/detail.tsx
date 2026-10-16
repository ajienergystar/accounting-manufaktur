"use client";

import Link from "next/link";
import { useParams } from "next/navigation";
import { useState } from "react";
import { useQuery } from "@tanstack/react-query";
import { Ban, Printer, Wallet } from "lucide-react";
import { Attachments } from "@/components/shared/attachments";
import { Info, InfoGrid, TotalsPanel } from "@/components/shared/info";
import { PageHeader } from "@/components/shared/page-header";
import { ErrorState } from "@/components/shared/states";
import { StatusBadge } from "@/components/shared/status-badge";
import { VoidDialog } from "@/components/shared/void-dialog";
import { Badge } from "@/components/ui/badge";
import { Button, buttonClasses } from "@/components/ui/button";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Skeleton } from "@/components/ui/skeleton";
import { useAuth } from "@/features/auth/auth-context";
import { RequirePermission } from "@/features/auth/require-permission";
import { useDocAction } from "@/features/documents/use-doc-action";
import { PAYMENT_TYPE_LABEL, type PurchaseInvoice } from "@/features/purchasing/types";
import { api, errorMessage } from "@/lib/api";
import { formatAmount, formatDate, formatQty } from "@/lib/format";

function Detail() {
  const { id } = useParams<{ id: string }>();
  const { can } = useAuth();
  const [voiding, setVoiding] = useState(false);
  const key = "/purchasing/invoices";
  const { data, isLoading, error, refetch } = useQuery({ queryKey: [key, id], queryFn: () => api.get<PurchaseInvoice>(`${key}/${id}`) });
  const voidDoc = useDocAction([key, "/purchasing/receipts"], "Faktur dibatalkan");

  if (isLoading) return <Skeleton className="h-96 w-full" />;
  if (error || !data) return <ErrorState message={errorMessage(error)} onRetry={refetch} />;

  const open = data.status === "UNPAID" || data.status === "PARTIAL";
  return (
    <>
      <PageHeader
        title={`Faktur ${data.docNo}`}
        description={`${data.supplier.name} · No. faktur pemasok ${data.supplierInvoiceNo}`}
        actions={
          <>
            <Button variant="outline" onClick={() => window.print()}>
              <Printer className="h-4 w-4" /> Cetak
            </Button>
            {data.status === "UNPAID" && Number(data.returnedAmount) === 0 && can("PURCHASE:APPROVE") && (
              <Button variant="outline" onClick={() => setVoiding(true)}>
                <Ban className="h-4 w-4 text-danger" /> Batalkan
              </Button>
            )}
            {open && (can("PURCHASE:POST") || can("CASHBANK:CREATE")) && (
              <Link href={`/pembelian/pembayaran/baru?supplierId=${data.supplier.id}&invoiceId=${data.id}`} className={buttonClasses()}>
                <Wallet className="h-4 w-4" /> Bayar
              </Link>
            )}
          </>
        }
      />
      <Card className="mb-6">
        <CardContent className="pt-5">
          <InfoGrid>
            <Info label="Tanggal" value={formatDate(data.docDate)} />
            <Info
              label="Jatuh tempo"
              value={
                <span className="flex items-center gap-1.5">
                  {formatDate(data.dueDate)} {data.daysOverdue > 0 && <Badge tone="danger">lewat {data.daysOverdue} hari</Badge>}
                </span>
              }
            />
            <Info label="Status" value={<StatusBadge status={data.status} />} />
            <Info label="Faktur pajak" value={data.taxInvoiceNo ?? "-"} />
            <Info label="Jurnal" value={data.journalId ? <Link className="text-primary hover:underline" href={`/jurnal/${data.journalId}`}>Lihat jurnal</Link> : "-"} />
            <Info label="Catatan" value={data.notes ?? "-"} />
          </InfoGrid>
        </CardContent>
      </Card>
      <Card className="mb-6">
        <div className="overflow-x-auto">
          <table className="w-full min-w-[800px] text-sm">
            <thead>
              <tr className="border-b border-border bg-surface-muted/60 text-left text-xs uppercase text-muted">
                <th className="px-4 py-2.5">Keterangan</th>
                <th className="px-4 py-2.5">Sumber</th>
                <th className="px-4 py-2.5 text-right">Qty</th>
                <th className="px-4 py-2.5 text-right">Harga</th>
                <th className="px-4 py-2.5 text-right">Jumlah</th>
              </tr>
            </thead>
            <tbody>
              {data.lines.map((l) => (
                <tr key={l.id} className="border-b border-border">
                  <td className="px-4 py-2">{l.description}</td>
                  <td className="px-4 py-2 text-muted">
                    {l.receiptNo ?? `${l.accountCode} ${l.accountName}`}
                    {l.departmentName && ` · ${l.departmentName}`}
                  </td>
                  <td className="num px-4 py-2">{formatQty(l.qty)}</td>
                  <td className="num px-4 py-2">{formatAmount(l.unitPrice)}</td>
                  <td className="num px-4 py-2">{formatAmount(l.lineTotal)}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
        <div className="p-4">
          <TotalsPanel
            rows={[
              { label: "Subtotal", value: formatAmount(data.subtotal) },
              { label: "Diskon", value: `(${formatAmount(data.discountAmount)})`, muted: true },
              { label: "Ongkos angkut", value: formatAmount(data.freightAmount), muted: true },
              { label: "DPP", value: formatAmount(data.dpp) },
              { label: `PPN ${Number(data.taxRate) ? `${Number(data.taxRate)}%` : ""}`, value: formatAmount(data.taxAmount) },
              { label: "Total", value: formatAmount(data.total), strong: true },
              { label: "PPh dipotong", value: `(${formatAmount(data.pphAmount)})`, muted: true },
              { label: "Utang", value: formatAmount(data.payableAmount) },
              { label: "Dibayar", value: `(${formatAmount(data.paidAmount)})`, muted: true },
              { label: "Retur / nota debit", value: `(${formatAmount(data.returnedAmount)})`, muted: true },
              { label: "Sisa utang", value: formatAmount(data.outstanding), strong: true },
            ]}
          />
        </div>
      </Card>
      {data.payments.length > 0 && (
        <Card className="mb-6">
          <CardHeader>
            <CardTitle>Riwayat pembayaran</CardTitle>
          </CardHeader>
          <ul className="divide-y divide-border text-sm">
            {data.payments.map((p) => (
              <li key={p.paymentId} className="flex items-center justify-between px-5 py-2.5">
                <Link className="font-mono text-xs text-primary hover:underline" href={`/pembelian/pembayaran/${p.paymentId}`}>
                  {p.paymentNo}
                </Link>
                <span className="text-muted">
                  {formatDate(p.date)} · {PAYMENT_TYPE_LABEL[p.type as keyof typeof PAYMENT_TYPE_LABEL]}
                </span>
                <span className="num">{formatAmount(p.amount)}</span>
              </li>
            ))}
          </ul>
        </Card>
      )}
      <Attachments entityType="PURCHASE_INVOICE" entityId={data.id} canEdit={can("PURCHASE:EDIT")} />
      <VoidDialog
        open={voiding}
        loading={voidDoc.isPending}
        onClose={() => setVoiding(false)}
        onConfirm={(b) => voidDoc.mutate({ path: `${key}/${id}/void`, body: b }, { onSuccess: () => (setVoiding(false), refetch()) })}
      />
    </>
  );
}

export function PiDetail() {
  return (
    <RequirePermission permission="PURCHASE:VIEW">
      <Detail />
    </RequirePermission>
  );
}
