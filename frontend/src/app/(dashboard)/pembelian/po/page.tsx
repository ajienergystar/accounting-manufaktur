"use client";

import type { ColumnDef } from "@tanstack/react-table";
import { DocList } from "@/components/shared/doc-list";
import { StatusBadge } from "@/components/shared/status-badge";
import type { PurchaseOrder } from "@/features/purchasing/types";
import { formatDate, formatRupiah } from "@/lib/format";

const columns: ColumnDef<PurchaseOrder, unknown>[] = [
  { header: "Tanggal", cell: ({ row }) => formatDate(row.original.docDate) },
  { header: "No. PO", cell: ({ row }) => <span className="font-mono text-xs">{row.original.docNo}</span> },
  { header: "Pemasok", cell: ({ row }) => row.original.supplier.name },
  { header: "Gudang", cell: ({ row }) => row.original.warehouse.name },
  { header: "Perkiraan tiba", cell: ({ row }) => formatDate(row.original.expectedDate) },
  { header: "Total", meta: { align: "right" }, cell: ({ row }) => formatRupiah(row.original.total) },
  { header: "Status", cell: ({ row }) => <StatusBadge status={row.original.status} /> },
];

export default function Page() {
  return (
    <DocList<PurchaseOrder>
      title="Purchase Order"
      description="Pesanan pembelian ke pemasok. PO harus disetujui sebelum barang diterima."
      endpoint="/purchasing/orders"
      detailHref={(r) => `/pembelian/po/${r.id}`}
      newHref="/pembelian/po/baru"
      newLabel="PO baru"
      permission="PURCHASE:VIEW"
      createPermission="PURCHASE:CREATE"
      columns={columns}
      partyResource="/suppliers"
      statusOptions={["DRAFT", "APPROVED", "PARTIAL", "RECEIVED", "CLOSED", "CANCELLED"].map((s) => ({ value: s, label: s }))}
    />
  );
}
