"use client";

import { useMemo, useState, type ReactNode } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { Controller, useForm, type DefaultValues, type FieldValues, type Path, type Resolver } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import type { ColumnDef } from "@tanstack/react-table";
import type { ZodType } from "zod";
import { Pencil, Plus, Search, Trash2 } from "lucide-react";
import { toast } from "sonner";
import { PageHeader } from "./page-header";
import { DataTable } from "./data-table";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { Card } from "@/components/ui/card";
import { ConfirmDialog } from "@/components/ui/confirm-dialog";
import { Dialog } from "@/components/ui/dialog";
import { FormField } from "@/components/ui/form-field";
import { Input } from "@/components/ui/input";
import { MoneyInput } from "@/components/ui/money-input";
import { Select } from "@/components/ui/select";
import { Textarea } from "@/components/ui/textarea";
import { useAuth } from "@/features/auth/auth-context";
import { RequirePermission } from "@/features/auth/require-permission";
import { api, errorMessage } from "@/lib/api";
import { applyServerErrors } from "@/lib/form-errors";
import { cn } from "@/lib/utils";
import type { PageResponse } from "@/types/common";

export interface SelectOption {
  value: string;
  label: string;
}

export interface FieldDef<F> {
  name: Path<F>;
  label: string;
  type?: "text" | "email" | "textarea" | "number" | "money" | "select" | "checkbox";
  required?: boolean;
  placeholder?: string;
  hint?: string;
  /** Lebar kolom di grid 2 kolom. */
  span?: 1 | 2;
  options?: SelectOption[];
  /** Kosongkan pilihan → null. */
  allowEmpty?: boolean;
  disabledOnEdit?: boolean;
}

interface CrudPageProps<T extends { id: number; active?: boolean }, F extends FieldValues> {
  title: string;
  description?: string;
  /** Endpoint resource, contoh "/customers". */
  endpoint: string;
  /** Endpoint list (default = endpoint). */
  listEndpoint?: string;
  entityLabel: string;
  permissionPrefix?: string;
  columns: ColumnDef<T, unknown>[];
  fields: FieldDef<F>[];
  schema: ZodType<F, F>;
  emptyValues: DefaultValues<F>;
  toForm: (row: T) => DefaultValues<F>;
  toPayload?: (values: F) => unknown;
  extraFilters?: (set: (k: string, v: string) => void, values: Record<string, string>) => ReactNode;
  dialogSize?: "md" | "lg";
}

export function CrudPage<T extends { id: number; active?: boolean }, F extends FieldValues>(props: CrudPageProps<T, F>) {
  return (
    <RequirePermission permission={`${props.permissionPrefix ?? "MASTER"}:VIEW`}>
      <CrudPageInner {...props} />
    </RequirePermission>
  );
}

