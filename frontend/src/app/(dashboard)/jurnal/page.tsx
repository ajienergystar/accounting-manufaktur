"use client";

import Link from "next/link";
import { useRouter } from "next/navigation";
import { useMemo, useState } from "react";
import { useQuery } from "@tanstack/react-query";
import type { ColumnDef } from "@tanstack/react-table";
import { Plus, Search } from "lucide-react";
import { PageHeader } from "@/components/shared/page-header";
import { DataTable } from "@/components/shared/data-table";
import { Badge } from "@/components/ui/badge";
import { buttonClasses } from "@/components/ui/button";
import { Card } from "@/components/ui/card";
import { Input } from "@/components/ui/input";
import { Select } from "@/components/ui/select";
import { Can, RequirePermission } from "@/features/auth/require-permission";
import { JOURNAL_TYPE_LABEL, STATUS_META, type JournalSummary } from "@/features/ledger/types";
import { api, errorMessage } from "@/lib/api";
import { formatAmount, formatDate } from "@/lib/format";
import type { PageResponse } from "@/types/common";

function firstDayOfMonth() {
  const d = new Date();
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, "0")}-01`;
}

function JournalList() {
  const router = useRouter();
  const [q, setQ] = useState("");
  const [from, setFrom] = useState(firstDayOfMonth());
  const [to, setTo] = useState("");
  const [status, setStatus] = useState("");
  const [type, setType] = useState("");
  const [page, setPage] = useState(0);

  const { data, isLoading, error, refetch } = useQuery({
    queryKey: ["journals", { q, from, to, status, type, page }],
    queryFn: () => api.get<PageResponse<JournalSummary>>("/journals", { q, from, to, status, type, page, size: 20 }),
    placeholderData: (p) => p,
  });

  const columns = useMemo<ColumnDef<JournalSummary, unknown>[]>(
    () => [
      { header: "Tanggal", cell: ({ row }) => <span className="whitespace-nowrap">{formatDate(row.original.journalDate)}</span> },
      { header: "No. Jurnal", cell: ({ row }) => <span className="font-mono text-xs">{row.original.journalNo}</span> },
      {
        header: "Keterangan",
        cell: ({ row }) => (
          <div className="max-w-md">
            <p className="truncate">{row.original.description}</p>
            <p className="text-xs text-muted">
              {row.original.sourceTypeLabel}
              {row.original.sourceNo && ` · ${row.original.sourceNo}`}
            </p>
          </div>
        ),
      },
      { header: "Tipe", cell: ({ row }) => <span className="text-muted">{row.original.journalTypeLabel}</span> },
      { header: "Nilai", meta: { align: "right" }, cell: ({ row }) => formatAmount(row.original.totalDebit) },
      {
        header: "Status",
        cell: ({ row }) => (
          <div className="flex gap-1">
            <Badge tone={STATUS_META[row.original.status].tone}>{STATUS_META[row.original.status].label}</Badge>
            {row.original.reversed && <Badge tone="warning">Dibalik</Badge>}
          </div>
        ),
      },
    ],
    [],
  );

  return (
    <>
      <PageHeader
        title="Jurnal Umum"
        description="Semua jurnal manual dan otomatis dari setiap modul."
        actions={
          <Can permission="LEDGER:CREATE">
            <Link href="/jurnal/baru" className={buttonClasses()}>
              <Plus className="h-4 w-4" /> Jurnal baru
            </Link>
          </Can>
        }
      />
      <Card>
        <div className="grid gap-3 border-b border-border p-4 md:grid-cols-[1fr_150px_150px_150px_170px]">
          <div className="relative">
            <Search className="pointer-events-none absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-muted" />
            <Input
              className="pl-9"
              placeholder="No. jurnal, keterangan, no. dokumen…"
              value={q}
              onChange={(e) => (setQ(e.target.value), setPage(0))}
              aria-label="Cari jurnal"
            />
          </div>
          <Input type="date" value={from} onChange={(e) => (setFrom(e.target.value), setPage(0))} aria-label="Dari tanggal" />
          <Input type="date" value={to} onChange={(e) => (setTo(e.target.value), setPage(0))} aria-label="Sampai tanggal" />
          <Select value={status} onChange={(e) => (setStatus(e.target.value), setPage(0))} aria-label="Status">
            <option value="">Semua status</option>
            <option value="DRAFT">Draft</option>
            <option value="POSTED">Posted</option>
          </Select>
          <Select value={type} onChange={(e) => (setType(e.target.value), setPage(0))} aria-label="Tipe">
            <option value="">Semua tipe</option>
            {Object.entries(JOURNAL_TYPE_LABEL).map(([v, l]) => (
              <option key={v} value={v}>
                {l}
              </option>
            ))}
          </Select>
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
          onRowClick={(j) => router.push(`/jurnal/${j.id}`)}
          emptyTitle="Belum ada jurnal pada rentang ini"
        />
      </Card>
    </>
  );
}

export default function Page() {
  return (
    <RequirePermission permission="LEDGER:VIEW">
      <JournalList />
    </RequirePermission>
  );
}
