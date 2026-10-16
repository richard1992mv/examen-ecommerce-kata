package com.examen.ecommerce.security;

import com.examen.ecommerce.exception.ErrorMessages;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import java.time.Clock;
import java.time.Instant;
import java.util.Date;
import javax.crypto.SecretKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Emite y valida tokens JWT firmados con HS256. La hora sale del {@link Clock} inyectado tanto al emitir
 * (iat/exp) como al validar la expiración, así que emisión y validación usan el mismo reloj.
 */
@Service
public class JwtService {

    /** HS256 exige una clave de al menos 256 bits (32 bytes). */
    private static final int MIN_KEY_BYTES = 32;
    private static final String WEAK_SECRET = "app.jwt.secret debe tener al menos 256 bits codificados en Base64";

    private final SecretKey signingKey;
    private final long expirationMs;
    private final Clock clock;

    public JwtService(@Value("${app.jwt.secret}") String base64Secret,
                      @Value("${app.jwt.expiration-ms:3600000}") long expirationMs,
                      Clock clock) {
        byte[] keyBytes = Decoders.BASE64.decode(base64Secret);
        if (keyBytes.length < MIN_KEY_BYTES) {
            throw new IllegalStateException(WEAK_SECRET);
        }
        this.signingKey = Keys.hmacShaKeyFor(keyBytes);
        this.expirationMs = expirationMs;
        this.clock = clock;
    }

    public String generateToken(String username) {
        Instant now = clock.instant();
        return Jwts.builder()
                .subject(username)
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusMillis(expirationMs)))
                .signWith(signingKey, Jwts.SIG.HS256)
                .compact();
    }

    /**
     * Valida firma y vigencia y devuelve el usuario (subject).
     *
     * @throws ExpiredJwtException si el token expiró
     * @throws JwtException        si el token es inválido (firma, formato, etc.)
     */
    public String validateAndGetUsername(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(signingKey)
                .clock(() -> Date.from(clock.instant()))
                .build()
                .parseSignedClaims(token)
                .getPayload();
        String subject = claims.getSubject();
        if (subject == null || subject.isBlank()) {
            throw new JwtException(ErrorMessages.TOKEN_WITHOUT_SUBJECT);
        }
        return subject;
    }

    public long getExpirationMs() {
        return expirationMs;
    }

    public long getExpirationSeconds() {
        return expirationMs / 1000;
    }
}