function CrudPageInner<T extends { id: number; active?: boolean }, F extends FieldValues>({
  title,
  description,
  endpoint,
  listEndpoint,
  entityLabel,
  permissionPrefix = "MASTER",
  columns,
  fields,
  schema,
  emptyValues,
  toForm,
  toPayload,
  extraFilters,
  dialogSize = "lg",
}: CrudPageProps<T, F>) {
  const { can } = useAuth();
  const qc = useQueryClient();
  const [q, setQ] = useState("");
  const [active, setActive] = useState<"" | "true" | "false">("true");
  const [filters, setFilters] = useState<Record<string, string>>({});
  const [page, setPage] = useState(0);
  const [editing, setEditing] = useState<T | "new" | null>(null);
  const [deleting, setDeleting] = useState<T | null>(null);

  const list = useQuery({
    queryKey: [endpoint, "list", { q, active, page, filters }],
    queryFn: () =>
      api.get<PageResponse<T>>(listEndpoint ?? endpoint, { q, active, page, size: 20, ...filters }),
    placeholderData: (prev) => prev,
  });

  const invalidate = () => {
    qc.invalidateQueries({ queryKey: [endpoint] });
    qc.invalidateQueries({ queryKey: ["options", endpoint] });
  };

  const remove = useMutation({
    mutationFn: (id: number) => api.delete(`${endpoint}/${id}`),
    onSuccess: () => {
      toast.success(`${entityLabel} dihapus`);
      setDeleting(null);
      invalidate();
    },
    onError: (e) => toast.error(errorMessage(e)),
  });

  const allColumns = useMemo<ColumnDef<T, unknown>[]>(
    () => [
      ...columns,
      {
        id: "status",
        header: "Status",
        cell: ({ row }) =>
          row.original.active === false ? <Badge>Nonaktif</Badge> : <Badge tone="success">Aktif</Badge>,
      },
      {
        id: "actions",
        header: "",
        meta: { align: "right" },
        cell: ({ row }) => (
          <div className="flex justify-end gap-1">
            {can(`${permissionPrefix}:EDIT`) && (
              <Button size="icon" variant="ghost" onClick={() => setEditing(row.original)} aria-label="Ubah">
                <Pencil className="h-4 w-4" />
              </Button>
            )}
            {can(`${permissionPrefix}:DELETE`) && (
              <Button size="icon" variant="ghost" onClick={() => setDeleting(row.original)} aria-label="Hapus">
                <Trash2 className="h-4 w-4 text-danger" />
              </Button>
            )}
          </div>
        ),
      },
    ],
    [columns, can, permissionPrefix],
  );

  return (
    <>
      <PageHeader
        title={title}
        description={description}
        actions={
          can(`${permissionPrefix}:CREATE`) && (
            <Button onClick={() => setEditing("new")}>
              <Plus className="h-4 w-4" /> Tambah {entityLabel}
            </Button>
          )
        }
      />
      <Card>
        <div className="flex flex-col gap-3 border-b border-border p-4 sm:flex-row">
          <div className="relative flex-1">
            <Search className="pointer-events-none absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-muted" />
            <Input
              className="pl-9"
              placeholder="Cari kode atau nama…"
              value={q}
              onChange={(e) => {
                setQ(e.target.value);
                setPage(0);
              }}
              aria-label={`Cari ${entityLabel}`}
            />
          </div>
          {extraFilters?.((k, v) => {
            setFilters((f) => ({ ...f, [k]: v }));
            setPage(0);
          }, filters)}
          <Select
            className="sm:w-40"
            value={active}
            onChange={(e) => {
              setActive(e.target.value as "" | "true" | "false");
              setPage(0);
            }}
            aria-label="Filter status"
          >
            <option value="true">Aktif</option>
            <option value="false">Nonaktif</option>
            <option value="">Semua</option>
          </Select>
        </div>
        <DataTable
          columns={allColumns}
          data={list.data?.content}
          loading={list.isLoading}
          error={list.error ? errorMessage(list.error) : null}
          onRetry={list.refetch}
          page={page}
          totalElements={list.data?.totalElements}
          totalPages={list.data?.totalPages}
          onPageChange={setPage}
          emptyTitle={`Belum ada ${entityLabel.toLowerCase()}`}
        />
      </Card>

      {editing && (
        <CrudForm
          key={editing === "new" ? "new" : editing.id}
          entityLabel={entityLabel}
          endpoint={endpoint}
          fields={fields}
          schema={schema}
          defaults={editing === "new" ? emptyValues : toForm(editing)}
          id={editing === "new" ? null : editing.id}
          toPayload={toPayload}
          size={dialogSize}
          onClose={() => setEditing(null)}
          onSaved={invalidate}
        />
      )}

      <ConfirmDialog
        open={deleting !== null}
        title={`Hapus ${entityLabel.toLowerCase()}?`}
        message={
          deleting
            ? `Data ${(deleting as unknown as { code?: string }).code ?? ""} akan dihapus permanen. Jika sudah dipakai transaksi, penghapusan akan ditolak — nonaktifkan saja.`
            : ""
        }
        confirmLabel="Hapus"
        tone="danger"
        loading={remove.isPending}
        onConfirm={() => deleting && remove.mutate(deleting.id)}
        onClose={() => setDeleting(null)}
      />
    </>
  );
}

