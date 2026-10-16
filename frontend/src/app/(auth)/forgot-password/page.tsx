"use client";

import Link from "next/link";
import { useState } from "react";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { ArrowLeft, MailCheck } from "lucide-react";
import { Button } from "@/components/ui/button";
import { FormField } from "@/components/ui/form-field";
import { Input } from "@/components/ui/input";
import { forgotSchema, type ForgotForm } from "@/features/auth/schemas";
import { api, errorMessage } from "@/lib/api";
import type { MessageResponse } from "@/types/common";

export default function ForgotPasswordPage() {
  const [sent, setSent] = useState<string | null>(null);
  const [formError, setFormError] = useState<string | null>(null);
  const {
    register,
    handleSubmit,
    formState: { errors, isSubmitting },
  } = useForm<ForgotForm>({ resolver: zodResolver(forgotSchema), defaultValues: { email: "" } });

  const onSubmit = handleSubmit(async (values) => {
    setFormError(null);
    try {
      const res = await api.post<MessageResponse>("/auth/forgot-password", values, { skipAuthRefresh: true });
      setSent(res.message);
    } catch (err) {
      setFormError(errorMessage(err));
    }
  });

  return (
    <div>
      <Link href="/login" className="mb-6 inline-flex items-center gap-1 text-sm text-muted hover:text-foreground">
        <ArrowLeft className="h-4 w-4" /> Kembali ke login
      </Link>
      {sent ? (
        <div className="text-center">
          <MailCheck className="mx-auto mb-4 h-12 w-12 text-primary" />
          <h1 className="text-xl font-semibold">Periksa email Anda</h1>
          <p className="mt-2 text-sm text-muted">{sent}</p>
          <p className="mt-2 text-xs text-muted">Link berlaku 30 menit dan hanya bisa dipakai sekali.</p>
        </div>
      ) : (
        <>
          <h1 className="text-2xl font-semibold tracking-tight">Lupa password</h1>
          <p className="mt-1 text-sm text-muted">Masukkan email akun Anda, kami kirimkan link untuk membuat password baru.</p>
          {formError && <p role="alert" className="mt-4 text-sm text-danger">{formError}</p>}
          <form onSubmit={onSubmit} className="mt-6 space-y-4" noValidate>
            <FormField label="Email" htmlFor="email" error={errors.email?.message}>
              <Input id="email" type="email" autoComplete="email" autoFocus invalid={!!errors.email} {...register("email")} />
            </FormField>
            <Button type="submit" className="w-full" size="lg" loading={isSubmitting}>
              Kirim link reset
            </Button>
          </form>
        </>
      )}
    </div>
  );
}
