"use client";

import { PageHeader } from "@/components/shared/page-header";
import { RequirePermission } from "@/features/auth/require-permission";
import { JournalEditor } from "@/features/ledger/journal-editor";

export default function NewJournalPage() {
  return (
    <RequirePermission permission="LEDGER:CREATE">
      <PageHeader title="Jurnal Baru" description="Jurnal manual dengan validasi debit = kredit." />
      <JournalEditor />
    </RequirePermission>
  );
}
