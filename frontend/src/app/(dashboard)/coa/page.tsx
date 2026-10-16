"use client";

import { useMemo, useState } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { ChevronDown, ChevronRight, FolderTree, Pencil, Plus, Search, Trash2, Wallet } from "lucide-react";
import { toast } from "sonner";
import { PageHeader } from "@/components/shared/page-header";
import { ErrorState } from "@/components/shared/states";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { Card } from "@/components/ui/card";
import { ConfirmDialog } from "@/components/ui/confirm-dialog";
import { Input } from "@/components/ui/input";
import { Skeleton } from "@/components/ui/skeleton";
import { useAuth } from "@/features/auth/auth-context";
import { RequirePermission } from "@/features/auth/require-permission";
import type { Account } from "@/features/coa/types";
import { api, errorMessage } from "@/lib/api";
import { cn } from "@/lib/utils";
import { AccountForm } from "./account-form";

function ChartOfAccounts() {
  const { can } = useAuth();
  const qc = useQueryClient();
  const { data, isLoading, error, refetch } = useQuery({
    queryKey: ["accounts", "tree"],
    queryFn: () => api.get<Account[]>("/accounts"),
  });
  const [q, setQ] = useState("");
  const [collapsed, setCollapsed] = useState<Set<number>>(new Set());
  const [form, setForm] = useState<{ editing: Account | null; parent?: Account | null } | null>(null);
  const [deleting, setDeleting] = useState<Account | null>(null);

  const invalidate = () => qc.invalidateQueries({ queryKey: ["accounts"] });

  const remove = useMutation({
    mutationFn: (id: number) => api.delete(`/accounts/${id}`),
    onSuccess: () => {
      toast.success("Akun dihapus");
      setDeleting(null);
      invalidate();
    },
    onError: (e) => toast.error(errorMessage(e)),
  });

  const rows = useMemo(() => {
    if (!data) return [];
    const term = q.trim().toLowerCase();
    if (term) return data.filter((a) => a.code.includes(term) || a.name.toLowerCase().includes(term));
    const byId = new Map(data.map((a) => [a.id, a]));
    return data.filter((a) => {
      for (let p = a.parentId; p !== null; p = byId.get(p)?.parentId ?? null) {
        if (collapsed.has(p)) return false;
      }
      return true;
    });
  }, [data, q, collapsed]);

  const toggle = (id: number) =>
    setCollapsed((s) => {
      const n = new Set(s);
      if (n.has(id)) n.delete(id);
      else n.add(id);
      return n;
    });

  return (
    <>
      <PageHeader
        title="Bagan Akun"
        description="Chart of Accounts. Akun header untuk pengelompokan; hanya akun detail yang dapat dijurnal."
        actions={
          can("COA:CREATE") && (
            <Button onClick={() => setForm({ editing: null })}>
              <Plus className="h-4 w-4" /> Tambah akun
            </Button>
          )
        }
      />
      <Card>
        <div className="flex flex-col gap-3 border-b border-border p-4 sm:flex-row sm:items-center">
          <div className="relative flex-1">
            <Search className="pointer-events-none absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-muted" />
            <Input
              className="pl-9"
              placeholder="Cari kode atau nama akun…"
              value={q}
              onChange={(e) => setQ(e.target.value)}
              aria-label="Cari akun"
            />
          </div>
          <div className="flex gap-2">
            <Button variant="outline" size="sm" onClick={() => setCollapsed(new Set())}>
              Buka semua
            </Button>
            <Button
              variant="outline"
              size="sm"
              onClick={() => setCollapsed(new Set(data?.filter((a) => a.header && a.level >= 2).map((a) => a.id)))}
            >
              Ringkas
            </Button>
          </div>
        </div>
        {error ? (
          <ErrorState message={errorMessage(error)} onRetry={refetch} />
        ) : (
          <div className="overflow-x-auto">
            <table className="w-full text-sm">
              <thead>
                <tr className="border-b border-border bg-surface-muted/60 text-left text-xs uppercase tracking-wide text-muted">
                  <th className="px-4 py-2.5">Kode & Nama Akun</th>
                  <th className="px-4 py-2.5">Tipe</th>
                  <th className="px-4 py-2.5">Saldo Normal</th>
                  <th className="px-4 py-2.5">Keterangan</th>
                  <th className="px-4 py-2.5" />
                </tr>
              </thead>
              <tbody>
                {isLoading
                  ? Array.from({ length: 10 }).map((_, i) => (
                      <tr key={i} className="border-b border-border">
                        <td colSpan={5} className="px-4 py-2.5">
                          <Skeleton className="h-4 w-full" />
                        </td>
                      </tr>
                    ))
                  : rows.map((a) => (
                      <tr key={a.id} className={cn("border-b border-border hover:bg-surface-muted/50", !a.active && "opacity-50")}>
                        <td className="px-4 py-2">
                          <div className="flex items-center gap-1.5" style={{ paddingLeft: q ? 0 : (a.level - 1) * 20 }}>
                            {a.header && !q ? (
                              <button
                                type="button"
                                onClick={() => toggle(a.id)}
                                className="rounded p-0.5 text-muted hover:bg-surface-muted"
                                aria-label={collapsed.has(a.id) ? "Buka" : "Tutup"}
                                aria-expanded={!collapsed.has(a.id)}
                              >
                                {collapsed.has(a.id) ? <ChevronRight className="h-4 w-4" /> : <ChevronDown className="h-4 w-4" />}
                              </button>
                            ) : (
                              <span className="w-5" />
                            )}
                            {a.header ? (
                              <FolderTree className="h-4 w-4 text-muted" aria-hidden />
                            ) : a.cashBank ? (
                              <Wallet className="h-4 w-4 text-primary" aria-label="Kas/Bank" />
                            ) : null}
                            <span className={cn("font-mono text-xs", a.header && "font-semibold")}>{a.code}</span>
                            <span className={cn(a.header && "font-semibold")}>{a.name}</span>
                            {!a.active && <Badge>Nonaktif</Badge>}
                          </div>
                        </td>
                        <td className="px-4 py-2 text-muted">{a.accountTypeLabel}</td>
                        <td className="px-4 py-2">
                          <Badge tone={a.normalBalance === "DEBIT" ? "info" : "warning"}>
                            {a.normalBalance === "DEBIT" ? "Debit" : "Kredit"}
                          </Badge>
                        </td>
                        <td className="max-w-xs truncate px-4 py-2 text-muted">{a.description}</td>
                        <td className="px-4 py-2">
                          <div className="flex justify-end gap-1">
                            {a.header && can("COA:CREATE") && (
                              <Button
                                size="icon"
                                variant="ghost"
                                onClick={() => setForm({ editing: null, parent: a })}
                                aria-label={`Tambah sub-akun ${a.code}`}
                              >
                                <Plus className="h-4 w-4" />
                              </Button>
                            )}
                            {a.parentId !== null && can("COA:EDIT") && (
                              <Button size="icon" variant="ghost" onClick={() => setForm({ editing: a })} aria-label="Ubah">
                                <Pencil className="h-4 w-4" />
                              </Button>
                            )}
                            {a.parentId !== null && can("COA:DELETE") && (
                              <Button size="icon" variant="ghost" onClick={() => setDeleting(a)} aria-label="Hapus">
                                <Trash2 className="h-4 w-4 text-danger" />
                              </Button>
                            )}
                          </div>
                        </td>
                      </tr>
                    ))}
              </tbody>
            </table>
          </div>
        )}
      </Card>

      {form && data && (
        <AccountForm
          accounts={data}
          editing={form.editing}
          parentHint={form.parent}
          onClose={() => setForm(null)}
          onSaved={invalidate}
        />
      )}
      <ConfirmDialog
        open={deleting !== null}
        title="Hapus akun?"
        message={deleting ? `${deleting.code} ${deleting.name} akan dihapus. Akun yang sudah dipakai jurnal tidak bisa dihapus.` : ""}
        confirmLabel="Hapus"
        tone="danger"
        loading={remove.isPending}
        onConfirm={() => deleting && remove.mutate(deleting.id)}
        onClose={() => setDeleting(null)}
      />
    </>
  );
}

export default function Page() {
  return (
    <RequirePermission permission="COA:VIEW">
      <ChartOfAccounts />
    </RequirePermission>
  );
}
