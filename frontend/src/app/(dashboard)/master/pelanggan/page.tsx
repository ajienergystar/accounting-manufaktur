"use client";

import { z } from "zod";
import type { ColumnDef } from "@tanstack/react-table";
import { CrudPage, type FieldDef } from "@/components/shared/crud-page";
import { codeField, decimalString, emptyToNull, nameField, optionalText } from "@/features/master/schemas";
import { formatRupiah } from "@/lib/format";

interface Customer {
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
  creditLimit: string;
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
  creditLimit: decimalString,
  active: z.boolean(),
});
type Form = z.infer<typeof schema>;

const fields: FieldDef<Form>[] = [
  { name: "code", label: "Kode", required: true, placeholder: "CUST-001" },
  { name: "name", label: "Nama pelanggan", required: true },
  { name: "npwp", label: "NPWP" },
  { name: "contactPerson", label: "Kontak person" },
  { name: "phone", label: "Telepon" },
  { name: "email", label: "Email", type: "email" },
  { name: "city", label: "Kota" },
  { name: "paymentTermDays", label: "Termin pembayaran (hari)", required: true },
  { name: "address", label: "Alamat", type: "textarea", span: 2 },
  { name: "creditLimit", label: "Limit kredit (Rp)", type: "money", hint: "0 = tanpa batas" },
  { name: "active", label: "Aktif", type: "checkbox" },
];

const columns: ColumnDef<Customer, unknown>[] = [
  { accessorKey: "code", header: "Kode" },
  { accessorKey: "name", header: "Nama" },
  { accessorKey: "city", header: "Kota" },
  { accessorKey: "phone", header: "Telepon" },
  { header: "Termin", cell: ({ row }) => `${row.original.paymentTermDays} hari` },
  { header: "Limit Kredit", meta: { align: "right" }, cell: ({ row }) => formatRupiah(row.original.creditLimit) },
];

export default function CustomersPage() {
  return (
    <CrudPage<Customer, Form>
      title="Pelanggan"
      description="Data pelanggan, termin pembayaran, dan limit kredit."
      endpoint="/customers"
      entityLabel="Pelanggan"
      columns={columns}
      fields={fields}
      schema={schema}
      emptyValues={{ code: "", name: "", paymentTermDays: "30", creditLimit: "0", active: true }}
      toForm={(c) => ({
        ...c,
        npwp: c.npwp ?? "",
        address: c.address ?? "",
        city: c.city ?? "",
        phone: c.phone ?? "",
        email: c.email ?? "",
        contactPerson: c.contactPerson ?? "",
        paymentTermDays: String(c.paymentTermDays),
        creditLimit: c.creditLimit,
      })}
      toPayload={(v) => ({
        ...v,
        email: emptyToNull(v.email),
        paymentTermDays: Number(v.paymentTermDays),
        creditLimit: v.creditLimit || "0",
      })}
    />
  );
}
