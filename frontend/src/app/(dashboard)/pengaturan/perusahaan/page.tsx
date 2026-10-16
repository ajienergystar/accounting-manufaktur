"use client";

import { useEffect } from "react";
import { useQuery, useQueryClient } from "@tanstack/react-query";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { z } from "zod";
import { toast } from "sonner";
import { PageHeader } from "@/components/shared/page-header";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { FormField } from "@/components/ui/form-field";
import { Input } from "@/components/ui/input";
import { Select } from "@/components/ui/select";
import { Skeleton } from "@/components/ui/skeleton";
import { Textarea } from "@/components/ui/textarea";
import { useAuth } from "@/features/auth/auth-context";
import { RequirePermission } from "@/features/auth/require-permission";
import { api, errorMessage } from "@/lib/api";
import { applyServerErrors } from "@/lib/form-errors";

interface Company {
  id: number;
  code: string;
  name: string;
  npwp: string | null;
  address: string | null;
  city: string | null;
  phone: string | null;
  email: string | null;
  logoUrl: string | null;
  baseCurrency: string;
  fiscalYearStartMonth: number;
  inventoryCostingMethod: "MOVING_AVERAGE" | "FIFO";
  allowNegativeStock: boolean;
}

const schema = z.object({
  name: z.string().trim().min(1, "Nama perusahaan wajib diisi").max(150),
  npwp: z.string().max(30).optional(),
  address: z.string().max(500).optional(),
  city: z.string().max(100).optional(),
  phone: z.string().max(30).optional(),
  email: z.union([z.literal(""), z.email("Format email tidak valid")]).optional(),
  logoUrl: z.string().max(500).optional(),
  fiscalYearStartMonth: z.string(),
  inventoryCostingMethod: z.enum(["MOVING_AVERAGE", "FIFO"]),
  allowNegativeStock: z.boolean(),
});
type Form = z.infer<typeof schema>;

const MONTHS = ["Januari", "Februari", "Maret", "April", "Mei", "Juni", "Juli", "Agustus", "September", "Oktober", "November", "Desember"];

function CompanySettings() {
  const { can } = useAuth();
  const qc = useQueryClient();
  const { data, isLoading } = useQuery({ queryKey: ["company"], queryFn: () => api.get<Company>("/company") });
  const {
    register,
    handleSubmit,
    reset,
    setError,
    formState: { errors, isSubmitting, isDirty },
  } = useForm<Form>({ resolver: zodResolver(schema) });

  useEffect(() => {
    if (data)
      reset({
        name: data.name,
        npwp: data.npwp ?? "",
        address: data.address ?? "",
        city: data.city ?? "",
        phone: data.phone ?? "",
        email: data.email ?? "",
        logoUrl: data.logoUrl ?? "",
        fiscalYearStartMonth: String(data.fiscalYearStartMonth),
        inventoryCostingMethod: data.inventoryCostingMethod,
        allowNegativeStock: data.allowNegativeStock,
      });
  }, [data, reset]);

  const onSubmit = handleSubmit(async (v) => {
    try {
      await api.put("/company", { ...v, email: v.email || null, fiscalYearStartMonth: Number(v.fiscalYearStartMonth) });
      toast.success("Profil perusahaan disimpan");
      qc.invalidateQueries({ queryKey: ["company"] });
      qc.invalidateQueries({ queryKey: ["auth", "me"] });
    } catch (err) {
      if (!applyServerErrors(err, setError)) toast.error(errorMessage(err));
    }
  });

  if (isLoading) return <Skeleton className="h-96 w-full" />;
  const readOnly = !can("SETTINGS:EDIT");

  return (
    <form onSubmit={onSubmit} noValidate>
      <fieldset disabled={readOnly} className="grid gap-6 lg:grid-cols-2">
        <Card>
          <CardHeader>
            <CardTitle>Identitas perusahaan</CardTitle>
          </CardHeader>
          <CardContent className="grid gap-4">
            <FormField label="Nama perusahaan" htmlFor="name" required error={errors.name?.message}>
              <Input id="name" {...register("name")} />
            </FormField>
            <FormField label="NPWP" htmlFor="npwp">
              <Input id="npwp" placeholder="00.000.000.0-000.000" {...register("npwp")} />
            </FormField>
            <FormField label="Alamat" htmlFor="address">
              <Textarea id="address" {...register("address")} />
            </FormField>
            <div className="grid gap-4 sm:grid-cols-2">
              <FormField label="Kota" htmlFor="city">
                <Input id="city" {...register("city")} />
              </FormField>
              <FormField label="Telepon" htmlFor="phone">
                <Input id="phone" {...register("phone")} />
              </FormField>
            </div>
            <FormField label="Email" htmlFor="email" error={errors.email?.message}>
              <Input id="email" type="email" {...register("email")} />
            </FormField>
            <FormField label="URL logo" htmlFor="logoUrl" hint="Ditampilkan di kop laporan">
              <Input id="logoUrl" {...register("logoUrl")} />
            </FormField>
          </CardContent>
        </Card>
        <Card className="h-fit">
          <CardHeader>
            <CardTitle>Kebijakan akuntansi</CardTitle>
          </CardHeader>
          <CardContent className="grid gap-4">
            <FormField label="Mata uang dasar" htmlFor="currency">
              <Input id="currency" value={data?.baseCurrency ?? "IDR"} disabled readOnly />
            </FormField>
            <FormField
              label="Bulan awal tahun fiskal"
              htmlFor="fiscalYearStartMonth"
              hint="Tidak dapat diubah setelah tahun fiskal pertama dibuat"
            >
              <Select id="fiscalYearStartMonth" {...register("fiscalYearStartMonth")}>
                {MONTHS.map((m, i) => (
                  <option key={m} value={i + 1}>
                    {m}
                  </option>
                ))}
              </Select>
            </FormField>
            <FormField
              label="Metode penilaian persediaan"
              htmlFor="inventoryCostingMethod"
              hint="Tidak dapat diubah setelah ada mutasi stok"
            >
              <Select id="inventoryCostingMethod" {...register("inventoryCostingMethod")}>
                <option value="MOVING_AVERAGE">Rata-rata tertimbang (moving average)</option>
                <option value="FIFO">FIFO (masuk pertama keluar pertama)</option>
              </Select>
            </FormField>
            <label className="flex items-start gap-2 text-sm">
              <input type="checkbox" className="mt-0.5 h-4 w-4 accent-primary" {...register("allowNegativeStock")} />
              <span>
                Izinkan stok negatif
                <span className="block text-xs text-muted">Tidak disarankan: nilai HPP bisa tidak akurat.</span>
              </span>
            </label>
          </CardContent>
        </Card>
      </fieldset>
      {!readOnly && (
        <div className="mt-6 flex justify-end">
          <Button type="submit" loading={isSubmitting} disabled={!isDirty}>
            Simpan perubahan
          </Button>
        </div>
      )}
    </form>
  );
}

export default function Page() {
  return (
    <RequirePermission permission="SETTINGS:VIEW">
      <PageHeader title="Profil Perusahaan" description="Identitas perusahaan dan kebijakan akuntansi." />
      <CompanySettings />
    </RequirePermission>
  );
}
