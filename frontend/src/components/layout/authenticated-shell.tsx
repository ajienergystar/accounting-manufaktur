"use client";

import type { ReactNode } from "react";
import { AppShell } from "./app-shell";
import { AuthProvider, useAuth } from "@/features/auth/auth-context";
import { UserMenu } from "@/features/auth/user-menu";

function ShellWithAuth({ children }: { children: ReactNode }) {
  const { can } = useAuth();
  return (
    <AppShell can={can} userMenu={<UserMenu />}>
      {children}
    </AppShell>
  );
}

export function AuthenticatedShell({ children }: { children: ReactNode }) {
  return (
    <AuthProvider>
      <ShellWithAuth>{children}</ShellWithAuth>
    </AuthProvider>
  );
}
