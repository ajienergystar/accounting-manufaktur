import { z } from "zod";

export const codeField = (max = 20) =>
  z
    .string()
    .trim()
    .min(1, "Kode wajib diisi")
    .max(max, `Maksimal ${max} karakter`)
    .regex(/^[A-Za-z0-9._/-]+$/, "Hanya huruf, angka, titik, minus, garis miring, underscore");

export const nameField = (max = 150) => z.string().trim().min(1, "Nama wajib diisi").max(max);

export const optionalText = (max: number) => z.string().trim().max(max).optional().or(z.literal(""));

export const decimalString = z
  .string()
  .optional()
  .refine((v) => v === undefined || v === "" || /^-?\d+(\.\d+)?$/.test(v), "Angka tidak valid")
  .refine((v) => v === undefined || v === "" || Number(v) >= 0, "Tidak boleh negatif");

export const emptyToNull = (v: string | null | undefined) => (v === "" || v === undefined ? null : v);
