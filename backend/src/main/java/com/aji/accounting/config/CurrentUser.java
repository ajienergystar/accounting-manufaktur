package com.aji.accounting.config;

/**
 * Akses username pengguna yang sedang login. Diisi oleh lapisan security (Fase 2).
 */
public final class CurrentUser {
    public static final String SYSTEM = "system";

    private CurrentUser() {
    }

    public static String usernameOrSystem() {
        return SYSTEM;
    }
}
