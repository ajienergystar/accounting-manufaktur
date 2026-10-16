import { useQuery } from "@tanstack/react-query";
import { api } from "@/lib/api";

export interface Option {
  id: number;
  code: string;
  name: string;
}

/** Opsi dropdown dari endpoint /{resource}/options. */
export function useOptions(resource: string, enabled = true) {
  return useQuery({
    queryKey: ["options", resource],
    queryFn: () => api.get<Option[]>(`${resource}/options`),
    staleTime: 5 * 60_000,
    enabled,
  });
}

export interface AccountOption {
  id: number;
  code: string;
  name: string;
  accountType: string;
  normalBalance: "DEBIT" | "CREDIT";
  cashBank: boolean;
}

export function usePostableAccounts(params?: { type?: string; cashBank?: boolean }) {
  return useQuery({
    queryKey: ["accounts", "postable", params ?? {}],
    queryFn: () => api.get<AccountOption[]>("/accounts/postable", params),
    staleTime: 5 * 60_000,
  });
}
