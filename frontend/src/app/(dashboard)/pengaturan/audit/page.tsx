"use client";

import { useMemo, useState } from "react";
import { useQuery } from "@tanstack/react-query";
import type { ColumnDef } from "@tanstack/react-table";
import { PageHeader } from "@/components/shared/page-header";
import { DataTable } from "@/components/shared/data-table";
import { Badge } from "@/components/ui/badge";
import { Card } from "@/components/ui/card";
import { Dialog } from "@/components/ui/dialog";
import { Input } from "@/components/ui/input";
import { RequirePermission } from "@/features/auth/require-permission";
import { api, errorMessage } from "@/lib/api";
import { formatDateTime } from "@/lib/format";
import type { PageResponse } from "@/types/common";

interface AuditLog {
  id: number;
  occurredAt: string;
  username: string;
  action: string;
  entityType: string;
  entityId: string | null;
  description: string | null;
  beforeData: string | null;
  afterData: string | null;
  ipAddress: string | null;
}

const ACTION_TONE: Record<string, "success" | "info" | "danger" | "warning" | "neutral" | "primary"> = {
  CREATE: "success",
  UPDATE: "info",
  DELETE: "danger",
  POST: "primary",
  VOID: "danger",
  REVERSE: "warning",
  LOGIN_FAILED: "danger",
};

function pretty(json: string | null) {
  if (!json) return "-";
  try {
    return JSON.stringify(JSON.parse(json), null, 2);
  } catch {
    return json;
  }
}

function AuditTrail() {
  const [username, setUsername] = useState("");
  const [entityType, setEntityType] = useState("");
  const [from, setFrom] = useState("");
  const [to, setTo] = useState("");
  const [page, setPage] = useState(0);
  const [detail, setDetail] = useState<AuditLog | null>(null);

  const { data, isLoading, error, refetch } = useQuery({
    queryKey: ["audit", { username, entityType, from, to, page }],
    queryFn: () => api.get<PageResponse<AuditLog>>("/audit-logs", { username, entityType, from, to, page, size: 50 }),
    placeholderData: (p) => p,
  });

  const columns = useMemo<ColumnDef<AuditLog, unknown>[]>(
    () => [
      { header: "Waktu", cell: ({ row }) => <span className="whitespace-nowrap">{formatDateTime(row.original.occurredAt)}</span> },
      { accessorKey: "username", header: "User" },
      {
        header: "Aksi",
        cell: ({ row }) => <Badge tone={ACTION_TONE[row.original.action] ?? "neutral"}>{row.original.action}</Badge>,
      },
      {
        header: "Objek",
        cell: ({ row }) => (
          <span>
            {row.original.entityType}
            {row.original.entityId && <span className="text-muted"> #{row.original.entityId}</span>}
          </span>
        ),
      },
      { accessorKey: "description", header: "Keterangan" },
      { accessorKey: "ipAddress", header: "IP" },
    ],
    [],
  );

  return (
    <>
      <Card>
        <div className="grid gap-3 border-b border-border p-4 sm:grid-cols-4">
          <Input placeholder="Username" value={username} onChange={(e) => (setUsername(e.target.value), setPage(0))} aria-label="Filter username" />
          <Input placeholder="Objek (mis. Akun, User)" value={entityType} onChange={(e) => (setEntityType(e.target.value), setPage(0))} aria-label="Filter objek" />
          <Input type="date" value={from} onChange={(e) => (setFrom(e.target.value), setPage(0))} aria-label="Dari tanggal" />
          <Input type="date" value={to} onChange={(e) => (setTo(e.target.value), setPage(0))} aria-label="Sampai tanggal" />
        </div>
        <DataTable
          columns={columns}
          data={data?.content}
          loading={isLoading}
          error={error ? errorMessage(error) : null}
          onRetry={refetch}
          page={page}
          pageSize={50}
          totalElements={data?.totalElements}
          totalPages={data?.totalPages}
          onPageChange={setPage}
          onRowClick={setDetail}
          emptyTitle="Tidak ada catatan audit"
        />
      </Card>
      <Dialog open={detail !== null} onClose={() => setDetail(null)} title="Detail audit" size="lg">
        {detail && (
          <div className="space-y-3 text-sm">
            <p>
              <b>{detail.username}</b> · {detail.action} · {detail.entityType} #{detail.entityId} · {formatDateTime(detail.occurredAt)}
            </p>
            <p className="text-muted">{detail.description}</p>
            <div className="grid gap-3 md:grid-cols-2">
              <div>
                <p className="mb-1 text-xs font-semibold uppercase text-muted">Sebelum</p>
                <pre className="max-h-80 overflow-auto rounded-lg bg-surface-muted p-3 text-xs">{pretty(detail.beforeData)}</pre>
              </div>
              <div>
                <p className="mb-1 text-xs font-semibold uppercase text-muted">Sesudah</p>
                <pre className="max-h-80 overflow-auto rounded-lg bg-surface-muted p-3 text-xs">{pretty(detail.afterData)}</pre>
              </div>
            </div>
          </div>
        )}
      </Dialog>
    </>
  );
}

export default function Page() {
  return (
    <RequirePermission permission="AUDIT:VIEW">
      <PageHeader title="Audit Trail" description="Jejak semua perubahan data penting: siapa, kapan, dan apa yang diubah." />
      <AuditTrail />
    </RequirePermission>
  );
}
