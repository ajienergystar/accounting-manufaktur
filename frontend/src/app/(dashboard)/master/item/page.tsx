"use client";

import { z } from "zod";
import type { ColumnDef } from "@tanstack/react-table";
import { CrudPage, type FieldDef } from "@/components/shared/crud-page";
import { Badge } from "@/components/ui/badge";
import { Select } from "@/components/ui/select";
import { codeField, decimalString, nameField, optionalText } from "@/features/master/schemas";
import { useOptions } from "@/features/master/use-options";
import { ITEM_TYPES, ITEM_TYPE_TONE as TYPE_TONE, type ItemType } from "@/features/master/item-types";
import { formatAmount, formatQty, formatRupiah } from "@/lib/format";

interface Item {
  id: number;
  code: string;
  name: string;
  itemType: ItemType;
  categoryId: number | null;
  categoryName: string | null;
  uomId: number;
  uomCode: string;
  standardCost: string;
  sellingPrice: string;
  minStock: string;
  reorderPoint: string;
  description?: string;
  active: boolean;
}

const schema = z.object({
  code: codeField(30),
  name: nameField(),
  itemType: z.enum(
    ["RAW_MATERIAL", "SUPPORTING_MATERIAL", "WORK_IN_PROCESS", "FINISHED_GOOD", "SUPPLIES", "SERVICE"],
    { message: "Tipe item wajib dipilih" },
  ),
  categoryId: z.string().nullable().optional(),
  uomId: z.string({ message: "Satuan wajib dipilih" }).min(1, "Satuan wajib dipilih"),
  standardCost: decimalString,
  sellingPrice: decimalString,
  minStock: decimalString,
  reorderPoint: decimalString,
  description: optionalText(500),
  active: z.boolean(),
});
type Form = z.infer<typeof schema>;

const columns: ColumnDef<Item, unknown>[] = [
  { accessorKey: "code", header: "Kode" },
  { accessorKey: "name", header: "Nama" },
  {
    header: "Tipe",
    cell: ({ row }) => <Badge tone={TYPE_TONE[row.original.itemType]}>{ITEM_TYPES[row.original.itemType]}</Badge>,
  },
  { accessorKey: "uomCode", header: "Satuan" },
  { header: "Biaya Standar", meta: { align: "right" }, cell: ({ row }) => formatAmount(row.original.standardCost) },
  { header: "Harga Jual", meta: { align: "right" }, cell: ({ row }) => formatRupiah(row.original.sellingPrice) },
  { header: "Stok Min.", meta: { align: "right" }, cell: ({ row }) => formatQty(row.original.minStock) },
];

export default function ItemsPage() {
  const { data: uoms } = useOptions("/uoms");
  const { data: categories } = useOptions("/item-categories");

  const fields: FieldDef<Form>[] = [
    { name: "code", label: "Kode", required: true, placeholder: "BB-0001" },
    { name: "name", label: "Nama item", required: true },
    {
      name: "itemType",
      label: "Tipe",
      type: "select",
      required: true,
      options: Object.entries(ITEM_TYPES).map(([value, label]) => ({ value, label })),
      hint: "Menentukan akun persediaan yang dipakai di jurnal otomatis.",
    },
    {
      name: "uomId",
      label: "Satuan",
      type: "select",
      required: true,
      options: uoms?.map((u) => ({ value: String(u.id), label: `${u.code} — ${u.name}` })) ?? [],
    },
    {
      name: "categoryId",
      label: "Kategori",
      type: "select",
      allowEmpty: true,
      options: categories?.map((c) => ({ value: String(c.id), label: c.name })) ?? [],
    },
    { name: "standardCost", label: "Biaya standar / unit", type: "money", hint: "Untuk standard costing" },
    { name: "sellingPrice", label: "Harga jual / unit", type: "money" },
    { name: "minStock", label: "Stok minimum", type: "number" },
    { name: "reorderPoint", label: "Reorder point", type: "number" },
    { name: "description", label: "Deskripsi", type: "textarea", span: 2 },
    { name: "active", label: "Aktif", type: "checkbox" },
  ];

  return (
    <CrudPage<Item, Form>
      title="Item / Barang"
      description="Bahan baku, bahan penolong, barang jadi, supplies, dan jasa."
      endpoint="/items"
      listEndpoint="/items/search"
      entityLabel="Item"
      columns={columns}
      fields={fields}
      schema={schema}
      emptyValues={{
        code: "",
        name: "",
        itemType: "RAW_MATERIAL",
        categoryId: null,
        uomId: "",
        standardCost: "0",
        sellingPrice: "0",
        minStock: "0",
        reorderPoint: "0",
        description: "",
        active: true,
      }}
      toForm={(i) => ({
        ...i,
        categoryId: i.categoryId ? String(i.categoryId) : null,
        uomId: String(i.uomId),
        description: i.description ?? "",
      })}
      toPayload={(v) => ({
        ...v,
        categoryId: v.categoryId ? Number(v.categoryId) : null,
        uomId: Number(v.uomId),
        standardCost: v.standardCost || "0",
        sellingPrice: v.sellingPrice || "0",
        minStock: v.minStock || "0",
        reorderPoint: v.reorderPoint || "0",
      })}
      extraFilters={(set, values) => (
        <Select
          className="sm:w-48"
          value={values.type ?? ""}
          onChange={(e) => set("type", e.target.value)}
          aria-label="Filter tipe item"
        >
          <option value="">Semua tipe</option>
          {Object.entries(ITEM_TYPES).map(([v, l]) => (
            <option key={v} value={v}>
              {l}
            </option>
          ))}
        </Select>
      )}
    />
  );
}
