"use client";

import { useState } from "react";
import { Button } from "@/components/ui/button";
import { Dialog } from "@/components/ui/dialog";
import { FormField } from "@/components/ui/form-field";
import { Input } from "@/components/ui/input";
import { Textarea } from "@/components/ui/textarea";
import { todayIso } from "@/lib/format";

interface VoidDialogProps {
  open: boolean;
  title?: string;
  description?: string;
  loading?: boolean;
  onClose: () => void;
  onConfirm: (v: { date: string; reason: string }) => void;
}

/** Dialog pembatalan dokumen: tanggal pembatalan + alasan (jurnal pembalik dibuat otomatis). */
export function VoidDialog({
  open,
  title = "Batalkan dokumen",
  description = "Mutasi stok & jurnal dokumen ini akan dibalik otomatis pada tanggal pembatalan.",
  loading,
  onClose,
  onConfirm,
}: VoidDialogProps) {
  const [date, setDate] = useState(todayIso());
  const [reason, setReason] = useState("");
  return (
    <Dialog
      open={open}
      onClose={onClose}
      title={title}
      description={description}
      footer={
        <>
          <Button variant="outline" onClick={onClose}>
            Batal
          </Button>
          <Button variant="danger" loading={loading} disabled={!reason.trim()} onClick={() => onConfirm({ date, reason })}>
            Batalkan dokumen
          </Button>
        </>
      }
    >
      <div className="space-y-4">
        <FormField label="Tanggal pembatalan" htmlFor="void-date">
          <Input id="void-date" type="date" value={date} onChange={(e) => setDate(e.target.value)} />
        </FormField>
        <FormField label="Alasan" htmlFor="void-reason" required>
          <Textarea id="void-reason" value={reason} onChange={(e) => setReason(e.target.value)} />
        </FormField>
      </div>
    </Dialog>
  );
}
