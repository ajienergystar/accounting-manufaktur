"use client";

import { useState, type ReactNode } from "react";
import { Menu } from "lucide-react";
import { Sidebar } from "./sidebar";
import { ThemeToggle } from "./theme-toggle";

interface AppShellProps {
  children: ReactNode;
  userMenu?: ReactNode;
  can?: (permission?: string) => boolean;
}

export function AppShell({ children, userMenu, can = () => true }: AppShellProps) {
  const [open, setOpen] = useState(false);

  return (
    <div className="min-h-screen">
      <a href="#main" className="sr-only focus:not-sr-only focus:absolute focus:left-4 focus:top-4 focus:z-50">
        Lewati ke konten
      </a>
      <Sidebar open={open} onNavigate={() => setOpen(false)} can={can} />
      {open && (
        <div className="fixed inset-0 z-30 bg-black/40 lg:hidden" onClick={() => setOpen(false)} aria-hidden />
      )}
      <div className="lg:pl-64">
        <header className="no-print sticky top-0 z-20 flex h-16 items-center gap-3 border-b border-border bg-surface/90 px-4 backdrop-blur sm:px-6">
          <button
            type="button"
            className="rounded-lg p-2 text-muted hover:bg-surface-muted lg:hidden"
            onClick={() => setOpen(true)}
            aria-label="Buka menu"
          >
            <Menu className="h-5 w-5" />
          </button>
          <div className="flex-1" />
          <ThemeToggle />
          {userMenu}
        </header>
        <main id="main" className="mx-auto w-full max-w-[1400px] px-4 py-6 sm:px-6">
          {children}
        </main>
      </div>
    </div>
  );
}
