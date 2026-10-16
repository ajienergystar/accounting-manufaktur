package com.aji.accounting.config.security;

import com.aji.accounting.user.User;
import com.aji.accounting.user.UserStatus;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Principal yang tersimpan di SecurityContext.
 * Authority berisi kode permission (mis. LEDGER:POST) dan role dengan prefix ROLE_.
 */
public record AppUserPrincipal(
        Long id,
        Long companyId,
        String username,
        String fullName,
        String passwordHash,
        boolean active,
        List<GrantedAuthority> authorities
) implements UserDetails {

    public static AppUserPrincipal from(User u) {
        List<GrantedAuthority> auths = new ArrayList<>();
        u.permissionCodes().forEach(p -> auths.add(new SimpleGrantedAuthority(p)));
        u.roleCodes().forEach(r -> auths.add(new SimpleGrantedAuthority("ROLE_" + r)));
        return new AppUserPrincipal(u.getId(), u.getCompany().getId(), u.getUsername(), u.getFullName(),
                u.getPasswordHash(), u.getStatus() == UserStatus.ACTIVE, List.copyOf(auths));
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    @Override
    public String getPassword() {
        return passwordHash;
    }

    @Override
    public String getUsername() {
        return username;
    }

    @Override
    public boolean isEnabled() {
        return active;
    }
}
