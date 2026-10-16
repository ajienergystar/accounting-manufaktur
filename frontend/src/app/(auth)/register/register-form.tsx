"use client";

import Link from "next/link";
import { useState } from "react";
import { useForm, useWatch } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { useQuery } from "@tanstack/react-query";
import { AlertCircle, CheckCircle2 } from "lucide-react";
import { Button } from "@/components/ui/button";
import { FormField } from "@/components/ui/form-field";
import { Input } from "@/components/ui/input";
import { PasswordInput } from "@/features/auth/password-input";
import { PasswordStrength } from "@/features/auth/password-strength";
import { registerSchema, type RegisterForm as RegisterValues } from "@/features/auth/schemas";
import type { RegisterResponse } from "@/features/auth/types";
import { api, errorMessage } from "@/lib/api";
import { applyServerErrors } from "@/lib/form-errors";

export function RegisterForm() {
  const [formError, setFormError] = useState<string | null>(null);
  const [result, setResult] = useState<RegisterResponse | null>(null);

  const { data: regStatus } = useQuery({
    queryKey: ["auth", "registration-status"],
    queryFn: () => api.get<{ hasUsers: boolean }>("/auth/registration-status", undefined, { skipAuthRefresh: true }),
  });
  const needCompany = regStatus ? !regStatus.hasUsers : false;

  const {
    register,
    handleSubmit,
    setError,
    control,
    formState: { errors, isSubmitting },
  } = useForm<RegisterValues>({
    resolver: zodResolver(registerSchema),
    values: {
      fullName: "",
      email: "",
      username: "",
      password: "",
      confirmPassword: "",
      companyName: "",
      needCompany,
    },
    resetOptions: { keepDirtyValues: true },
  });
  const password = useWatch({ control, name: "password" }) ?? "";

  const onSubmit = handleSubmit(async (values) => {
    setFormError(null);
    const payload = {
      fullName: values.fullName,
      email: values.email,
      username: values.username,
      password: values.password,
      confirmPassword: values.confirmPassword,
      companyName: values.needCompany ? values.companyName : undefined,
    };
    try {
      setResult(await api.post<RegisterResponse>("/auth/register", payload, { skipAuthRefresh: true }));
    } catch (err) {
      if (!applyServerErrors(err, setError)) setFormError(errorMessage(err));
    }
  });

  if (result) {
    return (
      <div className="text-center">
        <CheckCircle2 className="mx-auto mb-4 h-12 w-12 text-success" />
        <h1 className="text-xl font-semibold">Registrasi berhasil</h1>
        <p className="mt-2 text-sm text-muted">{result.message}</p>
        <Link
          href="/login"
          className="mt-6 inline-flex h-10 items-center rounded-lg bg-primary px-5 text-sm font-medium text-primary-foreground hover:bg-primary-hover"
        >
          Ke halaman login
        </Link>
      </div>
    );
  }

  const field = (name: keyof RegisterValues) => ({
    invalid: !!errors[name],
    "aria-describedby": errors[name] ? `${name}-error` : undefined,
  });

  return (
    <div>
      <h1 className="text-2xl font-semibold tracking-tight">Buat akun baru</h1>
      <p className="mt-1 text-sm text-muted">
        {needCompany
          ? "Anda pendaftar pertama dan akan menjadi Administrator perusahaan."
          : "Akun baru perlu disetujui administrator sebelum bisa dipakai."}
      </p>

      {formError && (
        <div role="alert" className="mt-6 flex items-start gap-2 rounded-lg bg-danger-soft px-3 py-2.5 text-sm text-danger">
          <AlertCircle className="mt-0.5 h-4 w-4 shrink-0" />
          {formError}
        </div>
      )}

      <form onSubmit={onSubmit} className="mt-6 space-y-4" noValidate>
        {needCompany && (
          <FormField label="Nama perusahaan" htmlFor="companyName" required error={errors.companyName?.message}>
            <Input id="companyName" placeholder="PT Maju Jaya Manufaktur" {...field("companyName")} {...register("companyName")} />
          </FormField>
        )}
        <FormField label="Nama lengkap" htmlFor="fullName" required error={errors.fullName?.message}>
          <Input id="fullName" autoComplete="name" {...field("fullName")} {...register("fullName")} />
        </FormField>
        <div className="grid gap-4 sm:grid-cols-2">
          <FormField label="Email" htmlFor="email" required error={errors.email?.message}>
            <Input id="email" type="email" autoComplete="email" {...field("email")} {...register("email")} />
          </FormField>
          <FormField label="Username" htmlFor="username" required error={errors.username?.message}>
            <Input id="username" autoComplete="username" {...field("username")} {...register("username")} />
          </FormField>
        </div>
        <FormField label="Password" htmlFor="password" required error={errors.password?.message}>
          <PasswordInput id="password" autoComplete="new-password" {...field("password")} {...register("password")} />
          <PasswordStrength value={password} />
        </FormField>
        <FormField label="Konfirmasi password" htmlFor="confirmPassword" required error={errors.confirmPassword?.message}>
          <PasswordInput
            id="confirmPassword"
            autoComplete="new-password"
            {...field("confirmPassword")}
            {...register("confirmPassword")}
          />
        </FormField>

        <Button type="submit" className="w-full" size="lg" loading={isSubmitting}>
          Daftar
        </Button>
      </form>

      <p className="mt-6 text-center text-sm text-muted">
        Sudah punya akun?{" "}
        <Link href="/login" className="font-medium text-primary hover:underline">
          Masuk
        </Link>
      </p>
    </div>
  );
}
