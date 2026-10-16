"use client";

import type { ColumnDef } from "@tanstack/react-table";
import { DocList } from "@/components/shared/doc-list";
import { StatusBadge } from "@/components/shared/status-badge";
import type { PurchaseRequest } from "@/features/purchasing/types";
import { formatDate } from "@/lib/format";

const columns: ColumnDef<PurchaseRequest, unknown>[] = [
  { header: "Tanggal", cell: ({ row }) => formatDate(row.original.docDate) },
  { header: "No. PR", cell: ({ row }) => <span className="font-mono text-xs">{row.original.docNo}</span> },
  { header: "Departemen", cell: ({ row }) => row.original.department?.name ?? "-" },
  { accessorKey: "requestedBy", header: "Diminta oleh" },
  { header: "Dibutuhkan", cell: ({ row }) => formatDate(row.original.neededDate) },
  { header: "Status", cell: ({ row }) => <StatusBadge status={row.original.status} /> },
];

export default function Page() {
  return (
    <DocList<PurchaseRequest>
      title="Permintaan Pembelian"
      description="Purchase request dari departemen, disetujui sebelum dibuatkan PO."
      endpoint="/purchasing/requests"
      detailHref={(r) => `/pembelian/pr/${r.id}`}
      newHref="/pembelian/pr/baru"
      newLabel="PR baru"
      permission="PURCHASE:VIEW"
      createPermission="PURCHASE:CREATE"
      columns={columns}
      statusOptions={["DRAFT", "SUBMITTED", "APPROVED", "REJECTED", "ORDERED", "CANCELLED"].map((s) => ({ value: s, label: s }))}
    />
  );
}
