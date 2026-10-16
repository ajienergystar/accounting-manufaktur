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
import { ADJ_TYPE_LABEL, type Adjustment } from "@/features/inventory/types";
import { api, errorMessage } from "@/lib/api";
import { formatAmount, formatDate, formatQty } from "@/lib/format";

function Detail() {
  const { id } = useParams<{ id: string }>();
  const { can } = useAuth();
  const [voiding, setVoiding] = useState(false);
  const key = "/inventory/adjustments";
  const { data, isLoading, error, refetch } = useQuery({ queryKey: [key, id], queryFn: () => api.get<Adjustment>(`${key}/${id}`) });
  const voidDoc = useDocAction([key, "inventory"], "Penyesuaian dibatalkan");

  if (isLoading) return <Skeleton className="h-96 w-full" />;
  if (error || !data) return <ErrorState message={errorMessage(error)} onRetry={refetch} />;

  return (
    <>
      <PageHeader
        title={`Penyesuaian ${data.docNo}`}
        description={data.reason}
        actions={
          <>
            <Button variant="outline" onClick={() => window.print()}>
              <Printer className="h-4 w-4" /> Cetak
            </Button>
            {data.status === "POSTED" && data.adjustmentType !== "OPNAME" && can("INVENTORY:POST") && (
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
            <Info label="Tanggal" value={formatDate(data.docDate)} />
            <Info label="Gudang" value={data.warehouseName} />
            <Info label="Jenis" value={ADJ_TYPE_LABEL[data.adjustmentType]} />
            <Info label="Status" value={<StatusBadge status={data.status} />} />
            <Info
              label="Jurnal"
              value={data.journalId ? <Link className="text-primary hover:underline" href={`/jurnal/${data.journalId}`}>Lihat jurnal</Link> : "-"}
            />
            <Info label="Nilai bersih" value={formatAmount(data.totalValue, { parentheses: true })} />
          </InfoGrid>
        </CardContent>
      </Card>
      <Card>
        <table className="w-full text-sm">
          <thead>
            <tr className="border-b border-border bg-surface-muted/60 text-left text-xs uppercase text-muted">
              <th className="px-4 py-2.5">Item</th>
              <th className="px-4 py-2.5 text-right">Perubahan Qty</th>
              <th className="px-4 py-2.5 text-right">Biaya/Unit</th>
              <th className="px-4 py-2.5 text-right">Nilai</th>
              <th className="px-4 py-2.5">Catatan</th>
            </tr>
          </thead>
          <tbody>
            {data.lines.map((l) => (
              <tr key={l.itemId} className="border-b border-border">
                <td className="px-4 py-2">
                  <span className="font-mono text-xs">{l.itemCode}</span> {l.itemName}
                </td>
                <td className={`num px-4 py-2 ${Number(l.qtyChange) < 0 ? "text-danger" : "text-success"}`}>
                  {Number(l.qtyChange) > 0 ? "+" : ""}
                  {formatQty(l.qtyChange)} {l.uomCode}
                </td>
                <td className="num px-4 py-2">{formatAmount(l.unitCost)}</td>
                <td className="num px-4 py-2">{formatAmount(l.totalValue, { parentheses: true })}</td>
                <td className="px-4 py-2 text-muted">{l.note}</td>
              </tr>
            ))}
          </tbody>
        </table>
      </Card>
      <VoidDialog
        open={voiding}
        loading={voidDoc.isPending}
        onClose={() => setVoiding(false)}
        onConfirm={(b) => voidDoc.mutate({ path: `${key}/${id}/void`, body: b }, { onSuccess: () => (setVoiding(false), refetch()) })}
      />
    </>
  );
}

export function AdjustmentDetail() {
  return (
    <RequirePermission permission="INVENTORY:VIEW">
      <Detail />
    </RequirePermission>
  );
}
