"use client";

import Link from "next/link";
import { useParams } from "next/navigation";
import { useState } from "react";
import { useQuery } from "@tanstack/react-query";
import { Ban, FileText, Printer, Undo2 } from "lucide-react";
import { Attachments } from "@/components/shared/attachments";
import { Info, InfoGrid } from "@/components/shared/info";
import { PageHeader } from "@/components/shared/page-header";
import { ErrorState } from "@/components/shared/states";
import { StatusBadge } from "@/components/shared/status-badge";
import { VoidDialog } from "@/components/shared/void-dialog";
import { Button, buttonClasses } from "@/components/ui/button";
import { Card, CardContent } from "@/components/ui/card";
import { Skeleton } from "@/components/ui/skeleton";
import { useAuth } from "@/features/auth/auth-context";
import { RequirePermission } from "@/features/auth/require-permission";
import { useDocAction } from "@/features/documents/use-doc-action";
import type { GoodsReceipt } from "@/features/purchasing/types";
import { api, errorMessage } from "@/lib/api";
import { formatAmount, formatDate, formatQty } from "@/lib/format";

function Detail() {
  const { id } = useParams<{ id: string }>();
  const { can } = useAuth();
  const [voiding, setVoiding] = useState(false);
  const key = "/purchasing/receipts";
  const { data, isLoading, error, refetch } = useQuery({ queryKey: [key, id], queryFn: () => api.get<GoodsReceipt>(`${key}/${id}`) });
  const voidDoc = useDocAction([key, "/purchasing/orders", "inventory"], "Penerimaan dibatalkan");

  if (isLoading) return <Skeleton className="h-96 w-full" />;
  if (error || !data) return <ErrorState message={errorMessage(error)} onRetry={refetch} />;

  const posted = data.status === "POSTED";
  const hasOpen = data.lines.some((l) => Number(l.qty) - Number(l.qtyInvoiced) - Number(l.qtyReturned) > 0);
  return (
    <>
      <PageHeader
        title={`Penerimaan ${data.docNo}`}
        description={data.notes ?? undefined}
        actions={
          <>
            <Button variant="outline" onClick={() => window.print()}>
              <Printer className="h-4 w-4" /> Cetak
            </Button>
            {posted && hasOpen && can("PURCHASE:CREATE") && (
              <>
                <Link href={`/pembelian/retur/baru?receiptId=${data.id}`} className={buttonClasses("outline")}>
                  <Undo2 className="h-4 w-4" /> Retur
                </Link>
                <Link href={`/pembelian/faktur/baru?supplierId=${data.supplier.id}`} className={buttonClasses()}>
                  <FileText className="h-4 w-4" /> Buat faktur
                </Link>
              </>
            )}
            {posted && can("PURCHASE:APPROVE") && (
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
            <Info label="Gudang" value={data.warehouse.name} />
            <Info label="Status" value={<StatusBadge status={data.status} />} />
            <Info label="PO" value={data.orderId ? <Link className="text-primary hover:underline" href={`/pembelian/po/${data.orderId}`}>{data.orderNo}</Link> : "Tanpa PO"} />
            <Info label="Surat jalan" value={data.deliveryNoteNo ?? "-"} />
            <Info label="Ongkos angkut" value={formatAmount(data.freightCost)} />
            <Info label="Jurnal" value={data.journalId ? <Link className="text-primary hover:underline" href={`/jurnal/${data.journalId}`}>Lihat jurnal</Link> : "-"} />
          </InfoGrid>
        </CardContent>
      </Card>
      <Card className="mb-6">
        <div className="overflow-x-auto">
          <table className="w-full min-w-[900px] text-sm">
            <thead>
              <tr className="border-b border-border bg-surface-muted/60 text-left text-xs uppercase text-muted">
                <th className="px-4 py-2.5">Item</th>
                <th className="px-4 py-2.5 text-right">Qty</th>
                <th className="px-4 py-2.5 text-right">Harga</th>
                <th className="px-4 py-2.5 text-right">Ongkir</th>
                <th className="px-4 py-2.5 text-right">Biaya/Unit</th>
                <th className="px-4 py-2.5 text-right">Nilai</th>
                <th className="px-4 py-2.5 text-right">Difakturkan</th>
                <th className="px-4 py-2.5 text-right">Diretur</th>
              </tr>
            </thead>
            <tbody>
              {data.lines.map((l) => (
                <tr key={l.id} className="border-b border-border">
                  <td className="px-4 py-2">
                    <span className="font-mono text-xs">{l.itemCode}</span> {l.itemName}
                  </td>
                  <td className="num px-4 py-2">
                    {formatQty(l.qty)} {l.uomCode}
                  </td>
                  <td className="num px-4 py-2">{formatAmount(l.unitPrice)}</td>
                  <td className="num px-4 py-2">{formatAmount(l.freightAlloc)}</td>
                  <td className="num px-4 py-2">{formatAmount(l.unitCost)}</td>
                  <td className="num px-4 py-2">{formatAmount(l.totalCost)}</td>
                  <td className="num px-4 py-2">{formatQty(l.qtyInvoiced)}</td>
                  <td className="num px-4 py-2">{formatQty(l.qtyReturned)}</td>
                </tr>
              ))}
            </tbody>
            <tfoot>
              <tr className="font-semibold">
                <td colSpan={5} className="px-4 py-3 text-right">
                  Total
                </td>
                <td className="num px-4 py-3">{formatAmount(data.totalValue)}</td>
                <td colSpan={2} />
              </tr>
            </tfoot>
          </table>
        </div>
      </Card>
      <Attachments entityType="GOODS_RECEIPT" entityId={data.id} canEdit={can("PURCHASE:EDIT")} />
      <VoidDialog
        open={voiding}
        loading={voidDoc.isPending}
        onClose={() => setVoiding(false)}
        onConfirm={(b) => voidDoc.mutate({ path: `${key}/${id}/void`, body: b }, { onSuccess: () => (setVoiding(false), refetch()) })}
      />
    </>
  );
}

export function GrnDetail() {
  return (
    <RequirePermission permission="PURCHASE:VIEW">
      <Detail />
    </RequirePermission>
  );
}
