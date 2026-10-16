"use client";

import { useForm, useWatch } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { toast } from "sonner";
import { PageHeader } from "@/components/shared/page-header";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { FormField } from "@/components/ui/form-field";
import { useAuth } from "@/features/auth/auth-context";
import { PasswordInput } from "@/features/auth/password-input";
import { PasswordStrength } from "@/features/auth/password-strength";
import { changePasswordSchema, type ChangePasswordForm } from "@/features/auth/schemas";
import { api, errorMessage } from "@/lib/api";
import { applyServerErrors } from "@/lib/form-errors";

export default function ProfilePage() {
  const { me } = useAuth();
  const {
    register,
    handleSubmit,
    control,
    reset,
    setError,
    formState: { errors, isSubmitting },
  } = useForm<ChangePasswordForm>({
    resolver: zodResolver(changePasswordSchema),
    defaultValues: { currentPassword: "", newPassword: "", confirmPassword: "" },
  });
  const newPassword = useWatch({ control, name: "newPassword" }) ?? "";

  const onSubmit = handleSubmit(async (values) => {
    try {
      await api.post("/auth/change-password", values);
      toast.success("Password berhasil diubah. Sesi di perangkat lain telah diakhiri.");
      reset();
    } catch (err) {
      if (!applyServerErrors(err, setError)) toast.error(errorMessage(err));
    }
  });

  return (
    <>
      <PageHeader title="Profil" description="Informasi akun dan keamanan." />
      <div className="grid gap-6 lg:grid-cols-2">
        <Card>
          <CardHeader>
            <CardTitle>Informasi akun</CardTitle>
          </CardHeader>
          <CardContent>
            <dl className="grid grid-cols-[8rem_1fr] gap-y-2 text-sm">
              <dt className="text-muted">Nama</dt>
              <dd>{me?.fullName}</dd>
              <dt className="text-muted">Username</dt>
              <dd>{me?.username}</dd>
              <dt className="text-muted">Email</dt>
              <dd>{me?.email}</dd>
              <dt className="text-muted">Perusahaan</dt>
              <dd>{me?.company.name}</dd>
              <dt className="text-muted">Role</dt>
              <dd className="flex flex-wrap gap-1">
                {me?.roles.map((r) => (
                  <Badge key={r} tone="primary">
                    {r}
                  </Badge>
                ))}
              </dd>
            </dl>
          </CardContent>
        </Card>
        <Card>
          <CardHeader>
            <CardTitle>Ganti password</CardTitle>
          </CardHeader>
          <CardContent>
            <form onSubmit={onSubmit} className="space-y-4" noValidate>
              <FormField label="Password lama" htmlFor="currentPassword" error={errors.currentPassword?.message}>
                <PasswordInput id="currentPassword" autoComplete="current-password" {...register("currentPassword")} />
              </FormField>
              <FormField label="Password baru" htmlFor="newPassword" error={errors.newPassword?.message}>
                <PasswordInput id="newPassword" autoComplete="new-password" {...register("newPassword")} />
                <PasswordStrength value={newPassword} />
              </FormField>
              <FormField label="Konfirmasi password baru" htmlFor="confirmPassword" error={errors.confirmPassword?.message}>
                <PasswordInput id="confirmPassword" autoComplete="new-password" {...register("confirmPassword")} />
              </FormField>
              <Button type="submit" loading={isSubmitting}>
                Simpan password
              </Button>
            </form>
          </CardContent>
        </Card>
      </div>
    </>
  );
}
