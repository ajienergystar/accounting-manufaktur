"use client";

import { z } from "zod";
import type { ColumnDef } from "@tanstack/react-table";
import { CrudPage, type FieldDef } from "@/components/shared/crud-page";
import { codeField, nameField, optionalText } from "@/features/master/schemas";

interface Warehouse {
  id: number;
  code: string;
  name: string;
  address?: string;
  active: boolean;
}

const schema = z.object({ code: codeField(), name: nameField(100), address: optionalText(255), active: z.boolean() });
type Form = z.infer<typeof schema>;

const fields: FieldDef<Form>[] = [
  { name: "code", label: "Kode", required: true, placeholder: "GBB" },
  { name: "name", label: "Nama gudang", required: true, placeholder: "Gudang Bahan Baku" },
  { name: "address", label: "Alamat / lokasi", type: "textarea", span: 2 },
  { name: "active", label: "Aktif", type: "checkbox" },
];

const columns: ColumnDef<Warehouse, unknown>[] = [
  { accessorKey: "code", header: "Kode" },
  { accessorKey: "name", header: "Nama" },
  { accessorKey: "address", header: "Alamat" },
];

export default function WarehousesPage() {
  return (
    <CrudPage<Warehouse, Form>
      title="Gudang"
      description="Lokasi penyimpanan persediaan (multi-gudang)."
      endpoint="/warehouses"
      entityLabel="Gudang"
      columns={columns}
      fields={fields}
      schema={schema}
      dialogSize="md"
      emptyValues={{ code: "", name: "", address: "", active: true }}
      toForm={(w) => ({ ...w, address: w.address ?? "" })}
    />
  );
}
