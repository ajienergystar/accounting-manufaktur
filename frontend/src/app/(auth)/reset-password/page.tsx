import type { Metadata } from "next";
import { Suspense } from "react";
import { ResetPasswordForm } from "./reset-form";

export const metadata: Metadata = { title: "Reset Password" };

export default function ResetPasswordPage() {
  return (
    <Suspense>
      <ResetPasswordForm />
    </Suspense>
  );
}
