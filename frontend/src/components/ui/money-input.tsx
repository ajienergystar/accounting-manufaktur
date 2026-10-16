"use client";

import { forwardRef, useState } from "react";
import { Input, type InputProps } from "./input";
import { cn } from "@/lib/utils";

const display = new Intl.NumberFormat("id-ID", { minimumFractionDigits: 0, maximumFractionDigits: 4 });

/** Normalisasi input pengguna ("1.250.000,50" atau "1250000.50") → "1250000.50". */
export function normalizeDecimal(raw: string): string {
  const s = raw.trim().replace(/\s/g, "");
  if (s === "") return "";
  // Format Indonesia: titik ribuan, koma desimal
  if (s.includes(",")) return s.replace(/\./g, "").replace(",", ".").replace(/[^\d.-]/g, "");
  // Hanya titik: jika lebih dari satu titik → pemisah ribuan
  const dots = (s.match(/\./g) ?? []).length;
  if (dots > 1) return s.replace(/\./g, "").replace(/[^\d-]/g, "");
  return s.replace(/[^\d.-]/g, "");
}

interface MoneyInputProps extends Omit<InputProps, "value" | "onChange" | "type"> {
  value: string | undefined;
  onValueChange: (v: string) => void;
}

/**
 * Input angka dengan tampilan format Indonesia (1.250.000,5) saat tidak fokus.
 * Nilai yang disimpan selalu string desimal standar ("1250000.5").
 */
export const MoneyInput = forwardRef<HTMLInputElement, MoneyInputProps>(
  ({ value, onValueChange, className, onBlur, onFocus, ...props }, ref) => {
    const [focused, setFocused] = useState(false);
    const [draft, setDraft] = useState("");
    const num = value === undefined || value === "" ? null : Number(value);
    const shown = focused ? draft : num === null || Number.isNaN(num) ? "" : display.format(num);

    return (
      <Input
        ref={ref}
        inputMode="decimal"
        autoComplete="off"
        className={cn("num", className)}
        value={shown}
        onFocus={(e) => {
          setDraft(value ? String(value).replace(".", ",") : "");
          setFocused(true);
          onFocus?.(e);
        }}
        onChange={(e) => {
          setDraft(e.target.value);
          onValueChange(normalizeDecimal(e.target.value));
        }}
        onBlur={(e) => {
          setFocused(false);
          onBlur?.(e);
        }}
        {...props}
      />
    );
  },
);
MoneyInput.displayName = "MoneyInput";
