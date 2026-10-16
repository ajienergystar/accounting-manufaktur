"use client";

import Link from "next/link";
import { useSearchParams } from "next/navigation";
import { Suspense, useState } from "react";
import { useQuery } from "@tanstack/react-query";
import { Download, Printer } from "lucide-react";
import { PageHeader } from "@/components/shared/page-header";
import { ItemPicker, MasterSelect } from "@/components/shared/pickers";
import { EmptyState, ErrorState } from "@/components/shared/states";
import { Button } from "@/components/ui/button";
import { Card, CardContent } from "@/components/ui/card";
import { FormField } from "@/components/ui/form-field";
import { Input } from "@/components/ui/input";
import { Skeleton } from "@/components/ui/skeleton";
import { RequirePermission } from "@/features/auth/require-permission";
import { api, errorMessage } from "@/lib/api";
import { monthRange } from "@/lib/dates";
import { csvNumber, exportCsv } from "@/lib/export";
import { formatAmount, formatDate, formatQty } from "@/lib/format";

interface Line {
  movementId: number;
  date: string;
  sourceTypeLabel: string;
  sourceNo: string | null;
  description: string | null;
  qtyIn: string;
  qtyOut: string;
  unitCost: string;
  valueIn: string;
  valueOut: string;
  balanceQty: string;
  balanceValue: string;
}
interface StockCard {
  itemCode: string;
  itemName: string;
  uomCode: string;
  warehouseName: string;
  from: string;
  to: string;
  openingQty: string;
  openingValue: string;
  totalInQty: string;
  totalOutQty: string;
  closingQty: string;
  closingValue: string;
  lines: Line[];
}

const n = (v: string) => (Number(v) ? formatQty(v) : "");
const a = (v: string) => (Number(v) ? formatAmount(v) : "");

