"use client";

import { useParams } from "next/navigation";
import { useState } from "react";
import { useQuery } from "@tanstack/react-query";
import { Ban } from "lucide-react";
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
import type { Transfer } from "@/features/inventory/types";
import { api, errorMessage } from "@/lib/api";
import { formatAmount, formatDate, formatQty } from "@/lib/format";

function Detail() {
  const { id } = useParams<{ id: string }>();
  const { can } = useAuth();
  const [voiding, setVoiding] = useState(false);
  const key = "/inventory/transfers";
  const { data, isLoading, error, refetch } = useQuery({ queryKey: [key, id], queryFn: () => api.get<Transfer>(`${key}/${id}`) });
  const voidDoc = useDocAction([key, "inventory"], "Transfer dibatalkan");

  if (isLoading) return <Skeleton className="h-96 w-full" />;
  if (error || !data) return <ErrorState message={errorMessage(error)} onRetry={refetch} />;

  return (
    <>
      <PageHeader
        title={`Transfer ${data.docNo}`}
        description={data.notes ?? undefined}
        actions={
          data.status === "POSTED" &&
          can("INVENTORY:POST") && (
            <Button variant="outline" onClick={() => setVoiding(true)}>
              <Ban className="h-4 w-4 text-danger" /> Batalkan
            </Button>
          )
        }
      />
      <Card className="mb-6">
        <CardContent className="pt-5">
          <InfoGrid>
            <Info label="Tanggal" value={formatDate(data.docDate)} />
            <Info label="Dari" value={data.fromWarehouseName} />
            <Info label="Ke" value={data.toWarehouseName} />
            <Info label="Status" value={<StatusBadge status={data.status} />} />
          </InfoGrid>
        </CardContent>
      </Card>
      <Card>
        <table className="w-full text-sm">
          <thead>
            <tr className="border-b border-border bg-surface-muted/60 text-left text-xs uppercase text-muted">
              <th className="px-4 py-2.5">Item</th>
              <th className="px-4 py-2.5 text-right">Qty</th>
              <th className="px-4 py-2.5 text-right">Biaya/Unit</th>
              <th className="px-4 py-2.5 text-right">Nilai</th>
            </tr>
          </thead>
          <tbody>
            {data.lines.map((l) => (
              <tr key={l.itemId} className="border-b border-border">
                <td className="px-4 py-2">
                  <span className="font-mono text-xs">{l.itemCode}</span> {l.itemName}
                </td>
                <td className="num px-4 py-2">
                  {formatQty(l.qty)} {l.uomCode}
                </td>
                <td className="num px-4 py-2">{formatAmount(l.unitCost)}</td>
                <td className="num px-4 py-2">{formatAmount(l.totalValue)}</td>
              </tr>
            ))}
          </tbody>
          <tfoot>
            <tr className="font-semibold">
              <td colSpan={3} className="px-4 py-3 text-right">
                Total
              </td>
              <td className="num px-4 py-3">{formatAmount(data.totalValue)}</td>
            </tr>
          </tfoot>
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

export function TransferDetail() {
  return (
    <RequirePermission permission="INVENTORY:VIEW">
      <Detail />
    </RequirePermission>
  );
}
