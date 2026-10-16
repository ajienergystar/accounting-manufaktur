"use client";

import { z } from "zod";
import type { ColumnDef } from "@tanstack/react-table";
import { CrudPage, type FieldDef } from "@/components/shared/crud-page";
import { Badge } from "@/components/ui/badge";
import { codeField, decimalString, nameField } from "@/features/master/schemas";
import { usePostableAccounts } from "@/features/master/use-options";

interface TaxRate {
  id: number;
  code: string;
  name: string;
  taxType: "PPN" | "PPH21" | "PPH22" | "PPH23";
  rate: string;
  accountId: number | null;
  accountCode: string | null;
  accountName: string | null;
  active: boolean;
}

const schema = z.object({
  code: codeField(),
  name: nameField(100),
  taxType: z.enum(["PPN", "PPH21", "PPH22", "PPH23"], { message: "Jenis pajak wajib dipilih" }),
  rate: decimalString.refine((v) => v !== undefined && v !== "" && Number(v) <= 100, "Tarif 0-100%"),
  accountId: z.string().nullable().optional(),
  active: z.boolean(),
});
type Form = z.infer<typeof schema>;

const columns: ColumnDef<TaxRate, unknown>[] = [
  { accessorKey: "code", header: "Kode" },
  { accessorKey: "name", header: "Nama" },
  { header: "Jenis", cell: ({ row }) => <Badge tone="info">{row.original.taxType}</Badge> },
  { header: "Tarif", meta: { align: "right" }, cell: ({ row }) => `${Number(row.original.rate).toLocaleString("id-ID")}%` },
  {
    header: "Akun khusus",
    cell: ({ row }) =>
      row.original.accountCode ? `${row.original.accountCode} ${row.original.accountName}` : (
        <span className="text-muted">Default (account mapping)</span>
      ),
  },
];

export default function TaxRatesPage() {
  const { data: accounts } = usePostableAccounts();
  const fields: FieldDef<Form>[] = [
    { name: "code", label: "Kode", required: true, placeholder: "PPN11" },
    { name: "name", label: "Nama", required: true },
    {
      name: "taxType",
      label: "Jenis pajak",
      type: "select",
      required: true,
      options: [
        { value: "PPN", label: "PPN" },
        { value: "PPH21", label: "PPh 21" },
        { value: "PPH22", label: "PPh 22" },
        { value: "PPH23", label: "PPh 23" },
      ],
    },
    { name: "rate", label: "Tarif (%)", type: "number", required: true },
    {
      name: "accountId",
      label: "Akun khusus (opsional)",
      type: "select",
      span: 2,
      allowEmpty: true,
      hint: "Kosongkan untuk memakai akun default dari Account Mapping.",
      options: accounts?.map((a) => ({ value: String(a.id), label: `${a.code} — ${a.name}` })) ?? [],
    },
    { name: "active", label: "Aktif", type: "checkbox" },
  ];

  return (
    <CrudPage<TaxRate, Form>
      title="Pajak"
      description="Tarif PPN dan PPh yang dipakai di faktur pembelian & penjualan."
      endpoint="/tax-rates"
      entityLabel="Pajak"
      columns={columns}
      fields={fields}
      schema={schema}
      emptyValues={{ code: "", name: "", taxType: "PPN", rate: "11", accountId: null, active: true }}
      toForm={(t) => ({ ...t, accountId: t.accountId ? String(t.accountId) : null })}
      toPayload={(v) => ({ ...v, accountId: v.accountId ? Number(v.accountId) : null })}
    />
  );
}
