"use client";

import Link from "next/link";
import { useParams } from "next/navigation";
import { useState } from "react";
import { useQuery } from "@tanstack/react-query";
import { CheckCircle2, PackageCheck, Printer, XCircle } from "lucide-react";
import { Info, InfoGrid, TotalsPanel } from "@/components/shared/info";
import { PageHeader } from "@/components/shared/page-header";
import { ErrorState } from "@/components/shared/states";
import { StatusBadge } from "@/components/shared/status-badge";
import { Button, buttonClasses } from "@/components/ui/button";
import { Card, CardContent } from "@/components/ui/card";
import { ConfirmDialog } from "@/components/ui/confirm-dialog";
import { Skeleton } from "@/components/ui/skeleton";
import { useAuth } from "@/features/auth/auth-context";
import { RequirePermission } from "@/features/auth/require-permission";
import { useDocAction } from "@/features/documents/use-doc-action";
import { PoForm } from "@/features/purchasing/po-form";
import type { PurchaseOrder } from "@/features/purchasing/types";
import { api, errorMessage } from "@/lib/api";
import { formatAmount, formatDate, formatDateTime, formatQty } from "@/lib/format";

type Action = "approve" | "cancel" | "close";
const TEXT: Record<Action, { title: string; msg: string; label: string }> = {
  approve: { title: "Setujui PO?", msg: "PO yang disetujui dapat diterima barangnya dan tidak dapat diubah lagi.", label: "Setujui" },
  cancel: { title: "Batalkan PO?", msg: "PO akan dibatalkan.", label: "Batalkan PO" },
  close: { title: "Tutup PO?", msg: "Sisa qty yang belum diterima tidak akan diterima lagi.", label: "Tutup PO" },
};

function Detail() {
  const { id } = useParams<{ id: string }>();
  const { can } = useAuth();
  const [confirm, setConfirm] = useState<Action | null>(null);
  const key = "/purchasing/orders";
  const { data, isLoading, error, refetch } = useQuery({ queryKey: [key, id], queryFn: () => api.get<PurchaseOrder>(`${key}/${id}`) });
  const act = useDocAction([key], "Status PO diperbarui");

  if (isLoading) return <Skeleton className="h-96 w-full" />;
  if (error || !data) return <ErrorState message={errorMessage(error)} onRetry={refetch} />;

  if (data.status === "DRAFT" && can("PURCHASE:EDIT")) {
    return (
      <>
        <PageHeader
          title={`PO ${data.docNo} (draft)`}
          actions={
            <>
              <Button variant="outline" onClick={() => setConfirm("cancel")}>
                <XCircle className="h-4 w-4 text-danger" /> Batalkan
              </Button>
              {can("PURCHASE:APPROVE") && (
                <Button onClick={() => setConfirm("approve")}>
                  <CheckCircle2 className="h-4 w-4" /> Setujui
                </Button>
              )}
            </>
          }
        />
        <PoForm po={data} />
        {confirm && (
          <ConfirmDialog
            open
            title={TEXT[confirm].title}
            message={TEXT[confirm].msg}
            confirmLabel={TEXT[confirm].label}
            tone={confirm === "cancel" ? "danger" : "primary"}
            loading={act.isPending}
            onClose={() => setConfirm(null)}
            onConfirm={() => act.mutate({ path: `${key}/${id}/${confirm}` }, { onSuccess: () => (setConfirm(null), refetch()) })}
          />
        )}
      </>
    );
  }

  const canReceive = ["APPROVED", "PARTIAL"].includes(data.status);
  return (
    <>
      <PageHeader
        title={`Purchase Order ${data.docNo}`}
        description={data.notes ?? undefined}
        actions={
          <>
            <Button variant="outline" onClick={() => window.print()}>
              <Printer className="h-4 w-4" /> Cetak
            </Button>
            {data.status === "DRAFT" && can("PURCHASE:APPROVE") && (
              <Button onClick={() => setConfirm("approve")}>
                <CheckCircle2 className="h-4 w-4" /> Setujui
              </Button>
            )}
            {canReceive && can("PURCHASE:APPROVE") && (
              <Button variant="outline" onClick={() => setConfirm("close")}>
                Tutup PO
              </Button>
            )}
            {canReceive && (can("PURCHASE:CREATE") || can("INVENTORY:CREATE")) && (
              <Link href={`/pembelian/penerimaan/baru?orderId=${data.id}&supplierId=${data.supplier.id}`} className={buttonClasses()}>
                <PackageCheck className="h-4 w-4" /> Terima barang
              </Link>
            )}
          </>
        }
      />
      <Card className="mb-6">
        <CardContent className="pt-5">
          <InfoGrid>
            <Info label="Pemasok" value={`${data.supplier.code} — ${data.supplier.name}`} />
            <Info label="Tanggal" value={formatDate(data.docDate)} />
            <Info label="Gudang" value={data.warehouse.name} />
            <Info label="Status" value={<StatusBadge status={data.status} />} />
            <Info label="Perkiraan tiba" value={formatDate(data.expectedDate)} />
            <Info label="Disetujui" value={data.approvedBy ? `${data.approvedBy} · ${formatDateTime(data.approvedAt)}` : "-"} />
            <Info label="Dari PR" value={data.requestNo ?? "-"} />
          </InfoGrid>
        </CardContent>
      </Card>
      <Card>
        <div className="overflow-x-auto">
          <table className="w-full min-w-[860px] text-sm">
            <thead>
              <tr className="border-b border-border bg-surface-muted/60 text-left text-xs uppercase text-muted">
                <th className="px-4 py-2.5">Item</th>
                <th className="px-4 py-2.5 text-right">Qty</th>
                <th className="px-4 py-2.5 text-right">Diterima</th>
                <th className="px-4 py-2.5 text-right">Harga</th>
                <th className="px-4 py-2.5 text-right">Disk %</th>
                <th className="px-4 py-2.5 text-right">Jumlah</th>
              </tr>
            </thead>
            <tbody>
              {data.lines.map((l) => (
                <tr key={l.id} className="border-b border-border">
                  <td className="px-4 py-2">
                    <span className="font-mono text-xs">{l.itemCode}</span> {l.itemName}
                    {l.description && <span className="block text-xs text-muted">{l.description}</span>}
                  </td>
                  <td className="num px-4 py-2">
                    {formatQty(l.qty)} {l.uomCode}
                  </td>
                  <td className="num px-4 py-2">{formatQty(l.qtyReceived)}</td>
                  <td className="num px-4 py-2">{formatAmount(l.unitPrice)}</td>
                  <td className="num px-4 py-2">{Number(l.discountPct) ? formatQty(l.discountPct) : ""}</td>
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
              { label: `PPN ${Number(data.taxRate) ? `${Number(data.taxRate)}%` : ""}`, value: formatAmount(data.taxAmount) },
              { label: "Total", value: formatAmount(data.total), strong: true },
            ]}
          />
        </div>
      </Card>
      {confirm && (
        <ConfirmDialog
          open
          title={TEXT[confirm].title}
          message={TEXT[confirm].msg}
          confirmLabel={TEXT[confirm].label}
          loading={act.isPending}
          onClose={() => setConfirm(null)}
          onConfirm={() => act.mutate({ path: `${key}/${id}/${confirm}` }, { onSuccess: () => (setConfirm(null), refetch()) })}
        />
      )}
    </>
  );
}

export function PoDetail() {
  return (
    <RequirePermission permission="PURCHASE:VIEW">
      <Detail />
    </RequirePermission>
  );
}
