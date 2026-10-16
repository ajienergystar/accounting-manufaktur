import { z } from "zod";

export const PASSWORD_RULES = [
  { test: (v: string) => v.length >= 8, label: "Minimal 8 karakter" },
  { test: (v: string) => /[A-Z]/.test(v), label: "Huruf besar (A-Z)" },
  { test: (v: string) => /[a-z]/.test(v), label: "Huruf kecil (a-z)" },
  { test: (v: string) => /\d/.test(v), label: "Angka (0-9)" },
  { test: (v: string) => /[^A-Za-z0-9]/.test(v), label: "Simbol (!@#$...)" },
];

const strongPassword = z
  .string()
  .min(1, "Password wajib diisi")
  .max(100, "Password maksimal 100 karakter")
  .refine((v) => PASSWORD_RULES.every((r) => r.test(v)), "Password belum memenuhi semua syarat");

export const loginSchema = z.object({
  identifier: z.string().trim().min(1, "Email atau username wajib diisi"),
  password: z.string().min(1, "Password wajib diisi"),
  rememberMe: z.boolean(),
});
export type LoginForm = z.infer<typeof loginSchema>;

export const registerSchema = z
  .object({
    fullName: z.string().trim().min(1, "Nama lengkap wajib diisi").max(150),
    email: z.email("Format email tidak valid"),
    username: z
      .string()
      .trim()
      .regex(/^[a-zA-Z0-9._-]{3,50}$/, "3-50 karakter: huruf, angka, titik, minus, underscore"),
    password: strongPassword,
    confirmPassword: z.string().min(1, "Konfirmasi password wajib diisi"),
    companyName: z.string().trim().max(150).optional(),
    needCompany: z.boolean(),
  })
  .refine((d) => d.password === d.confirmPassword, {
    path: ["confirmPassword"],
    message: "Konfirmasi password tidak sama",
  })
  .refine((d) => !d.needCompany || (d.companyName && d.companyName.length > 0), {
    path: ["companyName"],
    message: "Nama perusahaan wajib diisi",
  });
export type RegisterForm = z.infer<typeof registerSchema>;

export const forgotSchema = z.object({ email: z.email("Format email tidak valid") });
export type ForgotForm = z.infer<typeof forgotSchema>;

export const resetSchema = z
  .object({ password: strongPassword, confirmPassword: z.string().min(1, "Konfirmasi password wajib diisi") })
  .refine((d) => d.password === d.confirmPassword, {
    path: ["confirmPassword"],
    message: "Konfirmasi password tidak sama",
  });
export type ResetForm = z.infer<typeof resetSchema>;

export const changePasswordSchema = z
  .object({
    currentPassword: z.string().min(1, "Password lama wajib diisi"),
    newPassword: strongPassword,
    confirmPassword: z.string().min(1, "Konfirmasi password wajib diisi"),
  })
  .refine((d) => d.newPassword === d.confirmPassword, {
    path: ["confirmPassword"],
    message: "Konfirmasi password tidak sama",
  });
export type ChangePasswordForm = z.infer<typeof changePasswordSchema>;
