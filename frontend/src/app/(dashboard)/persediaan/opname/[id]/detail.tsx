"use client";

import Link from "next/link";
import { useParams, useRouter } from "next/navigation";
import { useEffect, useState } from "react";
import { useQuery, useQueryClient } from "@tanstack/react-query";
import { Ban, Printer, Save, Send, Trash2 } from "lucide-react";
import { toast } from "sonner";
import { Info, InfoGrid } from "@/components/shared/info";
import { PageHeader } from "@/components/shared/page-header";
import { ErrorState } from "@/components/shared/states";
import { StatusBadge } from "@/components/shared/status-badge";
import { VoidDialog } from "@/components/shared/void-dialog";
import { Button } from "@/components/ui/button";
import { Card, CardContent } from "@/components/ui/card";
import { ConfirmDialog } from "@/components/ui/confirm-dialog";
import { Input } from "@/components/ui/input";
import { Skeleton } from "@/components/ui/skeleton";
import { useAuth } from "@/features/auth/auth-context";
import { RequirePermission } from "@/features/auth/require-permission";
import type { Opname } from "@/features/inventory/types";
import { api, errorMessage } from "@/lib/api";
import { formatAmount, formatDate, formatQty } from "@/lib/format";
import { cn } from "@/lib/utils";

function Detail() {
  const { id } = useParams<{ id: string }>();
  const router = useRouter();
  const qc = useQueryClient();
  const { can } = useAuth();
  const key = "/inventory/opnames";
  const { data, isLoading, error, refetch } = useQuery({ queryKey: [key, id], queryFn: () => api.get<Opname>(`${key}/${id}`) });
  const [counts, setCounts] = useState<Record<number, string>>({});
  const [busy, setBusy] = useState<string | null>(null);
  const [confirm, setConfirm] = useState<"post" | "delete" | null>(null);
  const [voiding, setVoiding] = useState(false);

  useEffect(() => {
    if (data) {
      const m: Record<number, string> = {};
      data.lines.forEach((l) => (m[l.itemId] = l.countedQty ?? ""));
      // eslint-disable-next-line react-hooks/set-state-in-effect
      setCounts(m);
    }
  }, [data]);

  if (isLoading) return <Skeleton className="h-96 w-full" />;
  if (error || !data) return <ErrorState message={errorMessage(error)} onRetry={refetch} />;

  const draft = data.status === "DRAFT";
  const run = async (label: string, fn: () => Promise<unknown>, done: string) => {
    setBusy(label);
    try {
      await fn();
      toast.success(done);
      qc.invalidateQueries({ queryKey: [key] });
      qc.invalidateQueries({ queryKey: ["inventory"] });
      await refetch();
    } catch (e) {
      toast.error(errorMessage(e));
    } finally {
      setBusy(null);
      setConfirm(null);
    }
  };
  const save = () =>
    api.put(`${key}/${id}/counts`, {
      lines: Object.entries(counts).map(([itemId, v]) => ({ itemId: Number(itemId), countedQty: v === "" ? null : v })),
    });

  return (
    <>
      <PageHeader
        title={`Stock Opname ${data.docNo}`}
        description={data.notes ?? undefined}
        actions={
          <>
            <Button variant="outline" onClick={() => window.print()}>
              <Printer className="h-4 w-4" /> Cetak lembar
            </Button>
            {draft && can("INVENTORY:EDIT") && (
              <>
                <Button variant="outline" onClick={() => setConfirm("delete")}>
                  <Trash2 className="h-4 w-4 text-danger" /> Hapus
                </Button>
                <Button variant="secondary" loading={busy === "save"} onClick={() => run("save", save, "Hasil hitung disimpan")}>
                  <Save className="h-4 w-4" /> Simpan hitungan
                </Button>
              </>
            )}
            {draft && can("INVENTORY:POST") && (
              <Button onClick={() => setConfirm("post")}>
                <Send className="h-4 w-4" /> Posting
              </Button>
            )}
            {data.status === "POSTED" && can("INVENTORY:POST") && (
              <Button variant="outline" onClick={() => setVoiding(true)}>
                <Ban className="h-4 w-4 text-danger" /> Batalkan
              </Button>
            )}
          </>
        }
      />
      <Card className="mb-6">
        <CardContent className="pt-5">
          <InfoGrid>
            <Info label="Tanggal" value={formatDate(data.docDate)} />
            <Info label="Gudang" value={data.warehouseName} />
            <Info label="Status" value={<StatusBadge status={data.status} />} />
            <Info
              label="Penyesuaian"
              value={data.adjustmentId ? <Link className="text-primary hover:underline" href={`/persediaan/penyesuaian/${data.adjustmentId}`}>{data.adjustmentNo}</Link> : "-"}
            />
            <Info label="Progres hitung" value={`${data.countedLines} / ${data.totalLines} item`} />
            <Info label="Total selisih (estimasi)" value={formatAmount(data.totalDiffValue, { parentheses: true })} />
          </InfoGrid>
        </CardContent>
      </Card>
      <Card>
        <div className="overflow-x-auto">
          <table className="w-full min-w-[860px] text-sm">
            <thead>
              <tr className="border-b border-border bg-surface-muted/60 text-left text-xs uppercase text-muted">
                <th className="px-4 py-2.5">Item</th>
                <th className="px-4 py-2.5 text-right">Saldo Sistem</th>
                <th className="w-44 px-4 py-2.5 text-right">Hasil Hitung</th>
                <th className="px-4 py-2.5 text-right">Selisih</th>
                <th className="px-4 py-2.5 text-right">Nilai Selisih</th>
              </tr>
            </thead>
            <tbody>
              {data.lines.map((l) => {
                const counted = counts[l.itemId] ?? "";
                const diff = counted === "" ? 0 : Number(counted) - Number(l.systemQty);
                return (
                  <tr key={l.itemId} className="border-b border-border">
                    <td className="px-4 py-2">
                      <span className="font-mono text-xs">{l.itemCode}</span> {l.itemName}
                    </td>
                    <td className="num px-4 py-2">
                      {formatQty(l.systemQty)} {l.uomCode}
                    </td>
                    <td className="px-4 py-1.5">
                      {draft ? (
                        <Input
                          className="num"
                          inputMode="decimal"
                          aria-label={`Hasil hitung ${l.itemCode}`}
                          value={counted}
                          onChange={(e) => setCounts({ ...counts, [l.itemId]: e.target.value.replace(",", ".") })}
                        />
                      ) : (
                        <span className="num block">{l.countedQty === null ? "-" : formatQty(l.countedQty)}</span>
                      )}
                    </td>
                    <td className={cn("num px-4 py-2", diff < 0 && "text-danger", diff > 0 && "text-success")}>
                      {counted === "" ? "" : `${diff > 0 ? "+" : ""}${formatQty(diff)}`}
                    </td>
                    <td className="num px-4 py-2">{counted === "" ? "" : formatAmount(diff * Number(l.unitCost), { parentheses: true })}</td>
                  </tr>
                );
              })}
            </tbody>
          </table>
        </div>
      </Card>
      <ConfirmDialog
        open={confirm !== null}
        title={confirm === "post" ? "Posting stock opname?" : "Hapus lembar opname?"}
        message={
          confirm === "post"
            ? "Hasil hitung disimpan, lalu selisih terhadap saldo sistem terkini dibukukan sebagai penyesuaian stok + jurnal."
            : "Lembar opname draft akan dihapus."
        }
        confirmLabel={confirm === "post" ? "Posting" : "Hapus"}
        tone={confirm === "delete" ? "danger" : "primary"}
        loading={busy !== null}
        onClose={() => setConfirm(null)}
        onConfirm={() =>
          confirm === "post"
            ? run("post", async () => {
                await save();
                await api.post(`${key}/${id}/post`);
              }, "Stock opname diposting")
            : (async () => {
                try {
                  await api.delete(`${key}/${id}`);
                  router.push("/persediaan/opname");
                } catch (e) {
                  toast.error(errorMessage(e));
                }
              })()
        }
      />
      <VoidDialog
        open={voiding}
        loading={busy === "void"}
        onClose={() => setVoiding(false)}
        onConfirm={(b) => run("void", () => api.post(`${key}/${id}/void`, b), "Stock opname dibatalkan").then(() => setVoiding(false))}
      />
    </>
  );
}

export function OpnameDetail() {
  return (
    <RequirePermission permission="INVENTORY:VIEW">
      <Detail />
    </RequirePermission>
  );
}
