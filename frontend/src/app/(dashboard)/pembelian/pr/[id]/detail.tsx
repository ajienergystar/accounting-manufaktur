"use client";

import Link from "next/link";
import { useParams } from "next/navigation";
import { useQuery } from "@tanstack/react-query";
import { CheckCircle2, FilePlus2, Send, XCircle } from "lucide-react";
import { Info, InfoGrid } from "@/components/shared/info";
import { PageHeader } from "@/components/shared/page-header";
import { ErrorState } from "@/components/shared/states";
import { StatusBadge } from "@/components/shared/status-badge";
import { Button, buttonClasses } from "@/components/ui/button";
import { Card, CardContent } from "@/components/ui/card";
import { Skeleton } from "@/components/ui/skeleton";
import { useAuth } from "@/features/auth/auth-context";
import { RequirePermission } from "@/features/auth/require-permission";
import { useDocAction } from "@/features/documents/use-doc-action";
import { PrForm } from "@/features/purchasing/pr-form";
import type { PurchaseRequest } from "@/features/purchasing/types";
import { api, errorMessage } from "@/lib/api";
import { formatDate, formatDateTime, formatQty } from "@/lib/format";

function Detail() {
  const { id } = useParams<{ id: string }>();
  const { can } = useAuth();
  const key = "/purchasing/requests";
  const { data, isLoading, error, refetch } = useQuery({ queryKey: [key, id], queryFn: () => api.get<PurchaseRequest>(`${key}/${id}`) });
  const act = useDocAction([key], "Status PR diperbarui");
  const run = (a: string) => act.mutate({ path: `${key}/${id}/${a}` }, { onSuccess: () => refetch() });

  if (isLoading) return <Skeleton className="h-96 w-full" />;
  if (error || !data) return <ErrorState message={errorMessage(error)} onRetry={refetch} />;

  const actions = (
    <>
      {data.status === "DRAFT" && can("PURCHASE:CREATE") && (
        <Button onClick={() => run("submit")} loading={act.isPending}>
          <Send className="h-4 w-4" /> Ajukan
        </Button>
      )}
      {data.status === "SUBMITTED" && can("PURCHASE:APPROVE") && (
        <>
          <Button variant="outline" onClick={() => run("reject")}>
            <XCircle className="h-4 w-4 text-danger" /> Tolak
          </Button>
          <Button onClick={() => run("approve")}>
            <CheckCircle2 className="h-4 w-4" /> Setujui
          </Button>
        </>
      )}
      {data.status === "APPROVED" && can("PURCHASE:CREATE") && (
        <Link href={`/pembelian/po/baru?requestId=${data.id}`} className={buttonClasses()}>
          <FilePlus2 className="h-4 w-4" /> Buat PO
        </Link>
      )}
      {["DRAFT", "SUBMITTED", "APPROVED"].includes(data.status) && can("PURCHASE:EDIT") && (
        <Button variant="ghost" onClick={() => run("cancel")}>
          Batalkan PR
        </Button>
      )}
    </>
  );

  return (
    <>
      <PageHeader title={`Permintaan Pembelian ${data.docNo}`} description={data.notes ?? undefined} actions={actions} />
      {data.status === "DRAFT" && can("PURCHASE:EDIT") ? (
        <PrForm pr={data} />
      ) : (
        <>
          <Card className="mb-6">
            <CardContent className="pt-5">
              <InfoGrid>
                <Info label="Tanggal" value={formatDate(data.docDate)} />
                <Info label="Departemen" value={data.department?.name ?? "-"} />
                <Info label="Diminta oleh" value={data.requestedBy} />
                <Info label="Status" value={<StatusBadge status={data.status} />} />
                <Info label="Dibutuhkan" value={formatDate(data.neededDate)} />
                <Info label="Disetujui/ditolak" value={data.approvedBy ? `${data.approvedBy} · ${formatDateTime(data.approvedAt)}` : "-"} />
              </InfoGrid>
            </CardContent>
          </Card>
          <Card>
            <table className="w-full text-sm">
              <thead>
                <tr className="border-b border-border bg-surface-muted/60 text-left text-xs uppercase text-muted">
                  <th className="px-4 py-2.5">Item</th>
                  <th className="px-4 py-2.5 text-right">Qty</th>
                  <th className="px-4 py-2.5">Catatan</th>
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
                    <td className="px-4 py-2 text-muted">{l.note}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </Card>
        </>
      )}
    </>
  );
}

export function PrDetail() {
  return (
    <RequirePermission permission="PURCHASE:VIEW">
      <Detail />
    </RequirePermission>
  );
}
