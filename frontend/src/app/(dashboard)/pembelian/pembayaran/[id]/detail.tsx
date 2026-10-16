"use client";

import Link from "next/link";
import { useParams } from "next/navigation";
import { useState } from "react";
import { useQuery } from "@tanstack/react-query";
import { Ban, Printer } from "lucide-react";
import { Info, InfoGrid } from "@/components/shared/info";
import { PageHeader } from "@/components/shared/page-header";
import { ErrorState } from "@/components/shared/states";
import { StatusBadge } from "@/components/shared/status-badge";
import { VoidDialog } from "@/components/shared/void-dialog";
import { Button } from "@/components/ui/button";
import { Card, CardContent } from "@/components/ui/card";
import { Skeleton } from "@/components/ui/skeleton";
import { useAuth } from "@/features/auth/auth-context";
import { RequirePermission } from "@/features/auth/require-permission";
import { useDocAction } from "@/features/documents/use-doc-action";
import { PAYMENT_TYPE_LABEL, type ApPayment } from "@/features/purchasing/types";
import { api, errorMessage } from "@/lib/api";
import { formatAmount, formatDate } from "@/lib/format";

function Detail() {
  const { id } = useParams<{ id: string }>();
  const { can } = useAuth();
  const [voiding, setVoiding] = useState(false);
  const key = "/purchasing/payments";
  const { data, isLoading, error, refetch } = useQuery({ queryKey: [key, id], queryFn: () => api.get<ApPayment>(`${key}/${id}`) });
  const voidDoc = useDocAction([key, "/purchasing/invoices"], "Pembayaran dibatalkan");

  if (isLoading) return <Skeleton className="h-96 w-full" />;
  if (error || !data) return <ErrorState message={errorMessage(error)} onRetry={refetch} />;

  return (
    <>
      <PageHeader
        title={`${PAYMENT_TYPE_LABEL[data.paymentType]} ${data.docNo}`}
        description={data.notes ?? undefined}
        actions={
          <>
            <Button variant="outline" onClick={() => window.print()}>
              <Printer className="h-4 w-4" /> Cetak bukti
            </Button>
            {data.status === "POSTED" && can("PURCHASE:APPROVE") && (
              <Button variant="outline" onClick={() => setVoiding(true)}>
                <Ban className="h-4 w-4 text-danger" /> Batalkan
              </Button>
            )}
          </>
        }
      />
      <Card className="mb-6">
        <CardContent className="pt-5">
          <InfoGrid>
            <Info label="Pemasok" value={data.supplier.name} />
            <Info label="Tanggal" value={formatDate(data.docDate)} />
            <Info label="Kas/Bank" value={data.cashAccount ? `${data.cashAccount.code} ${data.cashAccount.name}` : "Saldo uang muka"} />
            <Info label="Status" value={<StatusBadge status={data.status} />} />
            <Info label="Jumlah" value={<span className="text-lg font-semibold">{formatAmount(data.amount)}</span>} />
            <Info label="Biaya transfer" value={formatAmount(data.bankCharges)} />
            <Info label="Referensi" value={data.referenceNo ?? "-"} />
            <Info label="Jurnal" value={data.journalId ? <Link className="text-primary hover:underline" href={`/jurnal/${data.journalId}`}>Lihat jurnal</Link> : "-"} />
          </InfoGrid>
        </CardContent>
      </Card>
      {data.allocations.length > 0 && (
        <Card>
          <table className="w-full text-sm">
            <thead>
              <tr className="border-b border-border bg-surface-muted/60 text-left text-xs uppercase text-muted">
                <th className="px-4 py-2.5">Faktur</th>
                <th className="px-4 py-2.5">No. faktur pemasok</th>
                <th className="px-4 py-2.5 text-right">Dibayar</th>
              </tr>
            </thead>
            <tbody>
              {data.allocations.map((a) => (
                <tr key={a.invoiceId} className="border-b border-border">
                  <td className="px-4 py-2">
                    <Link className="font-mono text-xs text-primary hover:underline" href={`/pembelian/faktur/${a.invoiceId}`}>
                      {a.invoiceNo}
                    </Link>
                  </td>
                  <td className="px-4 py-2">{a.supplierInvoiceNo}</td>
                  <td className="num px-4 py-2">{formatAmount(a.amount)}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </Card>
      )}
      <VoidDialog
        open={voiding}
        loading={voidDoc.isPending}
        onClose={() => setVoiding(false)}
        onConfirm={(b) => voidDoc.mutate({ path: `${key}/${id}/void`, body: b }, { onSuccess: () => (setVoiding(false), refetch()) })}
      />
    </>
  );
}

export function PaymentDetail() {
  return (
    <RequirePermission permission="PURCHASE:VIEW">
      <Detail />
    </RequirePermission>
  );
}
