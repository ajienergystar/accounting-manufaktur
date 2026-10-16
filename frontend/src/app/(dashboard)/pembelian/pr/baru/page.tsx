"use client";

import { PageHeader } from "@/components/shared/page-header";
import { RequirePermission } from "@/features/auth/require-permission";
import { PrForm } from "@/features/purchasing/pr-form";

export default function Page() {
  return (
    <RequirePermission permission="PURCHASE:CREATE">
      <PageHeader title="Permintaan Pembelian Baru" />
      <PrForm />
    </RequirePermission>
  );
}
