"use client";

import { useMemo, useState } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { toast } from "sonner";
import { PageHeader } from "@/components/shared/page-header";
import { ErrorState } from "@/components/shared/states";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Select } from "@/components/ui/select";
import { Skeleton } from "@/components/ui/skeleton";
import { useAuth } from "@/features/auth/auth-context";
import { RequirePermission } from "@/features/auth/require-permission";
import type { Account } from "@/features/coa/types";
import { api, errorMessage } from "@/lib/api";

interface Mapping {
  key: string;
  groupName: string;
  description: string;
  accountId: number;
  accountCode: string;
  accountName: string;
}

function AccountMappings() {
  const { can } = useAuth();
  const qc = useQueryClient();
  const mappings = useQuery({ queryKey: ["account-mappings"], queryFn: () => api.get<Mapping[]>("/account-mappings") });
  const accounts = useQuery({ queryKey: ["accounts", "tree"], queryFn: () => api.get<Account[]>("/accounts") });
  const [saving, setSaving] = useState<string | null>(null);

  const update = useMutation({
    mutationFn: ({ key, accountId }: { key: string; accountId: number }) =>
      api.put(`/account-mappings/${key}`, { accountId }),
    onMutate: ({ key }) => setSaving(key),
    onSettled: () => setSaving(null),
    onSuccess: () => {
      toast.success("Mapping diperbarui");
      qc.invalidateQueries({ queryKey: ["account-mappings"] });
    },
    onError: (e) => toast.error(errorMessage(e)),
  });

  const groups = useMemo(() => {
    const m = new Map<string, Mapping[]>();
    mappings.data?.forEach((x) => m.set(x.groupName, [...(m.get(x.groupName) ?? []), x]));
    return [...m.entries()];
  }, [mappings.data]);

  if (mappings.isLoading || accounts.isLoading) return <Skeleton className="h-96 w-full" />;
  if (mappings.error) return <ErrorState message={errorMessage(mappings.error)} onRetry={mappings.refetch} />;

  const detail = accounts.data?.filter((a) => !a.header && a.active) ?? [];
  const headers = accounts.data?.filter((a) => a.header) ?? [];
  const editable = can("SETTINGS:EDIT");

  return (
    <div className="grid gap-6 xl:grid-cols-2">
      {groups.map(([group, items]) => (
        <Card key={group}>
          <CardHeader>
            <CardTitle>{group}</CardTitle>
          </CardHeader>
          <CardContent className="space-y-3">
            {items.map((m) => {
              const options = m.key === "FOH_ACTUAL_CONTROL" ? headers : detail;
              return (
                <div key={m.key} className="grid gap-1 sm:grid-cols-[1fr_1.2fr] sm:items-center sm:gap-4">
                  <label htmlFor={`map-${m.key}`} className="text-sm">
                    {m.description}
                    <span className="block font-mono text-[11px] text-muted">{m.key}</span>
                  </label>
                  <Select
                    id={`map-${m.key}`}
                    value={m.accountId}
                    disabled={!editable || saving === m.key}
                    onChange={(e) => update.mutate({ key: m.key, accountId: Number(e.target.value) })}
                  >
                    {options.map((a) => (
                      <option key={a.id} value={a.id}>
                        {a.code} — {a.name}
                      </option>
                    ))}
                  </Select>
                </div>
              );
            })}
          </CardContent>
        </Card>
      ))}
    </div>
  );
}

export default function Page() {
  return (
    <RequirePermission permission="SETTINGS:VIEW">
      <PageHeader
        title="Account Mapping"
        description="Akun default yang dipakai jurnal otomatis dari setiap modul. Perubahan berlaku untuk transaksi berikutnya."
      />
      <AccountMappings />
    </RequirePermission>
  );
}
