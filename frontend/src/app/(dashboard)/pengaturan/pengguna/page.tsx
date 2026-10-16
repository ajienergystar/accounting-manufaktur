"use client";

import { useMemo, useState } from "react";
import type { ColumnDef } from "@tanstack/react-table";
import { Lock, Search, ShieldCheck, UserCheck, UserX } from "lucide-react";
import { toast } from "sonner";
import { PageHeader } from "@/components/shared/page-header";
import { DataTable } from "@/components/shared/data-table";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { Card } from "@/components/ui/card";
import { Dialog } from "@/components/ui/dialog";
import { Input } from "@/components/ui/input";
import { Select } from "@/components/ui/select";
import { ConfirmDialog } from "@/components/ui/confirm-dialog";
import { RequirePermission } from "@/features/auth/require-permission";
import { useAuth } from "@/features/auth/auth-context";
import { useRoles, useUserAction, useUsers, type UserRow, type UserStatus } from "@/features/users/api";
import { errorMessage } from "@/lib/api";
import { formatDateTime } from "@/lib/format";

const STATUS_TONE = { PENDING: "warning", ACTIVE: "success", INACTIVE: "neutral" } as const;
const STATUS_LABEL = { PENDING: "Menunggu", ACTIVE: "Aktif", INACTIVE: "Nonaktif" } as const;

function RolesDialog({
  user,
  mode,
  onClose,
}: {
  user: UserRow;
  mode: "approve" | "roles";
  onClose: () => void;
}) {
  const { data: roles } = useRoles();
  const action = useUserAction();
  const [selected, setSelected] = useState<string[]>(user.roles);

  const toggle = (code: string) =>
    setSelected((s) => (s.includes(code) ? s.filter((c) => c !== code) : [...s, code]));

  const submit = () =>
    action.mutate(
      { id: user.id, action: mode, roleCodes: selected },
      {
        onSuccess: () => {
          toast.success(mode === "approve" ? `${user.fullName} disetujui` : "Role diperbarui");
          onClose();
        },
        onError: (e) => toast.error(errorMessage(e)),
      },
    );

  return (
    <Dialog
      open
      onClose={onClose}
      title={mode === "approve" ? `Setujui ${user.fullName}` : `Atur role ${user.fullName}`}
      description="Pilih satu atau lebih role untuk pengguna ini."
      footer={
        <>
          <Button variant="outline" onClick={onClose}>
            Batal
          </Button>
          <Button onClick={submit} loading={action.isPending} disabled={selected.length === 0}>
            {mode === "approve" ? "Setujui" : "Simpan"}
          </Button>
        </>
      }
    >
      <fieldset className="space-y-2">
        <legend className="sr-only">Role</legend>
        {roles?.map((r) => (
          <label
            key={r.code}
            className="flex cursor-pointer items-start gap-3 rounded-lg border border-border p-3 hover:bg-surface-muted"
          >
            <input
              type="checkbox"
              className="mt-0.5 h-4 w-4 accent-primary"
              checked={selected.includes(r.code)}
              onChange={() => toggle(r.code)}
            />
            <span>
              <span className="block text-sm font-medium">
                {r.name} <span className="text-xs text-muted">({r.code})</span>
              </span>
              <span className="block text-xs text-muted">{r.description}</span>
            </span>
          </label>
        ))}
      </fieldset>
    </Dialog>
  );
}

