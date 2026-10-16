"use client";

import { useRef } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { Download, FileText, Paperclip, Trash2, Upload } from "lucide-react";
import { toast } from "sonner";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { api, downloadFile, errorMessage } from "@/lib/api";
import { formatDateTime } from "@/lib/format";

interface AttachmentRow {
  id: number;
  fileName: string;
  contentType: string;
  sizeBytes: number;
  uploadedBy: string;
  uploadedAt: string;
}

function size(b: number) {
  if (b < 1024) return `${b} B`;
  if (b < 1024 * 1024) return `${(b / 1024).toFixed(0)} KB`;
  return `${(b / 1024 / 1024).toFixed(1)} MB`;
}

/** Daftar & upload lampiran bukti transaksi untuk entitas tertentu. */
export function Attachments({ entityType, entityId, canEdit }: { entityType: string; entityId: number; canEdit: boolean }) {
  const qc = useQueryClient();
  const inputRef = useRef<HTMLInputElement>(null);
  const key = ["attachments", entityType, entityId];
  const { data } = useQuery({ queryKey: key, queryFn: () => api.get<AttachmentRow[]>("/attachments", { entityType, entityId }) });

  const upload = useMutation({
    mutationFn: (file: File) => {
      const fd = new FormData();
      fd.append("file", file);
      return api.post(`/attachments?entityType=${entityType}&entityId=${entityId}`, fd);
    },
    onSuccess: () => {
      toast.success("Lampiran diunggah");
      qc.invalidateQueries({ queryKey: key });
    },
    onError: (e) => toast.error(errorMessage(e)),
  });
  const remove = useMutation({
    mutationFn: (id: number) => api.delete(`/attachments/${id}`),
    onSuccess: () => qc.invalidateQueries({ queryKey: key }),
    onError: (e) => toast.error(errorMessage(e)),
  });

  return (
    <Card className="no-print">
      <CardHeader>
        <CardTitle className="flex items-center gap-2">
          <Paperclip className="h-4 w-4" /> Lampiran
        </CardTitle>
        {canEdit && (
          <>
            <input
              ref={inputRef}
              type="file"
              className="hidden"
              accept=".pdf,.png,.jpg,.jpeg,.webp,.xlsx,.xls,.docx,.csv,.txt"
              onChange={(e) => {
                const f = e.target.files?.[0];
                if (f) upload.mutate(f);
                e.target.value = "";
              }}
            />
            <Button size="sm" variant="outline" onClick={() => inputRef.current?.click()} loading={upload.isPending}>
              <Upload className="h-4 w-4" /> Unggah
            </Button>
          </>
        )}
      </CardHeader>
      <CardContent>
        {!data?.length ? (
          <p className="text-sm text-muted">Belum ada lampiran. Maks. 10 MB (PDF, gambar, Excel, Word, CSV).</p>
        ) : (
          <ul className="divide-y divide-border">
            {data.map((a) => (
              <li key={a.id} className="flex items-center gap-3 py-2 text-sm">
                <FileText className="h-4 w-4 shrink-0 text-muted" />
                <span className="min-w-0 flex-1">
                  <span className="block truncate font-medium">{a.fileName}</span>
                  <span className="text-xs text-muted">
                    {size(a.sizeBytes)} · {a.uploadedBy} · {formatDateTime(a.uploadedAt)}
                  </span>
                </span>
                <Button
                  size="icon"
                  variant="ghost"
                  aria-label="Unduh"
                  onClick={() => downloadFile(`/attachments/${a.id}/download`, {}, a.fileName).catch((e) => toast.error(errorMessage(e)))}
                >
                  <Download className="h-4 w-4" />
                </Button>
                {canEdit && (
                  <Button size="icon" variant="ghost" aria-label="Hapus" onClick={() => remove.mutate(a.id)}>
                    <Trash2 className="h-4 w-4 text-danger" />
                  </Button>
                )}
              </li>
            ))}
          </ul>
        )}
      </CardContent>
    </Card>
  );
}
