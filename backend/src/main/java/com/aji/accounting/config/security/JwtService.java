package com.aji.accounting.config.security;

import com.aji.accounting.config.AppProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.Optional;

@Service
public class JwtService {

    private static final String ISSUER = "accounting-manufaktur";

    private final SecretKey key;
    private final Duration accessTtl;

    public JwtService(AppProperties props) {
        byte[] bytes = Decoders.BASE64.decode(props.jwt().secret());
        if (bytes.length < 32) {
            throw new IllegalStateException("app.jwt.secret minimal 256-bit (32 byte) base64");
        }
        this.key = Keys.hmacShaKeyFor(bytes);
        this.accessTtl = Duration.ofMinutes(props.jwt().accessTokenMinutes());
    }

    public Duration accessTtl() {
        return accessTtl;
    }

    public String generateAccessToken(AppUserPrincipal user) {
        Instant now = Instant.now();
        return Jwts.builder()
                .issuer(ISSUER)
                .subject(user.username())
                .claim("uid", user.id())
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(accessTtl)))
                .signWith(key)
                .compact();
    }

    /** @return username bila token valid dan belum kedaluwarsa. */
    public Optional<String> validateAndGetUsername(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(key)
                    .requireIssuer(ISSUER)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
            return Optional.ofNullable(claims.getSubject());
        } catch (JwtException | IllegalArgumentException e) {
            return Optional.empty();
        }
    }
}
