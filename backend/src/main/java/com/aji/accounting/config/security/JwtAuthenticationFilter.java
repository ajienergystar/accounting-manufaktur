package com.aji.accounting.config.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Optional;

/**
 * Membaca JWT dari cookie access_token (atau header Authorization: Bearer untuk klien API/Swagger),
 * memuat user beserta permission terbarunya, lalu mengisi SecurityContext.
 */
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final AppUserDetailsService userDetailsService;
    private final AuthCookies cookies;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        if (SecurityContextHolder.getContext().getAuthentication() == null) {
            resolveToken(request)
                    .flatMap(jwtService::validateAndGetUsername)
                    .ifPresent(username -> authenticate(username, request));
        }
        chain.doFilter(request, response);
    }

    private Optional<String> resolveToken(HttpServletRequest request) {
        String header = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (header != null && header.startsWith("Bearer ")) {
            return Optional.of(header.substring(7));
        }
        return cookies.read(request, AuthCookies.ACCESS);
    }

    private void authenticate(String username, HttpServletRequest request) {
        try {
            AppUserPrincipal principal = userDetailsService.loadUserByUsername(username);
            if (!principal.isEnabled()) {
                return;
            }
            var auth = new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
            auth.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
            SecurityContextHolder.getContext().setAuthentication(auth);
        } catch (UsernameNotFoundException ignored) {
            // token milik user yang sudah dihapus: biarkan tidak terautentikasi
        }
    }
}
