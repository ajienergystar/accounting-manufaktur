"use client";

import { useForm, useWatch } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { z } from "zod";
import { toast } from "sonner";
import { Button } from "@/components/ui/button";
import { Dialog } from "@/components/ui/dialog";
import { FormField } from "@/components/ui/form-field";
import { Input } from "@/components/ui/input";
import { Select } from "@/components/ui/select";
import { Textarea } from "@/components/ui/textarea";
import { ACCOUNT_TYPES, CASH_FLOW, type Account } from "@/features/coa/types";
import { api, errorMessage } from "@/lib/api";
import { applyServerErrors } from "@/lib/form-errors";

const schema = z.object({
  code: z.string().regex(/^\d-\d{4}$/, "Format kode X-XXXX, contoh 1-1210"),
  name: z.string().trim().min(1, "Nama akun wajib diisi").max(150),
  parentId: z.string().min(1, "Akun induk wajib dipilih"),
  accountType: z.string().optional(),
  normalBalance: z.enum(["DEBIT", "CREDIT"]),
  header: z.boolean(),
  cashBank: z.boolean(),
  cashFlowCategory: z.string().optional(),
  description: z.string().max(255).optional(),
  active: z.boolean(),
});
type Form = z.infer<typeof schema>;

interface Props {
  accounts: Account[];
  editing: Account | null;
  parentHint?: Account | null;
  onClose: () => void;
  onSaved: () => void;
}

export function AccountForm({ accounts, editing, parentHint, onClose, onSaved }: Props) {
  const headers = accounts.filter((a) => a.header && a.id !== editing?.id);
  const initialParent = editing?.parentId ?? parentHint?.id;
  const parent = accounts.find((a) => a.id === initialParent);

  const {
    register,
    handleSubmit,
    control,
    setValue,
    setError,
    formState: { errors, isSubmitting },
  } = useForm<Form>({
    resolver: zodResolver(schema),
    defaultValues: editing
      ? {
          code: editing.code,
          name: editing.name,
          parentId: String(editing.parentId ?? ""),
          accountType: editing.accountType,
          normalBalance: editing.normalBalance,
          header: editing.header,
          cashBank: editing.cashBank,
          cashFlowCategory: editing.cashFlowCategory && editing.cashFlowCategory !== "CASH" ? editing.cashFlowCategory : "",
          description: editing.description ?? "",
          active: editing.active,
        }
      : {
          code: parent ? parent.code.slice(0, 2) : "",
          name: "",
          parentId: parent ? String(parent.id) : "",
          accountType: parent?.accountType,
          normalBalance: parent?.normalBalance ?? "DEBIT",
          header: false,
          cashBank: false,
          cashFlowCategory: "",
          description: "",
          active: true,
        },
  });

  const [header, accountType] = useWatch({ control, name: ["header", "accountType"] });
  const balanceSheet = accountType === "ASSET" || accountType === "LIABILITY" || accountType === "EQUITY";

  const onSubmit = handleSubmit(async (v) => {
    const body = {
      ...v,
      parentId: Number(v.parentId),
      accountType: v.accountType || null,
      cashFlowCategory: v.cashFlowCategory || null,
    };
    try {
      if (editing) await api.put(`/accounts/${editing.id}`, body);
      else await api.post("/accounts", body);
      toast.success("Akun disimpan");
      onSaved();
      onClose();
    } catch (err) {
      if (!applyServerErrors(err, setError)) toast.error(errorMessage(err));
    }
  });

  return (
    <Dialog
      open
      onClose={onClose}
      size="lg"
      title={editing ? `Ubah akun ${editing.code}` : "Tambah akun"}
      footer={
        <>
          <Button variant="outline" onClick={onClose}>
            Batal
          </Button>
          <Button type="submit" form="account-form" loading={isSubmitting}>
            Simpan
          </Button>
        </>
      }
    >
      <form id="account-form" onSubmit={onSubmit} className="grid gap-4 sm:grid-cols-2" noValidate>
        <FormField label="Akun induk" htmlFor="parentId" required error={errors.parentId?.message} className="sm:col-span-2">
          <Select
            id="parentId"
            invalid={!!errors.parentId}
            {...register("parentId", {
              onChange: (e) => {
                const p = accounts.find((a) => String(a.id) === e.target.value);
                if (p) {
                  setValue("accountType", p.accountType);
                  setValue("normalBalance", p.normalBalance);
                }
              },
            })}
          >
            <option value="">— Pilih akun header —</option>
            {headers.map((a) => (
              <option key={a.id} value={a.id}>
                {"  ".repeat(a.level - 1)}
                {a.code} {a.name}
              </option>
            ))}
          </Select>
        </FormField>
        <FormField label="Kode akun" htmlFor="code" required error={errors.code?.message} hint="Format X-XXXX">
          <Input id="code" invalid={!!errors.code} {...register("code")} />
        </FormField>
        <FormField label="Nama akun" htmlFor="name" required error={errors.name?.message}>
          <Input id="name" invalid={!!errors.name} {...register("name")} />
        </FormField>
        <FormField label="Tipe akun" htmlFor="accountType" hint="Mengikuti akun induk">
          <Select id="accountType" {...register("accountType")}>
            {Object.entries(ACCOUNT_TYPES).map(([v, l]) => (
              <option key={v} value={v}>
                {l}
              </option>
            ))}
          </Select>
        </FormField>
        <FormField label="Saldo normal" htmlFor="normalBalance" hint="Ubah untuk akun kontra (mis. akumulasi penyusutan)">
          <Select id="normalBalance" {...register("normalBalance")}>
            <option value="DEBIT">Debit</option>
            <option value="CREDIT">Kredit</option>
          </Select>
        </FormField>
        {balanceSheet && !header && (
          <FormField label="Kategori arus kas" htmlFor="cashFlowCategory" hint="Untuk laporan arus kas metode tidak langsung">
            <Select id="cashFlowCategory" {...register("cashFlowCategory")}>
              <option value="">— Tidak ada —</option>
              {Object.entries(CASH_FLOW).map(([v, l]) => (
                <option key={v} value={v}>
                  {l}
                </option>
              ))}
            </Select>
          </FormField>
        )}
        <FormField label="Keterangan" htmlFor="description" className="sm:col-span-2">
          <Textarea id="description" {...register("description")} />
        </FormField>
        <div className="flex flex-wrap gap-6 sm:col-span-2">
          <label className="flex items-center gap-2 text-sm">
            <input type="checkbox" className="h-4 w-4 accent-primary" {...register("header")} />
            Akun header (pengelompokan)
          </label>
          {!header && accountType === "ASSET" && (
            <label className="flex items-center gap-2 text-sm">
              <input type="checkbox" className="h-4 w-4 accent-primary" {...register("cashBank")} />
              Akun kas / bank
            </label>
          )}
          <label className="flex items-center gap-2 text-sm">
            <input type="checkbox" className="h-4 w-4 accent-primary" {...register("active")} />
            Aktif
          </label>
        </div>
      </form>
    </Dialog>
  );
}
