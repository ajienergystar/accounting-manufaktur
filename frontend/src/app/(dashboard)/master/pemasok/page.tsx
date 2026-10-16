"use client";

import { z } from "zod";
import type { ColumnDef } from "@tanstack/react-table";
import { CrudPage, type FieldDef } from "@/components/shared/crud-page";
import { codeField, emptyToNull, nameField, optionalText } from "@/features/master/schemas";

interface Supplier {
  id: number;
  code: string;
  name: string;
  npwp?: string;
  address?: string;
  city?: string;
  phone?: string;
  email?: string;
  contactPerson?: string;
  paymentTermDays: number;
  active: boolean;
}

const schema = z.object({
  code: codeField(),
  name: nameField(),
  npwp: optionalText(30),
  address: optionalText(500),
  city: optionalText(100),
  phone: optionalText(30),
  email: z.union([z.literal(""), z.email("Format email tidak valid")]).optional(),
  contactPerson: optionalText(100),
  paymentTermDays: z.string().regex(/^\d{1,3}$/, "Isi jumlah hari 0-365"),
  active: z.boolean(),
});
type Form = z.infer<typeof schema>;

const fields: FieldDef<Form>[] = [
  { name: "code", label: "Kode", required: true, placeholder: "SUP-001" },
  { name: "name", label: "Nama pemasok", required: true },
  { name: "npwp", label: "NPWP" },
  { name: "contactPerson", label: "Kontak person" },
  { name: "phone", label: "Telepon" },
  { name: "email", label: "Email", type: "email" },
  { name: "city", label: "Kota" },
  { name: "paymentTermDays", label: "Termin pembayaran (hari)", required: true },
  { name: "address", label: "Alamat", type: "textarea", span: 2 },
  { name: "active", label: "Aktif", type: "checkbox" },
];

const columns: ColumnDef<Supplier, unknown>[] = [
  { accessorKey: "code", header: "Kode" },
  { accessorKey: "name", header: "Nama" },
  { accessorKey: "city", header: "Kota" },
  { accessorKey: "phone", header: "Telepon" },
  { accessorKey: "npwp", header: "NPWP" },
  { header: "Termin", cell: ({ row }) => `${row.original.paymentTermDays} hari` },
];

export default function SuppliersPage() {
  return (
    <CrudPage<Supplier, Form>
      title="Pemasok"
      description="Data pemasok bahan baku, bahan penolong, dan jasa."
      endpoint="/suppliers"
      entityLabel="Pemasok"
      columns={columns}
      fields={fields}
      schema={schema}
      emptyValues={{ code: "", name: "", paymentTermDays: "30", active: true }}
      toForm={(s) => ({
        ...s,
        npwp: s.npwp ?? "",
        address: s.address ?? "",
        city: s.city ?? "",
        phone: s.phone ?? "",
        email: s.email ?? "",
        contactPerson: s.contactPerson ?? "",
        paymentTermDays: String(s.paymentTermDays),
      })}
      toPayload={(v) => ({ ...v, email: emptyToNull(v.email), paymentTermDays: Number(v.paymentTermDays) })}
    />
  );
}
