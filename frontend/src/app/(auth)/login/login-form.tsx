"use client";

import Link from "next/link";
import { useRouter, useSearchParams } from "next/navigation";
import { useState } from "react";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { AlertCircle, Factory } from "lucide-react";
import { useQueryClient } from "@tanstack/react-query";
import { Button } from "@/components/ui/button";
import { FormField } from "@/components/ui/form-field";
import { Input } from "@/components/ui/input";
import { PasswordInput } from "@/features/auth/password-input";
import { loginSchema, type LoginForm as LoginValues } from "@/features/auth/schemas";
import { ME_QUERY_KEY } from "@/features/auth/auth-context";
import type { Me } from "@/features/auth/types";
import { api, errorMessage } from "@/lib/api";
import { applyServerErrors } from "@/lib/form-errors";

function safeNext(next: string | null): string {
  // Cegah open redirect: hanya path internal.
  return next && next.startsWith("/") && !next.startsWith("//") ? next : "/dashboard";
}

export function LoginForm() {
  const router = useRouter();
  const params = useSearchParams();
  const qc = useQueryClient();
  const [formError, setFormError] = useState<string | null>(null);

  const {
    register,
    handleSubmit,
    setError,
    formState: { errors, isSubmitting },
  } = useForm<LoginValues>({
    resolver: zodResolver(loginSchema),
    defaultValues: { identifier: "", password: "", rememberMe: false },
  });

  const onSubmit = handleSubmit(async (values) => {
    setFormError(null);
    try {
      const me = await api.post<Me>("/auth/login", values, { skipAuthRefresh: true });
      qc.setQueryData(ME_QUERY_KEY, me);
      router.replace(safeNext(params.get("next")));
      router.refresh();
    } catch (err) {
      if (!applyServerErrors(err, setError)) setFormError(errorMessage(err));
    }
  });

  return (
    <div>
      <div className="mb-8 lg:hidden">
        <span className="flex h-10 w-10 items-center justify-center rounded-xl bg-primary text-primary-foreground">
          <Factory className="h-5 w-5" />
        </span>
      </div>
      <h1 className="text-2xl font-semibold tracking-tight">Masuk ke akun Anda</h1>
      <p className="mt-1 text-sm text-muted">Gunakan email atau username yang terdaftar.</p>

      {formError && (
        <div role="alert" className="mt-6 flex items-start gap-2 rounded-lg bg-danger-soft px-3 py-2.5 text-sm text-danger">
          <AlertCircle className="mt-0.5 h-4 w-4 shrink-0" />
          {formError}
        </div>
      )}

      <form onSubmit={onSubmit} className="mt-6 space-y-4" noValidate>
        <FormField label="Email atau username" htmlFor="identifier" error={errors.identifier?.message}>
          <Input
            id="identifier"
            autoComplete="username"
            autoFocus
            invalid={!!errors.identifier}
            aria-describedby={errors.identifier ? "identifier-error" : undefined}
            {...register("identifier")}
          />
        </FormField>

        <FormField label="Password" htmlFor="password" error={errors.password?.message}>
          <PasswordInput
            id="password"
            autoComplete="current-password"
            invalid={!!errors.password}
            aria-describedby={errors.password ? "password-error" : undefined}
            {...register("password")}
          />
        </FormField>

        <div className="flex items-center justify-between">
          <label className="flex items-center gap-2 text-sm">
            <input type="checkbox" className="h-4 w-4 rounded border-border accent-primary" {...register("rememberMe")} />
            Ingat saya
          </label>
          <Link href="/forgot-password" className="text-sm font-medium text-primary hover:underline">
            Lupa password?
          </Link>
        </div>

        <Button type="submit" className="w-full" size="lg" loading={isSubmitting}>
          Masuk
        </Button>
      </form>

      <p className="mt-6 text-center text-sm text-muted">
        Belum punya akun?{" "}
        <Link href="/register" className="font-medium text-primary hover:underline">
          Daftar
        </Link>
      </p>
    </div>
  );
}
