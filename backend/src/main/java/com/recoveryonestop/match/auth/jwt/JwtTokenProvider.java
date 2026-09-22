package com.recoveryonestop.match.auth.jwt;

import com.recoveryonestop.match.user.domain.User;
import com.recoveryonestop.match.user.domain.UserRole;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.JwtBuilder;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.time.Clock;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;

@Component
public class JwtTokenProvider {

    private static final String ROLE_CLAIM = "role";
    private static final String TYPE_CLAIM = "type";
    private static final String ACCESS_TYPE = "access";
    private static final String REFRESH_TYPE = "refresh";

    private final JwtProperties properties;
    private final Clock clock;
    private final SecretKey signingKey;

    public JwtTokenProvider(JwtProperties properties, Clock clock) {
        this.properties = properties;
        this.clock = clock;
        this.signingKey = createSigningKey(properties.secret());
    }

    public String createAccessToken(User user) {
        return createToken(user, ACCESS_TYPE, properties.accessTokenExpirationMs());
    }

    public String createRefreshToken(User user) {
        return createToken(user, REFRESH_TYPE, properties.refreshTokenExpirationMs());
    }

    public TokenClaims parseAccessToken(String token) {
        return parseToken(token, ACCESS_TYPE);
    }

    public TokenClaims parseRefreshToken(String token) {
        return parseToken(token, REFRESH_TYPE);
    }

    public long getAccessTokenExpirationSeconds() {
        return properties.accessTokenExpirationMs() / 1_000;
    }

    public long getRefreshTokenExpirationMs() {
        return properties.refreshTokenExpirationMs();
    }

    private String createToken(User user, String type, long expirationMs) {
        Instant issuedAt = clock.instant();
        Instant expiresAt = issuedAt.plusMillis(expirationMs);

        JwtBuilder builder = Jwts.builder()
                .subject(user.getLoginId())
                .claim(ROLE_CLAIM, user.getRole().name())
                .claim(TYPE_CLAIM, type)
                .issuedAt(Date.from(issuedAt))
                .expiration(Date.from(expiresAt));
        if (REFRESH_TYPE.equals(type)) {
            builder.id(UUID.randomUUID().toString());
        }

        return builder.signWith(signingKey)
                .compact();
    }

    private TokenClaims parseToken(String token, String expectedType) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(signingKey)
                    .clock(() -> Date.from(clock.instant()))
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();

            String loginId = claims.getSubject();
            String role = claims.get(ROLE_CLAIM, String.class);
            String type = claims.get(TYPE_CLAIM, String.class);
            if (loginId == null || loginId.isBlank() || !expectedType.equals(type)) {
                throw new InvalidJwtException();
            }

            return new TokenClaims(loginId, UserRole.valueOf(role));
        } catch (io.jsonwebtoken.ExpiredJwtException exception) {
            throw new ExpiredJwtException();
        } catch (JwtException | IllegalArgumentException | NullPointerException exception) {
            throw new InvalidJwtException();
        }
    }

    private SecretKey createSigningKey(String encodedSecret) {
        try {
            return Keys.hmacShaKeyFor(Decoders.BASE64.decode(encodedSecret));
        } catch (RuntimeException exception) {
            throw new IllegalStateException(
                    "JWT_SECRET must be Base64-encoded and contain at least 32 bytes of key material.",
                    exception
            );
        }
    }
}
