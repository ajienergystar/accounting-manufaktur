"use client";

import { useMemo } from "react";
import { useQuery } from "@tanstack/react-query";
import { Combobox } from "./combobox";
import { Select, type SelectProps } from "@/components/ui/select";
import { useOptions } from "@/features/master/use-options";
import { api } from "@/lib/api";

export interface ItemOption {
  id: number;
  code: string;
  name: string;
  itemType: string;
  uomId: number;
  uomCode: string;
  standardCost: string;
  sellingPrice: string;
}

export function useItemOptions(types?: string[]) {
  return useQuery({
    queryKey: ["options", "/items", types ?? []],
    queryFn: () => api.get<ItemOption[]>(`/items/item-options${types?.length ? `?types=${types.join(",")}` : ""}`),
    staleTime: 5 * 60_000,
  });
}

interface PickerProps {
  value: number | null | undefined;
  onChange: (id: number | null) => void;
  invalid?: boolean;
  disabled?: boolean;
  id?: string;
  "aria-label"?: string;
  clearable?: boolean;
}

export function ItemPicker({ types, ...props }: PickerProps & { types?: string[] }) {
  const { data } = useItemOptions(types);
  const options = useMemo(
    () => (data ?? []).map((i) => ({ value: i.id, label: `${i.code} — ${i.name}`, sublabel: i.uomCode })),
    [data],
  );
  return <Combobox options={options} placeholder="Cari kode / nama item…" {...props} />;
}

/** Combobox untuk resource master yang punya endpoint /options. */
export function MasterPicker({ resource, placeholder, ...props }: PickerProps & { resource: string; placeholder?: string }) {
  const { data } = useOptions(resource);
  const options = useMemo(() => (data ?? []).map((o) => ({ value: o.id, label: `${o.code} — ${o.name}` })), [data]);
  return <Combobox options={options} placeholder={placeholder ?? "Cari…"} {...props} />;
}

export function SupplierPicker(props: PickerProps) {
  return <MasterPicker resource="/suppliers" placeholder="Cari pemasok…" {...props} />;
}

export function CustomerPicker(props: PickerProps) {
  return <MasterPicker resource="/customers" placeholder="Cari pelanggan…" {...props} />;
}

/** Select sederhana untuk resource master. value/onChange berupa string id. */
export function MasterSelect({
  resource,
  emptyLabel = "— Pilih —",
  ...props
}: SelectProps & { resource: string; emptyLabel?: string }) {
  const { data } = useOptions(resource);
  return (
    <Select {...props}>
      <option value="">{emptyLabel}</option>
      {data?.map((o) => (
        <option key={o.id} value={o.id}>
          {o.code} — {o.name}
        </option>
      ))}
    </Select>
  );
}

export interface TaxOption {
  id: number;
  code: string;
  name: string;
  taxType: string;
  rate: string;
}

export function useTaxRates() {
  return useQuery({
    queryKey: ["tax-rates", "active"],
    queryFn: () => api.get<{ content: TaxOption[] }>("/tax-rates", { active: true, size: 100 }).then((r) => r.content),
    staleTime: 10 * 60_000,
  });
}
