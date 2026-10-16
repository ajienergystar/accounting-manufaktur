"use client";

import type { ReactNode } from "react";
import { ShieldAlert } from "lucide-react";
import { useAuth } from "./auth-context";
import { Skeleton } from "@/components/ui/skeleton";

/** Guard level halaman/komponen berdasarkan permission. */
export function RequirePermission({ permission, children }: { permission: string; children: ReactNode }) {
  const { can, isLoading } = useAuth();
  if (isLoading) return <Skeleton className="h-40 w-full" />;
  if (!can(permission)) {
    return (
      <div role="alert" className="flex flex-col items-center justify-center py-20 text-center">
        <ShieldAlert className="mb-3 h-10 w-10 text-warning" />
        <h1 className="text-lg font-semibold">Akses ditolak</h1>
        <p className="mt-1 text-sm text-muted">Anda tidak memiliki hak akses ({permission}) untuk halaman ini.</p>
      </div>
    );
  }
  return <>{children}</>;
}

/** Render children hanya jika user punya permission (untuk tombol/aksi). */
export function Can({ permission, children }: { permission: string; children: ReactNode }) {
  const { can } = useAuth();
  return can(permission) ? <>{children}</> : null;
}
