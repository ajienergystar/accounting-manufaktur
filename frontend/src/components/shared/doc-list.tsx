"use client";

import Link from "next/link";
import { useRouter } from "next/navigation";
import { useState, type ReactNode } from "react";
import { useQuery } from "@tanstack/react-query";
import type { ColumnDef } from "@tanstack/react-table";
import { Plus, Search } from "lucide-react";
import { PageHeader } from "./page-header";
import { DataTable } from "./data-table";
import { MasterPicker } from "./pickers";
import { buttonClasses } from "@/components/ui/button";
import { Card } from "@/components/ui/card";
import { Input } from "@/components/ui/input";
import { Select } from "@/components/ui/select";
import { useAuth } from "@/features/auth/auth-context";
import { RequirePermission } from "@/features/auth/require-permission";
import { api, errorMessage } from "@/lib/api";
import type { PageResponse } from "@/types/common";

interface DocListProps<T extends { id: number }> {
  title: string;
  description?: string;
  endpoint: string;
  detailHref: (row: T) => string;
  newHref?: string;
  newLabel?: string;
  permission: string;
  createPermission?: string;
  columns: ColumnDef<T, unknown>[];
  statusOptions?: { value: string; label: string }[];
  /** Filter pihak (pemasok/pelanggan). */
  partyResource?: "/suppliers" | "/customers";
  partyParam?: string;
  extraActions?: ReactNode;
}

/** Halaman daftar dokumen transaksi standar (cari, tanggal, status, pihak, pagination). */
export function DocList<T extends { id: number }>(props: DocListProps<T>) {
  return (
    <RequirePermission permission={props.permission}>
      <DocListInner {...props} />
    </RequirePermission>
  );
}

function DocListInner<T extends { id: number }>({
  title,
  description,
  endpoint,
  detailHref,
  newHref,
  newLabel = "Baru",
  createPermission,
  columns,
  statusOptions,
  partyResource,
  partyParam = "supplierId",
  extraActions,
}: DocListProps<T>) {
  const router = useRouter();
  const { can } = useAuth();
  const [q, setQ] = useState("");
  const [from, setFrom] = useState("");
  const [to, setTo] = useState("");
  const [status, setStatus] = useState("");
  const [party, setParty] = useState<number | null>(null);
  const [page, setPage] = useState(0);

  const { data, isLoading, error, refetch } = useQuery({
    queryKey: [endpoint, "list", { q, from, to, status, party, page }],
    queryFn: () =>
      api.get<PageResponse<T>>(endpoint, { q, from, to, status, [partyParam]: party ?? undefined, page, size: 20 }),
    placeholderData: (p) => p,
  });

  return (
    <>
      <PageHeader
        title={title}
        description={description}
        actions={
          <>
            {extraActions}
            {newHref && (!createPermission || can(createPermission)) && (
              <Link href={newHref} className={buttonClasses()}>
                <Plus className="h-4 w-4" /> {newLabel}
              </Link>
            )}
          </>
        }
      />
      <Card>
        <div className="grid gap-3 border-b border-border p-4 md:grid-cols-[1fr_repeat(4,minmax(0,170px))]">
          <div className="relative">
            <Search className="pointer-events-none absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-muted" />
            <Input className="pl-9" placeholder="Cari nomor dokumen…" value={q} onChange={(e) => (setQ(e.target.value), setPage(0))} aria-label="Cari" />
          </div>
          {partyResource ? (
            <MasterPicker
              resource={partyResource}
              value={party}
              onChange={(v) => (setParty(v), setPage(0))}
              clearable
              placeholder={partyResource === "/suppliers" ? "Semua pemasok" : "Semua pelanggan"}
              aria-label="Filter pihak"
            />
          ) : (
            <span className="hidden md:block" />
          )}
          <Input type="date" value={from} onChange={(e) => (setFrom(e.target.value), setPage(0))} aria-label="Dari tanggal" />
          <Input type="date" value={to} onChange={(e) => (setTo(e.target.value), setPage(0))} aria-label="Sampai tanggal" />
          {statusOptions ? (
            <Select value={status} onChange={(e) => (setStatus(e.target.value), setPage(0))} aria-label="Status">
              <option value="">Semua status</option>
              {statusOptions.map((s) => (
                <option key={s.value} value={s.value}>
                  {s.label}
                </option>
              ))}
            </Select>
          ) : (
            <span className="hidden md:block" />
          )}
        </div>
        <DataTable
          columns={columns}
          data={data?.content}
          loading={isLoading}
          error={error ? errorMessage(error) : null}
          onRetry={refetch}
          page={page}
          totalElements={data?.totalElements}
          totalPages={data?.totalPages}
          onPageChange={setPage}
          onRowClick={(r) => router.push(detailHref(r))}
          emptyTitle="Belum ada dokumen"
        />
      </Card>
    </>
  );
}
