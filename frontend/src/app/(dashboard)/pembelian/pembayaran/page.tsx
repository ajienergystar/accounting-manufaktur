"use client";

import type { ColumnDef } from "@tanstack/react-table";
import { DocList } from "@/components/shared/doc-list";
import { StatusBadge } from "@/components/shared/status-badge";
import { Badge } from "@/components/ui/badge";
import { PAYMENT_TYPE_LABEL, type ApPayment } from "@/features/purchasing/types";
import { formatDate, formatRupiah } from "@/lib/format";

const columns: ColumnDef<ApPayment, unknown>[] = [
  { header: "Tanggal", cell: ({ row }) => formatDate(row.original.docDate) },
  { header: "No.", cell: ({ row }) => <span className="font-mono text-xs">{row.original.docNo}</span> },
  { header: "Pemasok", cell: ({ row }) => row.original.supplier.name },
  { header: "Jenis", cell: ({ row }) => <Badge tone="info">{PAYMENT_TYPE_LABEL[row.original.paymentType]}</Badge> },
  { header: "Kas/Bank", cell: ({ row }) => row.original.cashAccount?.name ?? "-" },
  { header: "Referensi", cell: ({ row }) => row.original.referenceNo ?? "-" },
  { header: "Jumlah", meta: { align: "right" }, cell: ({ row }) => formatRupiah(row.original.amount) },
  { header: "Status", cell: ({ row }) => <StatusBadge status={row.original.status} /> },
];

export default function Page() {
  return (
    <DocList<ApPayment>
      title="Pembayaran Utang"
      description="Pelunasan faktur, uang muka pembelian, dan kompensasi uang muka."
      endpoint="/purchasing/payments"
      detailHref={(r) => `/pembelian/pembayaran/${r.id}`}
      newHref="/pembelian/pembayaran/baru"
      newLabel="Pembayaran baru"
      permission="PURCHASE:VIEW"
      columns={columns}
      partyResource="/suppliers"
    />
  );
}
