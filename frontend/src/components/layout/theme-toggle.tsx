"use client";

import { Moon, Sun } from "lucide-react";
import { useSyncExternalStore } from "react";
import { applyTheme } from "@/lib/theme";

function subscribe(cb: () => void) {
  const obs = new MutationObserver(cb);
  obs.observe(document.documentElement, { attributes: true, attributeFilter: ["class"] });
  return () => obs.disconnect();
}

export function ThemeToggle() {
  const dark = useSyncExternalStore(
    subscribe,
    () => document.documentElement.classList.contains("dark"),
    () => false,
  );
  return (
    <button
      type="button"
      onClick={() => applyTheme(dark ? "light" : "dark")}
      className="rounded-lg p-2 text-muted hover:bg-surface-muted hover:text-foreground"
      aria-label={dark ? "Gunakan tema terang" : "Gunakan tema gelap"}
    >
      {dark ? <Sun className="h-4 w-4" /> : <Moon className="h-4 w-4" />}
    </button>
  );
}
