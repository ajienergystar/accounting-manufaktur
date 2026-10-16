"use client";

import { useState } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { CalendarPlus, Lock, LockOpen, ShieldCheck } from "lucide-react";
import { toast } from "sonner";
import { PageHeader } from "@/components/shared/page-header";
import { EmptyState, ErrorState } from "@/components/shared/states";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { ConfirmDialog } from "@/components/ui/confirm-dialog";
import { Dialog } from "@/components/ui/dialog";
import { FormField } from "@/components/ui/form-field";
import { Input } from "@/components/ui/input";
import { Skeleton } from "@/components/ui/skeleton";
import { useAuth } from "@/features/auth/auth-context";
import { RequirePermission } from "@/features/auth/require-permission";
import { api, errorMessage } from "@/lib/api";
import { formatDate, formatDateTime } from "@/lib/format";

type PeriodStatus = "OPEN" | "CLOSED" | "LOCKED";
interface Period {
  id: number;
  periodNo: number;
  name: string;
  startDate: string;
  endDate: string;
  status: PeriodStatus;
  closedAt: string | null;
  closedBy: string | null;
}
interface FiscalYear {
  id: number;
  year: number;
  startDate: string;
  endDate: string;
  status: "OPEN" | "CLOSED";
  periods: Period[];
}

const STATUS = {
  OPEN: { label: "Terbuka", tone: "success" },
  CLOSED: { label: "Ditutup", tone: "warning" },
  LOCKED: { label: "Dikunci", tone: "danger" },
} as const;

type Action = "close" | "reopen" | "lock";
const ACTION_TEXT: Record<Action, { title: string; message: string; label: string }> = {
  close: {
    title: "Tutup periode?",
    message: "Transaksi tidak dapat lagi diposting ke periode ini. Admin masih dapat membukanya kembali.",
    label: "Tutup periode",
  },
  reopen: { title: "Buka kembali periode?", message: "Periode akan kembali menerima posting transaksi.", label: "Buka kembali" },
  lock: {
    title: "Kunci periode permanen?",
    message: "Periode yang dikunci TIDAK DAPAT dibuka kembali. Pastikan laporan periode ini sudah final.",
    label: "Kunci permanen",
  },
};

