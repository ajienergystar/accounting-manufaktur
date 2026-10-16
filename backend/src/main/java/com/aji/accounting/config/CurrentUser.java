package com.aji.accounting.config;

import com.aji.accounting.config.security.AppUserPrincipal;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;

/** Akses pengguna yang sedang login dari SecurityContext. */
public final class CurrentUser {
    public static final String SYSTEM = "system";

    private CurrentUser() {
    }

    public static Optional<AppUserPrincipal> principal() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof AppUserPrincipal p) {
            return Optional.of(p);
        }
        return Optional.empty();
    }

    public static String usernameOrSystem() {
        return principal().map(AppUserPrincipal::username).orElse(SYSTEM);
    }

    public static Long id() {
        return principal().map(AppUserPrincipal::id)
                .orElseThrow(() -> new IllegalStateException("Tidak ada user yang login"));
    }

    public static Long companyId() {
        return principal().map(AppUserPrincipal::companyId)
                .orElseThrow(() -> new IllegalStateException("Tidak ada user yang login"));
    }
}
