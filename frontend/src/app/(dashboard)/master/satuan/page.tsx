"use client";

import { useState } from "react";
import { z } from "zod";
import type { ColumnDef } from "@tanstack/react-table";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { ArrowRight, Plus, Trash2 } from "lucide-react";
import { toast } from "sonner";
import { CrudPage, type FieldDef } from "@/components/shared/crud-page";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { MoneyInput } from "@/components/ui/money-input";
import { Select } from "@/components/ui/select";
import { Can } from "@/features/auth/require-permission";
import { codeField, nameField } from "@/features/master/schemas";
import { useOptions } from "@/features/master/use-options";
import { api, errorMessage } from "@/lib/api";
import { formatQty } from "@/lib/format";

interface Uom {
  id: number;
  code: string;
  name: string;
  active: boolean;
}

interface Conversion {
  id: number;
  fromUomId: number;
  fromUomCode: string;
  toUomId: number;
  toUomCode: string;
  factor: string;
}

const schema = z.object({ code: codeField(), name: nameField(100), active: z.boolean() });
type Form = z.infer<typeof schema>;

const fields: FieldDef<Form>[] = [
  { name: "code", label: "Kode", required: true, placeholder: "KG" },
  { name: "name", label: "Nama satuan", required: true, placeholder: "Kilogram" },
  { name: "active", label: "Aktif", type: "checkbox" },
];

const columns: ColumnDef<Uom, unknown>[] = [
  { accessorKey: "code", header: "Kode" },
  { accessorKey: "name", header: "Nama" },
];

function Conversions() {
  const qc = useQueryClient();
  const { data: uoms } = useOptions("/uoms");
  const { data } = useQuery({ queryKey: ["/uoms", "conversions"], queryFn: () => api.get<Conversion[]>("/uoms/conversions") });
  const [from, setFrom] = useState("");
  const [to, setTo] = useState("");
  const [factor, setFactor] = useState("");

  const save = useMutation({
    mutationFn: () => api.post("/uoms/conversions", { fromUomId: Number(from), toUomId: Number(to), factor }),
    onSuccess: () => {
      toast.success("Konversi disimpan");
      setFactor("");
      qc.invalidateQueries({ queryKey: ["/uoms", "conversions"] });
    },
    onError: (e) => toast.error(errorMessage(e)),
  });
  const remove = useMutation({
    mutationFn: (id: number) => api.delete(`/uoms/conversions/${id}`),
    onSuccess: () => qc.invalidateQueries({ queryKey: ["/uoms", "conversions"] }),
    onError: (e) => toast.error(errorMessage(e)),
  });

  return (
    <Card className="mt-6">
      <CardHeader>
        <div>
          <CardTitle>Konversi satuan</CardTitle>
          <p className="mt-0.5 text-sm text-muted">Contoh: 1 BOX = 12 PCS. Konversi kebalikannya dihitung otomatis.</p>
        </div>
      </CardHeader>
      <CardContent>
        <Can permission="MASTER:EDIT">
          <div className="mb-4 flex flex-wrap items-end gap-2">
            <span className="pb-2 text-sm">1</span>
            <Select className="w-32" value={from} onChange={(e) => setFrom(e.target.value)} aria-label="Satuan asal">
              <option value="">Dari</option>
              {uoms?.map((u) => (
                <option key={u.id} value={u.id}>
                  {u.code}
                </option>
              ))}
            </Select>
            <span className="pb-2 text-sm">=</span>
            <MoneyInput className="w-32" value={factor} onValueChange={setFactor} aria-label="Faktor" placeholder="Faktor" />
            <Select className="w-32" value={to} onChange={(e) => setTo(e.target.value)} aria-label="Satuan tujuan">
              <option value="">Ke</option>
              {uoms?.map((u) => (
                <option key={u.id} value={u.id}>
                  {u.code}
                </option>
              ))}
            </Select>
            <Button onClick={() => save.mutate()} loading={save.isPending} disabled={!from || !to || !factor}>
              <Plus className="h-4 w-4" /> Simpan
            </Button>
          </div>
        </Can>
        {data && data.length > 0 ? (
          <ul className="divide-y divide-border rounded-lg border border-border">
            {data.map((c) => (
              <li key={c.id} className="flex items-center justify-between px-4 py-2 text-sm">
                <span className="flex items-center gap-2">
                  1 <b>{c.fromUomCode}</b> <ArrowRight className="h-3 w-3 text-muted" /> {formatQty(c.factor)}{" "}
                  <b>{c.toUomCode}</b>
                </span>
                <Can permission="MASTER:DELETE">
                  <Button size="icon" variant="ghost" onClick={() => remove.mutate(c.id)} aria-label="Hapus konversi">
                    <Trash2 className="h-4 w-4 text-danger" />
                  </Button>
                </Can>
              </li>
            ))}
          </ul>
        ) : (
          <p className="text-sm text-muted">Belum ada konversi.</p>
        )}
      </CardContent>
    </Card>
  );
}

export default function UomsPage() {
  return (
    <>
      <CrudPage<Uom, Form>
        title="Satuan"
        description="Satuan ukur item (Unit of Measure)."
        endpoint="/uoms"
        entityLabel="Satuan"
        columns={columns}
        fields={fields}
        schema={schema}
        dialogSize="md"
        emptyValues={{ code: "", name: "", active: true }}
        toForm={(u) => u}
      />
      <Conversions />
    </>
  );
}
