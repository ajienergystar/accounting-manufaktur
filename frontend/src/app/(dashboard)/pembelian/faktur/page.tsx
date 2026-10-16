"use client";

import type { ColumnDef } from "@tanstack/react-table";
import { DocList } from "@/components/shared/doc-list";
import { StatusBadge } from "@/components/shared/status-badge";
import { Badge } from "@/components/ui/badge";
import type { PurchaseInvoice } from "@/features/purchasing/types";
import { formatDate, formatRupiah } from "@/lib/format";

const columns: ColumnDef<PurchaseInvoice, unknown>[] = [
  { header: "Tanggal", cell: ({ row }) => formatDate(row.original.docDate) },
  {
    header: "No. Faktur",
    cell: ({ row }) => (
      <div>
        <span className="font-mono text-xs">{row.original.docNo}</span>
        <span className="block text-xs text-muted">{row.original.supplierInvoiceNo}</span>
      </div>
    ),
  },
  { header: "Pemasok", cell: ({ row }) => row.original.supplier.name },
  {
    header: "Jatuh tempo",
    cell: ({ row }) => (
      <span className="flex items-center gap-1.5">
        {formatDate(row.original.dueDate)}
        {row.original.daysOverdue > 0 && <Badge tone="danger">{row.original.daysOverdue} hr</Badge>}
      </span>
    ),
  },
  { header: "Total", meta: { align: "right" }, cell: ({ row }) => formatRupiah(row.original.total) },
  { header: "Sisa utang", meta: { align: "right" }, cell: ({ row }) => formatRupiah(row.original.outstanding) },
  { header: "Status", cell: ({ row }) => <StatusBadge status={row.original.status} /> },
];

export default function Page() {
  return (
    <DocList<PurchaseInvoice>
      title="Faktur Pembelian"
      description="Tagihan pemasok dengan three-way matching (PO ↔ penerimaan ↔ faktur)."
      endpoint="/purchasing/invoices"
      detailHref={(r) => `/pembelian/faktur/${r.id}`}
      newHref="/pembelian/faktur/baru"
      newLabel="Faktur baru"
      permission="PURCHASE:VIEW"
      createPermission="PURCHASE:CREATE"
      columns={columns}
      partyResource="/suppliers"
      statusOptions={[
        { value: "UNPAID", label: "Belum dibayar" },
        { value: "PARTIAL", label: "Sebagian" },
        { value: "PAID", label: "Lunas" },
        { value: "VOID", label: "Batal" },
      ]}
    />
  );
}