function Periods() {
  const { can, hasRole } = useAuth();
  const qc = useQueryClient();
  const { data, isLoading, error, refetch } = useQuery({
    queryKey: ["fiscal-years"],
    queryFn: () => api.get<FiscalYear[]>("/fiscal-years"),
  });
  const [creating, setCreating] = useState(false);
  const [year, setYear] = useState(String(new Date().getFullYear()));
  const [confirm, setConfirm] = useState<{ period: Period; action: Action } | null>(null);

  const create = useMutation({
    mutationFn: () => api.post("/fiscal-years", { year: Number(year) }),
    onSuccess: () => {
      toast.success(`Tahun fiskal ${year} dibuat dengan 12 periode`);
      setCreating(false);
      qc.invalidateQueries({ queryKey: ["fiscal-years"] });
    },
    onError: (e) => toast.error(errorMessage(e)),
  });

  const act = useMutation({
    mutationFn: ({ period, action }: { period: Period; action: Action }) => api.post(`/periods/${period.id}/${action}`),
    onSuccess: () => {
      toast.success("Status periode diperbarui");
      setConfirm(null);
      qc.invalidateQueries({ queryKey: ["fiscal-years"] });
    },
    onError: (e) => toast.error(errorMessage(e)),
  });

  return (
    <>
      <PageHeader
        title="Periode Akuntansi"
        description="Tahun fiskal & periode bulanan. Transaksi hanya dapat diposting ke periode yang terbuka."
        actions={
          can("PERIOD:CREATE") && (
            <Button onClick={() => setCreating(true)}>
              <CalendarPlus className="h-4 w-4" /> Buat tahun fiskal
            </Button>
          )
        }
      />
      {isLoading ? (
        <Skeleton className="h-64 w-full" />
      ) : error ? (
        <ErrorState message={errorMessage(error)} onRetry={refetch} />
      ) : !data?.length ? (
        <Card>
          <EmptyState
            title="Belum ada tahun fiskal"
            description="Buat tahun fiskal terlebih dahulu sebelum mencatat transaksi."
            action={can("PERIOD:CREATE") && <Button onClick={() => setCreating(true)}>Buat tahun fiskal</Button>}
          />
        </Card>
      ) : (
        <div className="space-y-6">
          {data.map((fy) => (
            <Card key={fy.id}>
              <CardHeader>
                <div>
                  <CardTitle>Tahun Fiskal {fy.year}</CardTitle>
                  <p className="mt-0.5 text-sm text-muted">
                    {formatDate(fy.startDate)} – {formatDate(fy.endDate)}
                  </p>
                </div>
                <Badge tone={fy.status === "OPEN" ? "success" : "neutral"}>{fy.status === "OPEN" ? "Berjalan" : "Ditutup"}</Badge>
              </CardHeader>
              <CardContent className="p-0">
                <div className="overflow-x-auto">
                  <table className="w-full text-sm">
                    <thead>
                      <tr className="border-b border-border bg-surface-muted/60 text-left text-xs uppercase tracking-wide text-muted">
                        <th className="px-4 py-2.5">#</th>
                        <th className="px-4 py-2.5">Periode</th>
                        <th className="px-4 py-2.5">Rentang</th>
                        <th className="px-4 py-2.5">Status</th>
                        <th className="px-4 py-2.5">Ditutup oleh</th>
                        <th className="px-4 py-2.5" />
                      </tr>
                    </thead>
                    <tbody>
                      {fy.periods.map((p) => (
                        <tr key={p.id} className="border-b border-border last:border-0">
                          <td className="px-4 py-2 text-muted">{p.periodNo}</td>
                          <td className="px-4 py-2 font-medium">{p.name}</td>
                          <td className="px-4 py-2 text-muted">
                            {formatDate(p.startDate, "dd MMM")} – {formatDate(p.endDate, "dd MMM yyyy")}
                          </td>
                          <td className="px-4 py-2">
                            <Badge tone={STATUS[p.status].tone}>{STATUS[p.status].label}</Badge>
                          </td>
                          <td className="px-4 py-2 text-muted">
                            {p.closedBy ? `${p.closedBy} · ${formatDateTime(p.closedAt)}` : "-"}
                          </td>
                          <td className="px-4 py-2">
                            <div className="flex justify-end gap-1">
                              {p.status === "OPEN" && can("PERIOD:POST") && (
                                <Button size="sm" variant="outline" onClick={() => setConfirm({ period: p, action: "close" })}>
                                  <Lock className="h-3.5 w-3.5" /> Tutup
                                </Button>
                              )}
                              {p.status === "CLOSED" && hasRole("ADMIN") && (
                                <>
                                  <Button size="sm" variant="ghost" onClick={() => setConfirm({ period: p, action: "reopen" })}>
                                    <LockOpen className="h-3.5 w-3.5" /> Buka
                                  </Button>
                                  <Button size="sm" variant="ghost" onClick={() => setConfirm({ period: p, action: "lock" })}>
                                    <ShieldCheck className="h-3.5 w-3.5 text-danger" /> Kunci
                                  </Button>
                                </>
                              )}
                            </div>
                          </td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                </div>
              </CardContent>
            </Card>
          ))}
        </div>
      )}

      <Dialog
        open={creating}
        onClose={() => setCreating(false)}
        title="Buat tahun fiskal"
        description="12 periode bulanan dibuat otomatis mengikuti bulan awal tahun fiskal perusahaan."
        size="sm"
        footer={
          <>
            <Button variant="outline" onClick={() => setCreating(false)}>
              Batal
            </Button>
            <Button onClick={() => create.mutate()} loading={create.isPending}>
              Buat
            </Button>
          </>
        }
      >
        <FormField label="Tahun" htmlFor="fy-year">
          <Input id="fy-year" type="number" min={2000} max={2100} value={year} onChange={(e) => setYear(e.target.value)} />
        </FormField>
      </Dialog>

      {confirm && (
        <ConfirmDialog
          open
          title={ACTION_TEXT[confirm.action].title}
          message={`${confirm.period.name}: ${ACTION_TEXT[confirm.action].message}`}
          confirmLabel={ACTION_TEXT[confirm.action].label}
          tone={confirm.action === "lock" ? "danger" : "primary"}
          loading={act.isPending}
          onConfirm={() => act.mutate(confirm)}
          onClose={() => setConfirm(null)}
        />
      )}
    </>
  );
}

export default function Page() {
  return (
    <RequirePermission permission="PERIOD:VIEW">
      <Periods />
    </RequirePermission>
  );
}