function StockCardPage() {
  const params = useSearchParams();
  const initial = monthRange();
  const [itemId, setItemId] = useState<number | null>(params.get("itemId") ? Number(params.get("itemId")) : null);
  const [warehouseId, setWarehouseId] = useState(params.get("warehouseId") ?? "");
  const [from, setFrom] = useState(initial.from);
  const [to, setTo] = useState(initial.to);

  const enabled = itemId !== null && !!warehouseId;
  const { data, isFetching, error, refetch } = useQuery({
    queryKey: ["inventory", "card", { itemId, warehouseId, from, to }],
    queryFn: () => api.get<StockCard>("/inventory/stock-card", { itemId, warehouseId, from, to }),
    enabled,
  });

  return (
    <>
      <PageHeader
        title="Kartu Stok"
        description="Mutasi masuk/keluar per item per gudang beserta nilai persediaan."
        actions={
          data && (
            <>
              <Button
                variant="outline"
                onClick={() =>
                  exportCsv(
                    `kartu-stok-${data.itemCode}-${from}-${to}`,
                    ["Tanggal", "Dokumen", "Keterangan", "Masuk", "Keluar", "Biaya/Unit", "Nilai Masuk", "Nilai Keluar", "Saldo Qty", "Saldo Nilai"],
                    data.lines.map((l) => [l.date, l.sourceNo ?? "", l.description ?? "", csvNumber(l.qtyIn), csvNumber(l.qtyOut), csvNumber(l.unitCost), csvNumber(l.valueIn), csvNumber(l.valueOut), csvNumber(l.balanceQty), csvNumber(l.balanceValue)]),
                  )
                }
              >
                <Download className="h-4 w-4" /> CSV
              </Button>
              <Button variant="outline" onClick={() => window.print()}>
                <Printer className="h-4 w-4" /> Cetak
              </Button>
            </>
          )
        }
      />
      <Card className="no-print mb-6">
        <CardContent className="grid gap-4 pt-5 md:grid-cols-[1fr_220px_160px_160px]">
          <FormField label="Item" htmlFor="sc-item">
            <ItemPicker id="sc-item" value={itemId} onChange={setItemId} />
          </FormField>
          <FormField label="Gudang" htmlFor="sc-wh">
            <MasterSelect id="sc-wh" resource="/warehouses" value={warehouseId} onChange={(e) => setWarehouseId(e.target.value)} />
          </FormField>
          <FormField label="Dari" htmlFor="sc-from">
            <Input id="sc-from" type="date" value={from} onChange={(e) => setFrom(e.target.value)} />
          </FormField>
          <FormField label="Sampai" htmlFor="sc-to">
            <Input id="sc-to" type="date" value={to} onChange={(e) => setTo(e.target.value)} />
          </FormField>
        </CardContent>
      </Card>
      {!enabled ? (
        <Card>
          <EmptyState title="Pilih item & gudang" />
        </Card>
      ) : isFetching && !data ? (
        <Skeleton className="h-80 w-full" />
      ) : error ? (
        <ErrorState message={errorMessage(error)} onRetry={refetch} />
      ) : data ? (
        <Card>
          <div className="border-b border-border px-5 py-4">
            <h2 className="text-lg font-semibold">
              <span className="font-mono">{data.itemCode}</span> {data.itemName}
            </h2>
            <p className="text-sm text-muted">
              {data.warehouseName} · {formatDate(data.from)} – {formatDate(data.to)} · satuan {data.uomCode}
            </p>
          </div>
          <div className="overflow-x-auto">
            <table className="w-full min-w-[1000px] text-sm">
              <thead>
                <tr className="border-b border-border bg-surface-muted/60 text-xs uppercase tracking-wide text-muted">
                  <th className="px-3 py-2 text-left">Tanggal</th>
                  <th className="px-3 py-2 text-left">Dokumen</th>
                  <th className="px-3 py-2 text-right">Masuk</th>
                  <th className="px-3 py-2 text-right">Keluar</th>
                  <th className="px-3 py-2 text-right">Biaya/Unit</th>
                  <th className="px-3 py-2 text-right">Nilai Masuk</th>
                  <th className="px-3 py-2 text-right">Nilai Keluar</th>
                  <th className="px-3 py-2 text-right">Saldo Qty</th>
                  <th className="px-3 py-2 text-right">Saldo Nilai</th>
                </tr>
              </thead>
              <tbody>
                <tr className="border-b border-border bg-surface-muted/30 font-medium">
                  <td className="px-3 py-2" colSpan={7}>
                    Saldo awal
                  </td>
                  <td className="num px-3 py-2">{formatQty(data.openingQty)}</td>
                  <td className="num px-3 py-2">{formatAmount(data.openingValue)}</td>
                </tr>
                {data.lines.map((l) => (
                  <tr key={l.movementId} className="border-b border-border">
                    <td className="whitespace-nowrap px-3 py-2">{formatDate(l.date)}</td>
                    <td className="px-3 py-2">
                      <span className="font-mono text-xs">{l.sourceNo}</span>
                      <span className="block text-xs text-muted">
                        {l.sourceTypeLabel}
                        {l.description ? ` · ${l.description}` : ""}
                      </span>
                    </td>
                    <td className="num px-3 py-2 text-success">{n(l.qtyIn)}</td>
                    <td className="num px-3 py-2 text-danger">{n(l.qtyOut)}</td>
                    <td className="num px-3 py-2">{formatAmount(l.unitCost)}</td>
                    <td className="num px-3 py-2">{a(l.valueIn)}</td>
                    <td className="num px-3 py-2">{a(l.valueOut)}</td>
                    <td className="num px-3 py-2">{formatQty(l.balanceQty)}</td>
                    <td className="num px-3 py-2">{formatAmount(l.balanceValue)}</td>
                  </tr>
                ))}
              </tbody>
              <tfoot>
                <tr className="font-semibold">
                  <td className="px-3 py-3" colSpan={2}>
                    Total / saldo akhir
                  </td>
                  <td className="num px-3 py-3">{formatQty(data.totalInQty)}</td>
                  <td className="num px-3 py-3">{formatQty(data.totalOutQty)}</td>
                  <td colSpan={3} />
                  <td className="num px-3 py-3">{formatQty(data.closingQty)}</td>
                  <td className="num px-3 py-3">{formatAmount(data.closingValue)}</td>
                </tr>
              </tfoot>
            </table>
          </div>
          <p className="no-print px-5 py-3 text-xs text-muted">
            Lihat juga <Link className="text-primary hover:underline" href="/persediaan">daftar stok</Link>.
          </p>
        </Card>
      ) : null}
    </>
  );
}

export default function Page() {
  return (
    <RequirePermission permission="INVENTORY:VIEW">
      <Suspense fallback={<Skeleton className="h-80 w-full" />}>
        <StockCardPage />
      </Suspense>
    </RequirePermission>
  );
}
