"use client";

import { useSearchParams } from "next/navigation";
import { useQuery } from "@tanstack/react-query";
import { PageHeader } from "@/components/shared/page-header";
import { Skeleton } from "@/components/ui/skeleton";
import { RequirePermission } from "@/features/auth/require-permission";
import { PoForm } from "@/features/purchasing/po-form";
import type { PurchaseRequest } from "@/features/purchasing/types";
import { api } from "@/lib/api";

export function NewPo() {
  const requestId = useSearchParams().get("requestId");
  const pr = useQuery({
    queryKey: ["/purchasing/requests", requestId],
    queryFn: () => api.get<PurchaseRequest>(`/purchasing/requests/${requestId}`),
    enabled: !!requestId,
  });
  return (
    <RequirePermission permission="PURCHASE:CREATE">
      <PageHeader
        title="Purchase Order Baru"
        description={pr.data ? `Dari permintaan pembelian ${pr.data.docNo}` : "PO disimpan sebagai draft lalu disetujui oleh yang berwenang."}
      />
      {requestId && pr.isLoading ? <Skeleton className="h-96 w-full" /> : <PoForm fromRequest={pr.data} />}
    </RequirePermission>
  );
}