function UsersPage() {
  const { can, me } = useAuth();
  const [q, setQ] = useState("");
  const [status, setStatus] = useState<UserStatus | "">("");
  const [page, setPage] = useState(0);
  const [dialog, setDialog] = useState<{ user: UserRow; mode: "approve" | "roles" } | null>(null);
  const [confirm, setConfirm] = useState<{ user: UserRow; action: "activate" | "deactivate" | "unlock" } | null>(null);
  const { data, isLoading, error, refetch } = useUsers({ q, status, page });
  const action = useUserAction();

  const columns = useMemo<ColumnDef<UserRow, unknown>[]>(
    () => [
      {
        header: "Pengguna",
        cell: ({ row }) => (
          <div>
            <p className="font-medium">{row.original.fullName}</p>
            <p className="text-xs text-muted">
              @{row.original.username} · {row.original.email}
            </p>
          </div>
        ),
      },
      {
        header: "Role",
        cell: ({ row }) => (
          <div className="flex flex-wrap gap-1">
            {row.original.roles.map((r) => (
              <Badge key={r} tone="primary">
                {r}
              </Badge>
            ))}
          </div>
        ),
      },
      {
        header: "Status",
        cell: ({ row }) => (
          <div className="flex items-center gap-1.5">
            <Badge tone={STATUS_TONE[row.original.status]}>{STATUS_LABEL[row.original.status]}</Badge>
            {row.original.locked && (
              <Badge tone="danger">
                <Lock className="mr-1 h-3 w-3" /> Terkunci
              </Badge>
            )}
          </div>
        ),
      },
      { header: "Login terakhir", cell: ({ row }) => formatDateTime(row.original.lastLoginAt) },
      {
        id: "actions",
        header: "",
        meta: { align: "right" },
        cell: ({ row }) => {
          const u = row.original;
          const self = u.id === me?.id;
          return (
            <div className="flex justify-end gap-1">
              {u.status === "PENDING" && can("USER:APPROVE") && (
                <Button size="sm" onClick={() => setDialog({ user: u, mode: "approve" })}>
                  <UserCheck className="h-4 w-4" /> Setujui
                </Button>
              )}
              {u.status !== "PENDING" && can("USER:EDIT") && (
                <>
                  <Button size="sm" variant="outline" onClick={() => setDialog({ user: u, mode: "roles" })}>
                    <ShieldCheck className="h-4 w-4" /> Role
                  </Button>
                  {u.locked && (
                    <Button size="sm" variant="outline" onClick={() => setConfirm({ user: u, action: "unlock" })}>
                      Buka kunci
                    </Button>
                  )}
                  {u.status === "ACTIVE" && !self && (
                    <Button size="sm" variant="ghost" onClick={() => setConfirm({ user: u, action: "deactivate" })}>
                      <UserX className="h-4 w-4 text-danger" />
                      <span className="sr-only">Nonaktifkan</span>
                    </Button>
                  )}
                  {u.status === "INACTIVE" && (
                    <Button size="sm" variant="outline" onClick={() => setConfirm({ user: u, action: "activate" })}>
                      Aktifkan
                    </Button>
                  )}
                </>
              )}
            </div>
          );
        },
      },
    ],
    [can, me?.id],
  );

  const confirmText = {
    activate: { title: "Aktifkan pengguna?", message: "Pengguna akan dapat login kembali.", label: "Aktifkan" },
    deactivate: {
      title: "Nonaktifkan pengguna?",
      message: "Pengguna tidak akan bisa login dan semua sesinya diakhiri.",
      label: "Nonaktifkan",
    },
    unlock: { title: "Buka kunci akun?", message: "Penghitung gagal login akan direset.", label: "Buka kunci" },
  };

  return (
    <>
      <PageHeader title="Pengguna" description="Kelola akun, persetujuan registrasi, dan hak akses pengguna." />
      <Card>
        <div className="flex flex-col gap-3 border-b border-border p-4 sm:flex-row">
          <div className="relative flex-1">
            <Search className="pointer-events-none absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-muted" />
            <Input
              placeholder="Cari nama, username, atau email…"
              className="pl-9"
              value={q}
              onChange={(e) => {
                setQ(e.target.value);
                setPage(0);
              }}
              aria-label="Cari pengguna"
            />
          </div>
          <Select
            className="sm:w-48"
            value={status}
            onChange={(e) => {
              setStatus(e.target.value as UserStatus | "");
              setPage(0);
            }}
            aria-label="Filter status"
          >
            <option value="">Semua status</option>
            <option value="PENDING">Menunggu persetujuan</option>
            <option value="ACTIVE">Aktif</option>
            <option value="INACTIVE">Nonaktif</option>
          </Select>
        </div>
        <DataTable
          columns={columns}
          data={data?.content}
          loading={isLoading}
          error={error ? errorMessage(error) : null}
          onRetry={refetch}
          page={page}
          pageSize={20}
          totalElements={data?.totalElements}
          totalPages={data?.totalPages}
          onPageChange={setPage}
          emptyTitle="Tidak ada pengguna"
        />
      </Card>

      {dialog && <RolesDialog user={dialog.user} mode={dialog.mode} onClose={() => setDialog(null)} />}
      {confirm && (
        <ConfirmDialog
          open
          title={confirmText[confirm.action].title}
          message={`${confirm.user.fullName}: ${confirmText[confirm.action].message}`}
          confirmLabel={confirmText[confirm.action].label}
          tone={confirm.action === "deactivate" ? "danger" : "primary"}
          loading={action.isPending}
          onClose={() => setConfirm(null)}
          onConfirm={() =>
            action.mutate(
              { id: confirm.user.id, action: confirm.action },
              {
                onSuccess: () => {
                  toast.success("Berhasil disimpan");
                  setConfirm(null);
                },
                onError: (e) => toast.error(errorMessage(e)),
              },
            )
          }
        />
      )}
    </>
  );
}

export default function Page() {
  return (
    <RequirePermission permission="USER:VIEW">
      <UsersPage />
    </RequirePermission>
  );
}
