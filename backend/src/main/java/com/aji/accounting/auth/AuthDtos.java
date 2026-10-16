package com.aji.accounting.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.Set;

public final class AuthDtos {

    /** Minimal 8 karakter, ada huruf besar, huruf kecil, angka, dan simbol. */
    public static final String PASSWORD_REGEX = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[^A-Za-z0-9]).{8,100}$";
    public static final String PASSWORD_MESSAGE =
            "Password minimal 8 karakter dan wajib mengandung huruf besar, huruf kecil, angka, dan simbol";

    private AuthDtos() {
    }

    public record RegisterRequest(
            @NotBlank(message = "Nama lengkap wajib diisi") @Size(max = 150) String fullName,
            @NotBlank(message = "Email wajib diisi") @Email(message = "Format email tidak valid") @Size(max = 150) String email,
            @NotBlank(message = "Username wajib diisi")
            @Pattern(regexp = "^[a-zA-Z0-9._-]{3,50}$", message = "Username 3-50 karakter: huruf, angka, titik, minus, underscore")
            String username,
            @NotBlank(message = "Password wajib diisi") @Pattern(regexp = PASSWORD_REGEX, message = PASSWORD_MESSAGE) String password,
            @NotBlank(message = "Konfirmasi password wajib diisi") String confirmPassword,
            @Size(max = 150) String companyName
    ) {
    }

    public record RegisterResponse(String message, String status) {
    }

    public record RegistrationStatus(boolean hasUsers) {
    }

    public record LoginRequest(
            @NotBlank(message = "Email atau username wajib diisi") String identifier,
            @NotBlank(message = "Password wajib diisi") String password,
            boolean rememberMe
    ) {
    }

    public record ForgotPasswordRequest(
            @NotBlank(message = "Email wajib diisi") @Email(message = "Format email tidak valid") String email
    ) {
    }

    public record ResetPasswordRequest(
            @NotBlank String token,
            @NotBlank(message = "Password wajib diisi") @Pattern(regexp = PASSWORD_REGEX, message = PASSWORD_MESSAGE) String password,
            @NotBlank(message = "Konfirmasi password wajib diisi") String confirmPassword
    ) {
    }

    public record ChangePasswordRequest(
            @NotBlank(message = "Password lama wajib diisi") String currentPassword,
            @NotBlank(message = "Password baru wajib diisi") @Pattern(regexp = PASSWORD_REGEX, message = PASSWORD_MESSAGE) String newPassword,
            @NotBlank(message = "Konfirmasi password wajib diisi") String confirmPassword
    ) {
    }

    public record CompanyInfo(Long id, String code, String name) {
    }

    public record MeResponse(
            Long id,
            String username,
            String email,
            String fullName,
            CompanyInfo company,
            Set<String> roles,
            Set<String> permissions
    ) {
    }
}
