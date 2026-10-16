package com.aji.accounting.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

@ConfigurationProperties(prefix = "app")
public record AppProperties(
        String name,
        Cors cors,
        Jwt jwt,
        Security security,
        String frontendUrl
) {
    public record Cors(List<String> allowedOrigins) {
    }

    /**
     * @param secret              kunci HMAC base64 minimal 256-bit
     * @param accessTokenMinutes  umur access token
     * @param refreshTokenDays    umur refresh token
     * @param cookieSecure        true di production (HTTPS)
     */
    public record Jwt(String secret, long accessTokenMinutes, long refreshTokenDays, boolean cookieSecure) {
    }

    /**
     * @param maxFailedAttempts   jumlah gagal login sebelum akun dikunci
     * @param lockMinutes         lama penguncian
     * @param resetTokenMinutes   umur token reset password
     */
    public record Security(int maxFailedAttempts, long lockMinutes, long resetTokenMinutes) {
    }
}
