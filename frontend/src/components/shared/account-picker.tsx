"use client";

import { useMemo } from "react";
import { Combobox } from "./combobox";
import { usePostableAccounts } from "@/features/master/use-options";
import { ACCOUNT_TYPES, type AccountType } from "@/features/coa/types";

interface AccountPickerProps {
  value: number | null | undefined;
  onChange: (id: number | null) => void;
  type?: AccountType;
  cashBank?: boolean;
  invalid?: boolean;
  disabled?: boolean;
  id?: string;
  clearable?: boolean;
  placeholder?: string;
  "aria-label"?: string;
}

/** Pemilih akun detail aktif dengan pencarian kode/nama. */
export function AccountPicker({ type, cashBank, placeholder = "Cari kode / nama akun…", ...props }: AccountPickerProps) {
  const { data } = usePostableAccounts({ type, cashBank });
  const options = useMemo(
    () =>
      (data ?? []).map((a) => ({
        value: a.id,
        label: `${a.code} — ${a.name}`,
        sublabel: ACCOUNT_TYPES[a.accountType as AccountType],
      })),
    [data],
  );
  return <Combobox options={options} placeholder={placeholder} {...props} />;
}
