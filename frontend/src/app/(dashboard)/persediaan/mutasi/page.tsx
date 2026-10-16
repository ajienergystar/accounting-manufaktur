"use client";

import Link from "next/link";
import { useRouter } from "next/navigation";
import { useMemo, useState } from "react";
import { useQuery } from "@tanstack/react-query";
import type { ColumnDef } from "@tanstack/react-table";
import { ArrowRight, Plus } from "lucide-react";
import { DataTable } from "@/components/shared/data-table";
import { PageHeader } from "@/components/shared/page-header";
import { StatusBadge } from "@/components/shared/status-badge";
import { Badge } from "@/components/ui/badge";
import { buttonClasses } from "@/components/ui/button";
import { Card } from "@/components/ui/card";
import { useAuth } from "@/features/auth/auth-context";
import { RequirePermission } from "@/features/auth/require-permission";
import { ADJ_TYPE_LABEL, type Adjustment, type Transfer } from "@/features/inventory/types";
import { api, errorMessage } from "@/lib/api";
import { formatAmount, formatDate } from "@/lib/format";
import { cn } from "@/lib/utils";
import type { PageResponse } from "@/types/common";

function Mutations() {
  const router = useRouter();
  const { can } = useAuth();
  const [tab, setTab] = useState<"adj" | "trf">("adj");
  const [page, setPage] = useState(0);
  const endpoint = tab === "adj" ? "/inventory/adjustments" : "/inventory/transfers";
  const { data, isLoading, error, refetch } = useQuery({
    queryKey: [endpoint, "list", page],
    queryFn: () => api.get<PageResponse<Adjustment | Transfer>>(endpoint, { page, size: 20 }),
  });

  const adjCols = useMemo<ColumnDef<Adjustment, unknown>[]>(
    () => [
      { header: "Tanggal", cell: ({ row }) => formatDate(row.original.docDate) },
      { header: "No.", cell: ({ row }) => <span className="font-mono text-xs">{row.original.docNo}</span> },
      { header: "Jenis", cell: ({ row }) => <Badge tone="info">{ADJ_TYPE_LABEL[row.original.adjustmentType]}</Badge> },
      { accessorKey: "warehouseName", header: "Gudang" },
      { accessorKey: "reason", header: "Alasan" },
      { header: "Nilai bersih", meta: { align: "right" }, cell: ({ row }) => formatAmount(row.original.totalValue, { parentheses: true }) },
      { header: "Status", cell: ({ row }) => <StatusBadge status={row.original.status} /> },
    ],
    [],
  );
  const trfCols = useMemo<ColumnDef<Transfer, unknown>[]>(
    () => [
      { header: "Tanggal", cell: ({ row }) => formatDate(row.original.docDate) },
      { header: "No.", cell: ({ row }) => <span className="font-mono text-xs">{row.original.docNo}</span> },
      {
        header: "Rute",
        cell: ({ row }) => (
          <span className="flex items-center gap-1.5">
            {row.original.fromWarehouseName} <ArrowRight className="h-3 w-3 text-muted" /> {row.original.toWarehouseName}
          </span>
        ),
      },
      { header: "Nilai", meta: { align: "right" }, cell: ({ row }) => formatAmount(row.original.totalValue) },
      { header: "Status", cell: ({ row }) => <StatusBadge status={row.original.status} /> },
    ],
    [],
  );

  return (
    <>
      <PageHeader
        title="Penyesuaian & Transfer Stok"
        description="Penyesuaian (termasuk saldo awal persediaan) dan pemindahan barang antar gudang."
        actions={
          can("INVENTORY:CREATE") && (
            <>
              <Link href="/persediaan/penyesuaian/baru" className={buttonClasses("outline")}>
                <Plus className="h-4 w-4" /> Penyesuaian
              </Link>
              <Link href="/persediaan/transfer/baru" className={buttonClasses()}>
                <Plus className="h-4 w-4" /> Transfer
              </Link>
            </>
          )
        }
      />
      <div role="tablist" className="mb-4 flex gap-1 border-b border-border">
        {(
          [
            ["adj", "Penyesuaian"],
            ["trf", "Transfer gudang"],
          ] as const
        ).map(([k, l]) => (
          <button
            key={k}
            role="tab"
            aria-selected={tab === k}
            onClick={() => (setTab(k), setPage(0))}
            className={cn(
              "-mb-px border-b-2 px-4 py-2 text-sm font-medium",
              tab === k ? "border-primary text-primary" : "border-transparent text-muted hover:text-foreground",
            )}
          >
            {l}
          </button>
        ))}
      </div>
      <Card>
        {tab === "adj" ? (
          <DataTable
            columns={adjCols}
            data={data?.content as Adjustment[] | undefined}
            loading={isLoading}
            error={error ? errorMessage(error) : null}
            onRetry={refetch}
            page={page}
            totalElements={data?.totalElements}
            totalPages={data?.totalPages}
            onPageChange={setPage}
            onRowClick={(r) => router.push(`/persediaan/penyesuaian/${r.id}`)}
          />
        ) : (
          <DataTable
            columns={trfCols}
            data={data?.content as Transfer[] | undefined}
            loading={isLoading}
            error={error ? errorMessage(error) : null}
            onRetry={refetch}
            page={page}
            totalElements={data?.totalElements}
            totalPages={data?.totalPages}
            onPageChange={setPage}
            onRowClick={(r) => router.push(`/persediaan/transfer/${r.id}`)}
          />
        )}
      </Card>
    </>
  );
}

export default function Page() {
  return (
    <RequirePermission permission="INVENTORY:VIEW">
      <Mutations />
    </RequirePermission>
  );
}
