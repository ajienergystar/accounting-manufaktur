"use client";

import { z } from "zod";
import type { ColumnDef } from "@tanstack/react-table";
import { CrudPage, type FieldDef } from "@/components/shared/crud-page";
import { Badge } from "@/components/ui/badge";
import { codeField, nameField } from "@/features/master/schemas";

const TYPES = {
  PRODUCTION: "Produksi",
  SERVICE: "Pendukung (jasa pabrik)",
  NON_PRODUCTION: "Non-produksi",
} as const;
type DeptType = keyof typeof TYPES;

interface Department {
  id: number;
  code: string;
  name: string;
  deptType: DeptType;
  active: boolean;
}

const schema = z.object({
  code: codeField(),
  name: nameField(100),
  deptType: z.enum(["PRODUCTION", "SERVICE", "NON_PRODUCTION"], { message: "Tipe wajib dipilih" }),
  active: z.boolean(),
});
type Form = z.infer<typeof schema>;

const fields: FieldDef<Form>[] = [
  { name: "code", label: "Kode", required: true, placeholder: "PRD-A" },
  { name: "name", label: "Nama departemen", required: true },
  {
    name: "deptType",
    label: "Tipe",
    type: "select",
    required: true,
    span: 2,
    options: Object.entries(TYPES).map(([value, label]) => ({ value, label })),
    hint: "Biaya departemen pendukung dialokasikan ke departemen produksi.",
  },
  { name: "active", label: "Aktif", type: "checkbox" },
];

const columns: ColumnDef<Department, unknown>[] = [
  { accessorKey: "code", header: "Kode" },
  { accessorKey: "name", header: "Nama" },
  {
    header: "Tipe",
    cell: ({ row }) => (
      <Badge tone={row.original.deptType === "PRODUCTION" ? "primary" : row.original.deptType === "SERVICE" ? "info" : "neutral"}>
        {TYPES[row.original.deptType]}
      </Badge>
    ),
  },
];

export default function DepartmentsPage() {
  return (
    <CrudPage<Department, Form>
      title="Departemen / Cost Center"
      description="Departemen produksi, pendukung, dan non-produksi untuk pembebanan biaya."
      endpoint="/departments"
      entityLabel="Departemen"
      columns={columns}
      fields={fields}
      schema={schema}
      dialogSize="md"
      emptyValues={{ code: "", name: "", deptType: "PRODUCTION", active: true }}
      toForm={(d) => d}
    />
  );
}
