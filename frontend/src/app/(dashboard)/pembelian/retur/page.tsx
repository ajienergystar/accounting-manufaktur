"use client";

import type { ColumnDef } from "@tanstack/react-table";
import { DocList } from "@/components/shared/doc-list";
import { StatusBadge } from "@/components/shared/status-badge";
import type { PurchaseReturn } from "@/features/purchasing/types";
import { formatDate, formatRupiah } from "@/lib/format";

const columns: ColumnDef<PurchaseReturn, unknown>[] = [
  { header: "Tanggal", cell: ({ row }) => formatDate(row.original.docDate) },
  { header: "No. Retur", cell: ({ row }) => <span className="font-mono text-xs">{row.original.docNo}</span> },
  { header: "Pemasok", cell: ({ row }) => row.original.supplier.name },
  { header: "Penerimaan", cell: ({ row }) => row.original.receiptNo },
  { header: "Nota debit atas", cell: ({ row }) => row.original.invoiceNo ?? "-" },
  { header: "Nilai", meta: { align: "right" }, cell: ({ row }) => formatRupiah(row.original.total) },
  { header: "Status", cell: ({ row }) => <StatusBadge status={row.original.status} /> },
];

export default function Page() {
  return (
    <DocList<PurchaseReturn>
      title="Retur Pembelian"
      description="Pengembalian barang ke pemasok (nota debit bila sudah difakturkan)."
      endpoint="/purchasing/returns"
      detailHref={(r) => `/pembelian/retur/${r.id}`}
      newHref="/pembelian/retur/baru"
      newLabel="Retur baru"
      permission="PURCHASE:VIEW"
      createPermission="PURCHASE:CREATE"
      columns={columns}
      partyResource="/suppliers"
    />
  );
}
