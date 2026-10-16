"use client";

import { useRouter } from "next/navigation";
import { useMemo, useState } from "react";
import { useQuery } from "@tanstack/react-query";
import type { ColumnDef } from "@tanstack/react-table";
import { ClipboardList } from "lucide-react";
import { toast } from "sonner";
import { DataTable } from "@/components/shared/data-table";
import { PageHeader } from "@/components/shared/page-header";
import { MasterSelect } from "@/components/shared/pickers";
import { StatusBadge } from "@/components/shared/status-badge";
import { Button } from "@/components/ui/button";
import { Card } from "@/components/ui/card";
import { Dialog } from "@/components/ui/dialog";
import { FormField } from "@/components/ui/form-field";
import { Input } from "@/components/ui/input";
import { useAuth } from "@/features/auth/auth-context";
import { RequirePermission } from "@/features/auth/require-permission";
import type { Opname } from "@/features/inventory/types";
import { api, errorMessage } from "@/lib/api";
import { formatDate, todayIso } from "@/lib/format";
import type { PageResponse } from "@/types/common";

function Opnames() {
  const router = useRouter();
  const { can } = useAuth();
  const [page, setPage] = useState(0);
  const [creating, setCreating] = useState(false);
  const [form, setForm] = useState({ docDate: todayIso(), warehouseId: "", notes: "", includeZeroStock: false });
  const [saving, setSaving] = useState(false);
  const { data, isLoading, error, refetch } = useQuery({
    queryKey: ["/inventory/opnames", page],
    queryFn: () => api.get<PageResponse<Opname>>("/inventory/opnames", { page, size: 20 }),
  });

  const columns = useMemo<ColumnDef<Opname, unknown>[]>(
    () => [
      { header: "Tanggal", cell: ({ row }) => formatDate(row.original.docDate) },
      { header: "No.", cell: ({ row }) => <span className="font-mono text-xs">{row.original.docNo}</span> },
      { accessorKey: "warehouseName", header: "Gudang" },
      { accessorKey: "notes", header: "Catatan" },
      { header: "Penyesuaian", cell: ({ row }) => row.original.adjustmentNo ?? "-" },
      { header: "Status", cell: ({ row }) => <StatusBadge status={row.original.status} /> },
    ],
    [],
  );

  const create = async () => {
    if (!form.warehouseId) return toast.error("Pilih gudang");
    setSaving(true);
    try {
      const o = await api.post<Opname>("/inventory/opnames", { ...form, warehouseId: Number(form.warehouseId) });
      router.push(`/persediaan/opname/${o.id}`);
    } catch (e) {
      toast.error(errorMessage(e));
    } finally {
      setSaving(false);
    }
  };

  return (
    <>
      <PageHeader
        title="Stock Opname"
        description="Hitung fisik persediaan; selisih dibukukan otomatis sebagai penyesuaian."
        actions={
          can("INVENTORY:CREATE") && (
            <Button onClick={() => setCreating(true)}>
              <ClipboardList className="h-4 w-4" /> Lembar opname baru
            </Button>
          )
        }
      />
      <Card>
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
          onRowClick={(o) => router.push(`/persediaan/opname/${o.id}`)}
        />
      </Card>
      <Dialog
        open={creating}
        onClose={() => setCreating(false)}
        title="Lembar stock opname baru"
        description="Saldo sistem per item di gudang terpilih akan disalin ke lembar hitung."
        footer={
          <>
            <Button variant="outline" onClick={() => setCreating(false)}>
              Batal
            </Button>
            <Button onClick={create} loading={saving}>
              Buat lembar
            </Button>
          </>
        }
      >
        <div className="space-y-4">
          <FormField label="Tanggal opname" htmlFor="op-date">
            <Input id="op-date" type="date" value={form.docDate} onChange={(e) => setForm({ ...form, docDate: e.target.value })} />
          </FormField>
          <FormField label="Gudang" htmlFor="op-wh">
            <MasterSelect id="op-wh" resource="/warehouses" value={form.warehouseId} onChange={(e) => setForm({ ...form, warehouseId: e.target.value })} />
          </FormField>
          <FormField label="Catatan" htmlFor="op-notes">
            <Input id="op-notes" value={form.notes} onChange={(e) => setForm({ ...form, notes: e.target.value })} />
          </FormField>
          <label className="flex items-center gap-2 text-sm">
            <input type="checkbox" className="h-4 w-4 accent-primary" checked={form.includeZeroStock} onChange={(e) => setForm({ ...form, includeZeroStock: e.target.checked })} />
            Sertakan item yang stoknya nol
          </label>
        </div>
      </Dialog>
    </>
  );
}

export default function Page() {
  return (
    <RequirePermission permission="INVENTORY:VIEW">
      <Opnames />
    </RequirePermission>
  );
}
