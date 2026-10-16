"use client";

import Link from "next/link";
import { usePathname } from "next/navigation";
import { useState } from "react";
import { ChevronDown, Factory } from "lucide-react";
import { NAV, type NavGroup } from "./nav";
import { cn } from "@/lib/utils";

interface SidebarProps {
  open: boolean;
  onNavigate?: () => void;
  can: (permission?: string) => boolean;
}

function isActive(pathname: string, href: string) {
  return pathname === href || (href !== "/dashboard" && pathname.startsWith(href + "/"));
}

export function Sidebar({ open, onNavigate, can }: SidebarProps) {
  const pathname = usePathname();
  const groups: NavGroup[] = NAV.map((g) => ({ ...g, items: g.items.filter((i) => can(i.permission)) })).filter(
    (g) => g.items.length > 0,
  );

  const activeGroup = groups.find((g) => g.items.some((i) => isActive(pathname, i.href)))?.label;
  const [expanded, setExpanded] = useState<Record<string, boolean>>({});

  return (
    <aside
      className={cn(
        "no-print fixed inset-y-0 left-0 z-40 flex w-64 flex-col bg-sidebar text-sidebar-foreground transition-transform lg:translate-x-0",
        open ? "translate-x-0" : "-translate-x-full",
      )}
      aria-label="Navigasi utama"
    >
      <div className="flex h-16 items-center gap-2.5 border-b border-white/10 px-5">
        <span className="flex h-8 w-8 items-center justify-center rounded-lg bg-primary text-primary-foreground">
          <Factory className="h-4 w-4" />
        </span>
        <div className="leading-tight">
          <p className="text-sm font-semibold text-white">Akuntansi</p>
          <p className="text-xs text-sidebar-foreground/80">Manufaktur</p>
        </div>
      </div>

      <nav className="flex-1 overflow-y-auto px-3 py-4">
        <ul className="space-y-1">
          {groups.map((g) => {
            const single = g.items.length === 1;
            const isOpen = expanded[g.label] ?? g.label === activeGroup;
            const Icon = g.icon;
            if (single) {
              const item = g.items[0];
              const active = isActive(pathname, item.href);
              return (
                <li key={g.label}>
                  <Link
                    href={item.href}
                    onClick={onNavigate}
                    aria-current={active ? "page" : undefined}
                    className={cn(
                      "flex items-center gap-3 rounded-lg px-3 py-2 text-sm hover:bg-white/5",
                      active && "bg-sidebar-active font-medium text-white",
                    )}
                  >
                    <Icon className="h-4 w-4 shrink-0" aria-hidden />
                    {item.label}
                  </Link>
                </li>
              );
            }
            return (
              <li key={g.label}>
                <button
                  type="button"
                  onClick={() => setExpanded((s) => ({ ...s, [g.label]: !isOpen }))}
                  aria-expanded={isOpen}
                  className="flex w-full items-center gap-3 rounded-lg px-3 py-2 text-sm hover:bg-white/5"
                >
                  <Icon className="h-4 w-4 shrink-0" aria-hidden />
                  <span className="flex-1 text-left">{g.label}</span>
                  <ChevronDown className={cn("h-4 w-4 transition-transform", isOpen && "rotate-180")} aria-hidden />
                </button>
                {isOpen && (
                  <ul className="mt-1 space-y-0.5 border-l border-white/10 pl-3 ml-5">
                    {g.items.map((item) => {
                      const active = isActive(pathname, item.href);
                      return (
                        <li key={item.href}>
                          <Link
                            href={item.href}
                            onClick={onNavigate}
                            aria-current={active ? "page" : undefined}
                            className={cn(
                              "block rounded-md px-3 py-1.5 text-sm hover:bg-white/5 hover:text-white",
                              active && "bg-sidebar-active font-medium text-white",
                            )}
                          >
                            {item.label}
                          </Link>
                        </li>
                      );
                    })}
                  </ul>
                )}
              </li>
            );
          })}
        </ul>
      </nav>
    </aside>
  );
}
