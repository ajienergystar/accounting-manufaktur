"use client";

import Link from "next/link";
import { useSearchParams } from "next/navigation";
import { useState } from "react";
import { useForm, useWatch } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { AlertCircle, CheckCircle2 } from "lucide-react";
import { Button } from "@/components/ui/button";
import { FormField } from "@/components/ui/form-field";
import { PasswordInput } from "@/features/auth/password-input";
import { PasswordStrength } from "@/features/auth/password-strength";
import { resetSchema, type ResetForm } from "@/features/auth/schemas";
import { api, errorMessage } from "@/lib/api";
import { applyServerErrors } from "@/lib/form-errors";

export function ResetPasswordForm() {
  const token = useSearchParams().get("token");
  const [done, setDone] = useState(false);
  const [formError, setFormError] = useState<string | null>(null);
  const {
    register,
    handleSubmit,
    control,
    setError,
    formState: { errors, isSubmitting },
  } = useForm<ResetForm>({ resolver: zodResolver(resetSchema), defaultValues: { password: "", confirmPassword: "" } });
  const password = useWatch({ control, name: "password" }) ?? "";

  if (!token) {
    return (
      <div role="alert" className="text-center">
        <AlertCircle className="mx-auto mb-4 h-12 w-12 text-danger" />
        <h1 className="text-xl font-semibold">Link tidak valid</h1>
        <p className="mt-2 text-sm text-muted">Token reset tidak ditemukan. Silakan minta link baru.</p>
        <Link href="/forgot-password" className="mt-4 inline-block text-sm font-medium text-primary hover:underline">
          Minta link reset
        </Link>
      </div>
    );
  }

  if (done) {
    return (
      <div className="text-center">
        <CheckCircle2 className="mx-auto mb-4 h-12 w-12 text-success" />
        <h1 className="text-xl font-semibold">Password berhasil diubah</h1>
        <p className="mt-2 text-sm text-muted">Silakan login dengan password baru Anda.</p>
        <Link
          href="/login"
          className="mt-6 inline-flex h-10 items-center rounded-lg bg-primary px-5 text-sm font-medium text-primary-foreground"
        >
          Ke halaman login
        </Link>
      </div>
    );
  }

  const onSubmit = handleSubmit(async (values) => {
    setFormError(null);
    try {
      await api.post("/auth/reset-password", { token, ...values }, { skipAuthRefresh: true });
      setDone(true);
    } catch (err) {
      if (!applyServerErrors(err, setError)) setFormError(errorMessage(err));
    }
  });

  return (
    <div>
      <h1 className="text-2xl font-semibold tracking-tight">Buat password baru</h1>
      <p className="mt-1 text-sm text-muted">Setelah diganti, semua sesi login lama akan diakhiri.</p>
      {formError && (
        <p role="alert" className="mt-4 rounded-lg bg-danger-soft px-3 py-2 text-sm text-danger">
          {formError}
        </p>
      )}
      <form onSubmit={onSubmit} className="mt-6 space-y-4" noValidate>
        <FormField label="Password baru" htmlFor="password" error={errors.password?.message}>
          <PasswordInput id="password" autoComplete="new-password" invalid={!!errors.password} {...register("password")} />
          <PasswordStrength value={password} />
        </FormField>
        <FormField label="Konfirmasi password" htmlFor="confirmPassword" error={errors.confirmPassword?.message}>
          <PasswordInput
            id="confirmPassword"
            autoComplete="new-password"
            invalid={!!errors.confirmPassword}
            {...register("confirmPassword")}
          />
        </FormField>
        <Button type="submit" className="w-full" size="lg" loading={isSubmitting}>
          Simpan password
        </Button>
      </form>
    </div>
  );
}
