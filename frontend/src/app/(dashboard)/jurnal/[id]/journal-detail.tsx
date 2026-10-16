"use client";

import Link from "next/link";
import { useParams, useRouter } from "next/navigation";
import { useState } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { ArrowLeft, Send, Trash2 } from "lucide-react";
import { toast } from "sonner";
import { PageHeader } from "@/components/shared/page-header";
import { ErrorState } from "@/components/shared/states";
import { Button, buttonClasses } from "@/components/ui/button";
import { ConfirmDialog } from "@/components/ui/confirm-dialog";
import { Skeleton } from "@/components/ui/skeleton";
import { useAuth } from "@/features/auth/auth-context";
import { RequirePermission } from "@/features/auth/require-permission";
import { JournalEditor } from "@/features/ledger/journal-editor";
import { JournalView } from "@/features/ledger/journal-view";
import type { JournalDetail } from "@/features/ledger/types";
import { api, errorMessage } from "@/lib/api";

function Detail() {
  const { id } = useParams<{ id: string }>();
  const router = useRouter();
  const qc = useQueryClient();
  const { can } = useAuth();
  const [confirm, setConfirm] = useState<"post" | "delete" | null>(null);
  const { data, isLoading, error, refetch } = useQuery({
    queryKey: ["journal", Number(id)],
    queryFn: () => api.get<JournalDetail>(`/journals/${id}`),
  });

  const action = useMutation({
    mutationFn: (a: "post" | "delete") => (a === "post" ? api.post(`/journals/${id}/post`) : api.delete(`/journals/${id}`)),
    onSuccess: (_r, a) => {
      qc.invalidateQueries({ queryKey: ["journals"] });
      if (a === "delete") {
        toast.success("Draft dihapus");
        router.push("/jurnal");
      } else {
        toast.success("Jurnal diposting");
        setConfirm(null);
        refetch();
      }
    },
    onError: (e) => toast.error(errorMessage(e)),
  });

  if (isLoading) return <Skeleton className="h-96 w-full" />;
  if (error || !data) return <ErrorState message={errorMessage(error)} onRetry={refetch} />;

  const isDraft = data.status === "DRAFT";
  return (
    <>
      <PageHeader
        title={isDraft ? "Draft Jurnal" : `Jurnal ${data.journalNo}`}
        description={data.description}
        actions={
          <>
            <Link href="/jurnal" className={buttonClasses("ghost")}>
              <ArrowLeft className="h-4 w-4" /> Daftar jurnal
            </Link>
            {isDraft && can("LEDGER:DELETE") && (
              <Button variant="outline" onClick={() => setConfirm("delete")}>
                <Trash2 className="h-4 w-4 text-danger" /> Hapus draft
              </Button>
            )}
            {isDraft && can("LEDGER:POST") && (
              <Button onClick={() => setConfirm("post")}>
                <Send className="h-4 w-4" /> Posting
              </Button>
            )}
          </>
        }
      />
      {isDraft && can("LEDGER:EDIT") ? <JournalEditor journal={data} /> : <JournalView journal={data} />}
      <ConfirmDialog
        open={confirm !== null}
        title={confirm === "post" ? "Posting jurnal?" : "Hapus draft jurnal?"}
        message={
          confirm === "post"
            ? "Jurnal yang sudah diposting tidak dapat diubah. Koreksi hanya lewat jurnal pembalik."
            : "Draft akan dihapus permanen."
        }
        confirmLabel={confirm === "post" ? "Posting" : "Hapus"}
        tone={confirm === "delete" ? "danger" : "primary"}
        loading={action.isPending}
        onConfirm={() => confirm && action.mutate(confirm)}
        onClose={() => setConfirm(null)}
      />
    </>
  );
}

export function JournalDetailPage() {
  return (
    <RequirePermission permission="LEDGER:VIEW">
      <Detail />
    </RequirePermission>
  );
}
