package com.gympro.auth.util;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Date;

// ✅ Generates and validates JWT tokens
@Component
public class JwtUtil {

    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.expiration}")
    private long expiration;

    private Key getKey() {
        return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Create a token with email + role embedded.
     * Legacy overload — no userId claim. Kept for backward compatibility.
     */
    public String generateToken(String email, String role) {
        return generateToken(email, role, null);
    }

    /**
     * Create a token with email, role, AND numeric userId embedded.
     *
     * The userId is stored as the custom claim "userId" (Long).
     * The frontend decodes this from the JWT payload to wire up the
     * notification bell without a separate /me round-trip.
     *
     * Usage in AuthService.login():
     *   jwtUtil.generateToken(user.getEmail(), user.getRole(), user.getId());
     */
    public String generateToken(String email, String role, Long userId) {
        JwtBuilder builder = Jwts.builder()
            .setSubject(email)
            .claim("role", role)
            .setIssuedAt(new Date())
            .setExpiration(new Date(System.currentTimeMillis() + expiration));

        // Only embed userId when it is known (null-safe for legacy callers)
        if (userId != null) {
            builder.claim("userId", userId);
        }

        return builder
            .signWith(getKey(), SignatureAlgorithm.HS256)
            .compact();
    }

    // 🔍 Extract all claims from token
    public Claims extractClaims(String token) {
        return Jwts.parserBuilder()
            .setSigningKey(getKey())
            .build()
            .parseClaimsJws(token)
            .getBody();
    }
}