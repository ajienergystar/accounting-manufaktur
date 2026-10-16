"use client";

import { createContext, useCallback, useContext, useMemo, type ReactNode } from "react";
import { useQuery, useQueryClient } from "@tanstack/react-query";
import { api } from "@/lib/api";
import type { Me } from "./types";

interface AuthContextValue {
  me: Me | undefined;
  isLoading: boolean;
  can: (permission?: string) => boolean;
  hasRole: (role: string) => boolean;
  logout: () => Promise<void>;
}

const AuthContext = createContext<AuthContextValue | null>(null);

export const ME_QUERY_KEY = ["auth", "me"] as const;

export function AuthProvider({ children }: { children: ReactNode }) {
  const qc = useQueryClient();
  const { data: me, isLoading } = useQuery({
    queryKey: ME_QUERY_KEY,
    queryFn: () => api.get<Me>("/auth/me"),
    staleTime: 5 * 60_000,
  });

  const permissions = useMemo(() => new Set(me?.permissions ?? []), [me]);

  const can = useCallback((permission?: string) => !permission || permissions.has(permission), [permissions]);
  const hasRole = useCallback((role: string) => me?.roles.includes(role) ?? false, [me]);

  const logout = useCallback(async () => {
    try {
      await api.post("/auth/logout", undefined, { skipAuthRefresh: true });
    } finally {
      qc.clear();
      // Hard reload agar seluruh state client dibuang setelah logout.
      // eslint-disable-next-line @next/next/no-location-assign-relative-destination
      window.location.assign("/login");
    }
  }, [qc]);

  const value = useMemo(() => ({ me, isLoading, can, hasRole, logout }), [me, isLoading, can, hasRole, logout]);
  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth() {
  const ctx = useContext(AuthContext);
  if (!ctx) throw new Error("useAuth harus dipakai di dalam AuthProvider");
  return ctx;
}