interface CrudFormProps<F extends FieldValues> {
  entityLabel: string;
  endpoint: string;
  fields: FieldDef<F>[];
  schema: ZodType<F, F>;
  defaults: DefaultValues<F>;
  id: number | null;
  toPayload?: (values: F) => unknown;
  size: "md" | "lg";
  onClose: () => void;
  onSaved: () => void;
}

function CrudForm<F extends FieldValues>({
  entityLabel,
  endpoint,
  fields,
  schema,
  defaults,
  id,
  toPayload,
  size,
  onClose,
  onSaved,
}: CrudFormProps<F>) {
  const {
    register,
    control,
    handleSubmit,
    setError,
    formState: { errors, isSubmitting },
  } = useForm<F>({ resolver: zodResolver(schema) as Resolver<F>, defaultValues: defaults });

  const onSubmit = handleSubmit(async (values) => {
    const body = toPayload ? toPayload(values) : values;
    try {
      if (id === null) await api.post(endpoint, body);
      else await api.put(`${endpoint}/${id}`, body);
      toast.success(`${entityLabel} disimpan`);
      onSaved();
      onClose();
    } catch (err) {
      if (!applyServerErrors(err, setError)) toast.error(errorMessage(err));
    }
  });

  const errorOf = (name: string): string | undefined => {
    const e = (errors as Record<string, { message?: unknown } | undefined>)[name];
    return typeof e?.message === "string" ? e.message : undefined;
  };

  return (
    <Dialog
      open
      onClose={onClose}
      size={size}
      title={id === null ? `Tambah ${entityLabel}` : `Ubah ${entityLabel}`}
      footer={
        <>
          <Button variant="outline" onClick={onClose}>
            Batal
          </Button>
          <Button type="submit" form="crud-form" loading={isSubmitting}>
            Simpan
          </Button>
        </>
      }
    >
      <form id="crud-form" onSubmit={onSubmit} className="grid gap-4 sm:grid-cols-2" noValidate>
        {fields.map((f) => {
          const name = f.name as string;
          const err = errorOf(name);
          const disabled = f.disabledOnEdit && id !== null;
          if (f.type === "checkbox") {
            return (
              <label key={name} className={cn("flex items-center gap-2 text-sm", f.span === 2 && "sm:col-span-2")}>
                <input type="checkbox" className="h-4 w-4 accent-primary" {...register(f.name)} />
                {f.label}
              </label>
            );
          }
          return (
            <FormField
              key={name}
              label={f.label}
              htmlFor={name}
              required={f.required}
              error={err}
              hint={f.hint}
              className={cn(f.span === 2 && "sm:col-span-2")}
            >
              {f.type === "textarea" ? (
                <Textarea id={name} placeholder={f.placeholder} {...register(f.name)} />
              ) : f.type === "select" ? (
                <Controller
                  control={control}
                  name={f.name}
                  render={({ field }) => (
                    <Select
                      id={name}
                      invalid={!!err}
                      disabled={disabled}
                      value={field.value === null || field.value === undefined ? "" : String(field.value)}
                      onChange={(e) => field.onChange(e.target.value === "" ? null : e.target.value)}
                      onBlur={field.onBlur}
                    >
                      {(f.allowEmpty || !f.required) && <option value="">— Pilih —</option>}
                      {f.required && !f.allowEmpty && <option value="" disabled>— Pilih —</option>}
                      {f.options?.map((o) => (
                        <option key={o.value} value={o.value}>
                          {o.label}
                        </option>
                      ))}
                    </Select>
                  )}
                />
              ) : f.type === "money" || f.type === "number" ? (
                <Controller
                  control={control}
                  name={f.name}
                  render={({ field }) => (
                    <MoneyInput
                      id={name}
                      invalid={!!err}
                      value={field.value === null || field.value === undefined ? "" : String(field.value)}
                      onValueChange={field.onChange}
                      onBlur={field.onBlur}
                    />
                  )}
                />
              ) : (
                <Input
                  id={name}
                  type={f.type === "email" ? "email" : "text"}
                  placeholder={f.placeholder}
                  invalid={!!err}
                  disabled={disabled}
                  {...register(f.name)}
                />
              )}
            </FormField>
          );
        })}
      </form>
    </Dialog>
  );
}
