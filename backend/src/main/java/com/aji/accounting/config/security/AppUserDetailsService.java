package com.aji.accounting.config.security;

import com.aji.accounting.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AppUserDetailsService implements UserDetailsService {

    private final UserRepository users;

    @Override
    @Transactional(readOnly = true)
    public AppUserPrincipal loadUserByUsername(String username) {
        return users.findByIdentifierWithAuthorities(username)
                .map(AppUserPrincipal::from)
                .orElseThrow(() -> new UsernameNotFoundException("User tidak ditemukan"));
    }
}
