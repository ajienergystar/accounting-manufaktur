"use client";

import type { ColumnDef } from "@tanstack/react-table";
import { DocList } from "@/components/shared/doc-list";
import { StatusBadge } from "@/components/shared/status-badge";
import type { GoodsReceipt } from "@/features/purchasing/types";
import { formatDate, formatRupiah } from "@/lib/format";

const columns: ColumnDef<GoodsReceipt, unknown>[] = [
  { header: "Tanggal", cell: ({ row }) => formatDate(row.original.docDate) },
  { header: "No. GRN", cell: ({ row }) => <span className="font-mono text-xs">{row.original.docNo}</span> },
  { header: "Pemasok", cell: ({ row }) => row.original.supplier.name },
  { header: "PO", cell: ({ row }) => row.original.orderNo ?? "-" },
  { header: "Surat jalan", cell: ({ row }) => row.original.deliveryNoteNo ?? "-" },
  { header: "Gudang", cell: ({ row }) => row.original.warehouse.name },
  { header: "Nilai", meta: { align: "right" }, cell: ({ row }) => formatRupiah(row.original.totalValue) },
  { header: "Status", cell: ({ row }) => <StatusBadge status={row.original.status} /> },
];

export default function Page() {
  return (
    <DocList<GoodsReceipt>
      title="Penerimaan Barang"
      description="Goods Receipt Note (GRN): barang masuk gudang & jurnal Persediaan / GRNI."
      endpoint="/purchasing/receipts"
      detailHref={(r) => `/pembelian/penerimaan/${r.id}`}
      newHref="/pembelian/penerimaan/baru"
      newLabel="Penerimaan baru"
      permission="PURCHASE:VIEW"
      columns={columns}
      partyResource="/suppliers"
      statusOptions={[
        { value: "POSTED", label: "Posted" },
        { value: "VOID", label: "Batal" },
      ]}
    />
  );
}
