package nl.donniebankoebarkie.api.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import nl.donniebankoebarkie.api.config.JwtProperties;
import nl.donniebankoebarkie.api.model.User;
import nl.donniebankoebarkie.api.model.enums.UserRole;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.time.Instant;
import java.util.Date;

@Service
public class JwtService {
    private final SecretKey secretKey;
    private final long accessTokenExpirationSeconds;

    public JwtService(JwtProperties jwtProperties) {
        this.secretKey = Keys.hmacShaKeyFor(Decoders.BASE64.decode(toBase64(jwtProperties.secret())));
        this.accessTokenExpirationSeconds = jwtProperties.accessTokenExpirationSeconds();
    }

    public String generateAccessToken(User user) {
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(String.valueOf(user.getId()))
                .claim("email", user.getEmail())
                .claim("role", user.getRole().name())
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusSeconds(accessTokenExpirationSeconds)))
                .signWith(secretKey)
                .compact();
    }

    public AuthenticatedUser parseAccessToken(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();

        return new AuthenticatedUser(
                Long.parseLong(claims.getSubject()),
                claims.get("email", String.class),
                UserRole.valueOf(claims.get("role", String.class))
        );
    }

    private String toBase64(String secret) {
        return java.util.Base64.getEncoder().encodeToString(secret.getBytes(java.nio.charset.StandardCharsets.UTF_8));
    }
}
