"use client";

import Link from "next/link";
import { useMemo, useState } from "react";
import { useQuery } from "@tanstack/react-query";
import type { ColumnDef } from "@tanstack/react-table";
import { AlertTriangle, CheckCircle2, Search } from "lucide-react";
import { PageHeader } from "@/components/shared/page-header";
import { DataTable } from "@/components/shared/data-table";
import { MasterSelect } from "@/components/shared/pickers";
import { Badge } from "@/components/ui/badge";
import { buttonClasses } from "@/components/ui/button";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Input } from "@/components/ui/input";
import { Select } from "@/components/ui/select";
import { RequirePermission } from "@/features/auth/require-permission";
import { ITEM_TYPES, ITEM_TYPE_TONE, type ItemType } from "@/features/master/item-types";
import { api, errorMessage } from "@/lib/api";
import { formatAmount, formatQty, formatRupiah, todayIso } from "@/lib/format";
import { cn } from "@/lib/utils";
import type { PageResponse } from "@/types/common";

interface StockRow {
  itemId: number;
  itemCode: string;
  itemName: string;
  itemType: ItemType;
  uomCode: string;
  warehouseId: number;
  warehouseName: string;
  qty: string;
  avgCost: string;
  totalValue: string;
  minStock: string;
  belowMinimum: boolean;
}
interface ValuationRow {
  itemType: ItemType;
  itemTypeLabel: string;
  accountCode: string;
  accountName: string;
  stockValue: string;
  ledgerBalance: string;
  difference: string;
}

function StockPage() {
  const [q, setQ] = useState("");
  const [warehouseId, setWarehouseId] = useState("");
  const [type, setType] = useState("");
  const [lowOnly, setLowOnly] = useState(false);
  const [page, setPage] = useState(0);

  const { data, isLoading, error, refetch } = useQuery({
    queryKey: ["inventory", "stock", { q, warehouseId, type, lowOnly, page }],
    queryFn: () =>
      api.get<PageResponse<StockRow>>("/inventory/stock", { q, warehouseId, type, lowOnly, page, size: 50 }),
    placeholderData: (p) => p,
  });
  const valuation = useQuery({
    queryKey: ["inventory", "valuation"],
    queryFn: () => api.get<ValuationRow[]>("/inventory/valuation", { asOf: todayIso() }),
  });

  const columns = useMemo<ColumnDef<StockRow, unknown>[]>(
    () => [
      {
        header: "Item",
        cell: ({ row }) => (
          <div>
            <p className="font-medium">
              <span className="font-mono text-xs">{row.original.itemCode}</span> {row.original.itemName}
            </p>
            <Badge tone={ITEM_TYPE_TONE[row.original.itemType]}>{ITEM_TYPES[row.original.itemType]}</Badge>
          </div>
        ),
      },
      { accessorKey: "warehouseName", header: "Gudang" },
      {
        header: "Qty",
        meta: { align: "right" },
        cell: ({ row }) => (
          <span className={cn(row.original.belowMinimum && "font-semibold text-danger")}>
            {formatQty(row.original.qty)} {row.original.uomCode}
            {row.original.belowMinimum && <AlertTriangle className="ml-1 inline h-3.5 w-3.5" aria-label="Di bawah stok minimum" />}
          </span>
        ),
      },
      { header: "Biaya Rata-rata", meta: { align: "right" }, cell: ({ row }) => formatAmount(row.original.avgCost) },
      { header: "Nilai", meta: { align: "right" }, cell: ({ row }) => formatRupiah(row.original.totalValue) },
      {
        id: "card",
        header: "",
        cell: ({ row }) => (
          <Link
            href={`/persediaan/kartu-stok?itemId=${row.original.itemId}&warehouseId=${row.original.warehouseId}`}
            className="text-sm text-primary hover:underline"
            onClick={(e) => e.stopPropagation()}
          >
            Kartu stok
          </Link>
        ),
      },
    ],
    [],
  );

  return (
    <>
      <PageHeader
        title="Stok Persediaan"
        description="Saldo stok per item per gudang (perpetual)."
        actions={
          <>
            <Link href="/persediaan/mutasi" className={buttonClasses("outline")}>
              Penyesuaian & transfer
            </Link>
            <Link href="/persediaan/opname" className={buttonClasses("outline")}>
              Stock opname
            </Link>
          </>
        }
      />

      {valuation.data && (
        <Card className="mb-6">
          <CardHeader>
            <CardTitle>Rekonsiliasi persediaan vs buku besar</CardTitle>
          </CardHeader>
          <CardContent className="overflow-x-auto p-0">
            <table className="w-full text-sm">
              <thead>
                <tr className="border-b border-border text-left text-xs uppercase text-muted">
                  <th className="px-5 py-2">Kelompok</th>
                  <th className="px-5 py-2">Akun</th>
                  <th className="px-5 py-2 text-right">Kartu stok</th>
                  <th className="px-5 py-2 text-right">Buku besar</th>
                  <th className="px-5 py-2 text-right">Selisih</th>
                </tr>
              </thead>
              <tbody>
                {valuation.data.map((v) => (
                  <tr key={v.itemType} className="border-b border-border last:border-0">
                    <td className="px-5 py-2">{v.itemTypeLabel}</td>
                    <td className="px-5 py-2 text-muted">
                      {v.accountCode} {v.accountName}
                    </td>
                    <td className="num px-5 py-2">{formatAmount(v.stockValue)}</td>
                    <td className="num px-5 py-2">{formatAmount(v.ledgerBalance)}</td>
                    <td className={cn("num px-5 py-2", Number(v.difference) !== 0 ? "font-semibold text-danger" : "text-success")}>
                      {Number(v.difference) === 0 ? (
                        <CheckCircle2 className="ml-auto h-4 w-4" aria-label="Cocok" />
                      ) : (
                        formatAmount(v.difference)
                      )}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </CardContent>
        </Card>
      )}

      <Card>
        <div className="grid gap-3 border-b border-border p-4 md:grid-cols-[1fr_200px_200px_auto]">
          <div className="relative">
            <Search className="pointer-events-none absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-muted" />
            <Input className="pl-9" placeholder="Cari item…" value={q} onChange={(e) => (setQ(e.target.value), setPage(0))} aria-label="Cari item" />
          </div>
          <MasterSelect resource="/warehouses" emptyLabel="Semua gudang" value={warehouseId} onChange={(e) => (setWarehouseId(e.target.value), setPage(0))} aria-label="Gudang" />
          <Select value={type} onChange={(e) => (setType(e.target.value), setPage(0))} aria-label="Tipe">
            <option value="">Semua tipe</option>
            {Object.entries(ITEM_TYPES)
              .filter(([v]) => v !== "SERVICE")
              .map(([v, l]) => (
                <option key={v} value={v}>
                  {l}
                </option>
              ))}
          </Select>
          <label className="flex items-center gap-2 text-sm">
            <input type="checkbox" className="h-4 w-4 accent-primary" checked={lowOnly} onChange={(e) => (setLowOnly(e.target.checked), setPage(0))} />
            Di bawah minimum
          </label>
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
          emptyTitle="Belum ada stok"
          emptyDescription="Stok terbentuk dari penerimaan barang, saldo awal, atau hasil produksi."
        />
      </Card>
    </>
  );
}

export default function Page() {
  return (
    <RequirePermission permission="INVENTORY:VIEW">
      <StockPage />
    </RequirePermission>
  );
}
