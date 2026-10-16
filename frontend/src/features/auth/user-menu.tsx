"use client";

import Link from "next/link";
import { useEffect, useRef, useState } from "react";
import { ChevronDown, KeyRound, LogOut } from "lucide-react";
import { useAuth } from "./auth-context";

export function UserMenu() {
  const { me, logout } = useAuth();
  const [open, setOpen] = useState(false);
  const ref = useRef<HTMLDivElement>(null);

  useEffect(() => {
    if (!open) return;
    const onClick = (e: MouseEvent) => {
      if (ref.current && !ref.current.contains(e.target as Node)) setOpen(false);
    };
    const onKey = (e: KeyboardEvent) => e.key === "Escape" && setOpen(false);
    document.addEventListener("mousedown", onClick);
    document.addEventListener("keydown", onKey);
    return () => {
      document.removeEventListener("mousedown", onClick);
      document.removeEventListener("keydown", onKey);
    };
  }, [open]);

  if (!me) return <div className="h-9 w-32 animate-pulse rounded-lg bg-surface-muted" />;

  const initials = me.fullName
    .split(" ")
    .map((s) => s[0])
    .slice(0, 2)
    .join("")
    .toUpperCase();

  return (
    <div className="relative" ref={ref}>
      <button
        type="button"
        onClick={() => setOpen((o) => !o)}
        aria-haspopup="menu"
        aria-expanded={open}
        className="flex items-center gap-2 rounded-lg px-2 py-1.5 hover:bg-surface-muted"
      >
        <span className="flex h-8 w-8 items-center justify-center rounded-full bg-primary-soft text-xs font-semibold text-primary">
          {initials}
        </span>
        <span className="hidden text-left leading-tight sm:block">
          <span className="block text-sm font-medium">{me.fullName}</span>
          <span className="block text-xs text-muted">{me.company.name}</span>
        </span>
        <ChevronDown className="h-4 w-4 text-muted" />
      </button>
      {open && (
        <div
          role="menu"
          className="absolute right-0 mt-2 w-60 overflow-hidden rounded-xl border border-border bg-surface shadow-lg"
        >
          <div className="border-b border-border px-4 py-3">
            <p className="text-sm font-medium">{me.fullName}</p>
            <p className="truncate text-xs text-muted">{me.email}</p>
            <p className="mt-1 text-xs text-muted">Role: {me.roles.join(", ")}</p>
          </div>
          <Link
            role="menuitem"
            href="/profil"
            onClick={() => setOpen(false)}
            className="flex items-center gap-2 px-4 py-2.5 text-sm hover:bg-surface-muted"
          >
            <KeyRound className="h-4 w-4" /> Profil & ganti password
          </Link>
          <button
            role="menuitem"
            type="button"
            onClick={logout}
            className="flex w-full items-center gap-2 px-4 py-2.5 text-left text-sm text-danger hover:bg-danger-soft"
          >
            <LogOut className="h-4 w-4" /> Keluar
          </button>
        </div>
      )}
    </div>
  );
}
