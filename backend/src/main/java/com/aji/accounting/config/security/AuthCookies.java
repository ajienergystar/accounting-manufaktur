package com.aji.accounting.config.security;

import com.aji.accounting.config.AppProperties;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Arrays;
import java.util.Optional;

/**
 * Pengelolaan cookie autentikasi.
 * <ul>
 *   <li>access_token  – JWT, httpOnly, path /api</li>
 *   <li>refresh_token – token acak, httpOnly, path /api/v1/auth</li>
 *   <li>session_hint  – penanda login untuk redirect di Next.js proxy (bukan kredensial)</li>
 * </ul>
 * SameSite=Strict mencegah cookie terkirim dari situs lain (proteksi CSRF).
 */
@Component
@RequiredArgsConstructor
public class AuthCookies {

    public static final String ACCESS = "access_token";
    public static final String REFRESH = "refresh_token";
    public static final String HINT = "session_hint";

    private final AppProperties props;

    public void write(HttpServletResponse res, String accessToken, Duration accessTtl,
                      String refreshToken, Duration refreshTtl, boolean rememberMe) {
        add(res, build(ACCESS, accessToken, "/api", accessTtl));
        add(res, build(REFRESH, refreshToken, "/api/v1/auth", rememberMe ? refreshTtl : null));
        add(res, build(HINT, "1", "/", rememberMe ? refreshTtl : null));
    }

    public void clear(HttpServletResponse res) {
        add(res, build(ACCESS, "", "/api", Duration.ZERO));
        add(res, build(REFRESH, "", "/api/v1/auth", Duration.ZERO));
        add(res, build(HINT, "", "/", Duration.ZERO));
    }

    public Optional<String> read(HttpServletRequest req, String name) {
        Cookie[] cookies = req.getCookies();
        if (cookies == null) {
            return Optional.empty();
        }
        return Arrays.stream(cookies)
                .filter(c -> name.equals(c.getName()) && c.getValue() != null && !c.getValue().isBlank())
                .map(Cookie::getValue)
                .findFirst();
    }

    /** maxAge null = session cookie (hilang saat browser ditutup). */
    private ResponseCookie build(String name, String value, String path, Duration maxAge) {
        ResponseCookie.ResponseCookieBuilder b = ResponseCookie.from(name, value)
                .httpOnly(true)
                .secure(props.jwt().cookieSecure())
                .sameSite("Strict")
                .path(path);
        if (maxAge != null) {
            b.maxAge(maxAge);
        }
        return b.build();
    }

    private void add(HttpServletResponse res, ResponseCookie cookie) {
        res.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }
}
