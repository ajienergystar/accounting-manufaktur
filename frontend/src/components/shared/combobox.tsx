"use client";

import { useEffect, useId, useMemo, useRef, useState } from "react";
import { Check, ChevronsUpDown, X } from "lucide-react";
import { cn } from "@/lib/utils";

export interface ComboOption {
  value: number;
  label: string;
  sublabel?: string;
  /** Teks tambahan untuk pencarian. */
  keywords?: string;
}

interface ComboboxProps {
  options: ComboOption[];
  value: number | null | undefined;
  onChange: (value: number | null) => void;
  placeholder?: string;
  invalid?: boolean;
  disabled?: boolean;
  clearable?: boolean;
  id?: string;
  className?: string;
  "aria-label"?: string;
}

/** Combobox dengan pencarian & navigasi keyboard (↑ ↓ Enter Esc). */
export function Combobox({
  options,
  value,
  onChange,
  placeholder = "Pilih…",
  invalid,
  disabled,
  clearable,
  id,
  className,
  ...rest
}: ComboboxProps) {
  const listId = useId();
  const inputRef = useRef<HTMLInputElement>(null);
  const wrapRef = useRef<HTMLDivElement>(null);
  const [open, setOpen] = useState(false);
  const [query, setQuery] = useState("");
  const [active, setActive] = useState(0);

  const selected = options.find((o) => o.value === value);

  const filtered = useMemo(() => {
    const q = query.trim().toLowerCase();
    if (!q) return options.slice(0, 200);
    return options
      .filter((o) => `${o.label} ${o.sublabel ?? ""} ${o.keywords ?? ""}`.toLowerCase().includes(q))
      .slice(0, 200);
  }, [options, query]);

  useEffect(() => {
    if (!open) return;
    const onDoc = (e: MouseEvent) => {
      if (wrapRef.current && !wrapRef.current.contains(e.target as Node)) {
        setOpen(false);
        setQuery("");
      }
    };
    document.addEventListener("mousedown", onDoc);
    return () => document.removeEventListener("mousedown", onDoc);
  }, [open]);

  const choose = (o: ComboOption) => {
    onChange(o.value);
    setOpen(false);
    setQuery("");
  };

  return (
    <div ref={wrapRef} className={cn("relative", className)}>
      <input
        ref={inputRef}
        id={id}
        role="combobox"
        aria-expanded={open}
        aria-controls={listId}
        aria-autocomplete="list"
        aria-invalid={invalid || undefined}
        aria-activedescendant={open && filtered[active] ? `${listId}-${active}` : undefined}
        disabled={disabled}
        autoComplete="off"
        placeholder={selected ? undefined : placeholder}
        value={open ? query : selected ? selected.label : ""}
        onFocus={() => {
          setOpen(true);
          setActive(0);
        }}
        onChange={(e) => {
          setQuery(e.target.value);
          setActive(0);
          setOpen(true);
        }}
        onKeyDown={(e) => {
          if (e.key === "ArrowDown") {
            e.preventDefault();
            setOpen(true);
            setActive((a) => Math.min(a + 1, filtered.length - 1));
          } else if (e.key === "ArrowUp") {
            e.preventDefault();
            setActive((a) => Math.max(a - 1, 0));
          } else if (e.key === "Enter") {
            if (open && filtered[active]) {
              e.preventDefault();
              choose(filtered[active]);
            }
          } else if (e.key === "Escape") {
            setOpen(false);
            setQuery("");
          } else if (e.key === "Tab" && open && query && filtered[active]) {
            choose(filtered[active]);
          }
        }}
        className={cn(
          "h-10 w-full truncate rounded-lg border border-border bg-surface pl-3 pr-14 text-sm",
          "placeholder:text-muted focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring",
          "disabled:cursor-not-allowed disabled:opacity-60",
          invalid && "border-danger",
        )}
        {...rest}
      />
      <div className="pointer-events-none absolute inset-y-0 right-0 flex items-center gap-1 pr-2 text-muted">
        {clearable && selected && !disabled && (
          <button
            type="button"
            className="pointer-events-auto rounded p-0.5 hover:bg-surface-muted"
            onClick={() => onChange(null)}
            aria-label="Kosongkan"
          >
            <X className="h-3.5 w-3.5" />
          </button>
        )}
        <ChevronsUpDown className="h-4 w-4" />
      </div>
      {open && (
        <ul
          id={listId}
          role="listbox"
          className="absolute z-50 mt-1 max-h-72 w-full min-w-64 overflow-auto rounded-lg border border-border bg-surface py-1 shadow-lg"
        >
          {filtered.length === 0 ? (
            <li className="px-3 py-2 text-sm text-muted">Tidak ditemukan</li>
          ) : (
            filtered.map((o, i) => (
              <li
                key={o.value}
                id={`${listId}-${i}`}
                role="option"
                aria-selected={o.value === value}
                onMouseDown={(e) => {
                  e.preventDefault();
                  choose(o);
                }}
                onMouseEnter={() => setActive(i)}
                className={cn(
                  "flex cursor-pointer items-center gap-2 px-3 py-1.5 text-sm",
                  i === active && "bg-surface-muted",
                )}
              >
                <Check className={cn("h-3.5 w-3.5 shrink-0", o.value === value ? "text-primary" : "invisible")} />
                <span className="min-w-0 flex-1">
                  <span className="block truncate">{o.label}</span>
                  {o.sublabel && <span className="block truncate text-xs text-muted">{o.sublabel}</span>}
                </span>
              </li>
            ))
          )}
        </ul>
      )}
    </div>
  );
}
