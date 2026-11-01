package com.gympro.auth.util;

import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link JwtUtil}.
 * Uses ReflectionTestUtils to inject @Value fields without a Spring context.
 */
class JwtUtilTest {

    private JwtUtil jwtUtil;

    // Must be ≥ 256 bits (32 chars) for HS256
    private static final String TEST_SECRET =
            "gympro_jwt_secret_key_for_hs256_algorithm_minimum_256_bits";
    private static final long TEST_EXPIRATION = 3_600_000L; // 1 hour

    @BeforeEach
    void setUp() {
        jwtUtil = new JwtUtil();
        ReflectionTestUtils.setField(jwtUtil, "secret", TEST_SECRET);
        ReflectionTestUtils.setField(jwtUtil, "expiration", TEST_EXPIRATION);
    }

    // ================================================================
    // generateToken() tests
    // ================================================================

    @Test
    @DisplayName("generateToken - returns a non-null, non-blank JWT string")
    void generateToken_returnsNonBlankToken() {
        String token = jwtUtil.generateToken("user@gympro.com", "MEMBER");
        assertNotNull(token);
        assertFalse(token.isBlank());
    }

    @Test
    @DisplayName("generateToken - token has three JWT segments (header.payload.signature)")
    void generateToken_hasThreeSegments() {
        String token = jwtUtil.generateToken("user@gympro.com", "MEMBER");
        String[] parts = token.split("\\.");
        assertEquals(3, parts.length, "JWT must have exactly 3 dot-separated segments");
    }

    @Test
    @DisplayName("generateToken - different emails produce different tokens")
    void generateToken_differentEmailsProduceDifferentTokens() {
        String token1 = jwtUtil.generateToken("alice@gympro.com", "MEMBER");
        String token2 = jwtUtil.generateToken("bob@gympro.com", "TRAINER");
        assertNotEquals(token1, token2);
    }

    // ================================================================
    // extractClaims() tests
    // ================================================================

    @Test
    @DisplayName("extractClaims - subject matches the email used during generation")
    void extractClaims_subjectIsEmail() {
        String email = "shreyas@gympro.com";
        String token = jwtUtil.generateToken(email, "ADMIN");

        Claims claims = jwtUtil.extractClaims(token);

        assertEquals(email, claims.getSubject());
    }

    @Test
    @DisplayName("extractClaims - role claim matches the role used during generation")
    void extractClaims_roleClaimIsCorrect() {
        String token = jwtUtil.generateToken("trainer@gympro.com", "TRAINER");

        Claims claims = jwtUtil.extractClaims(token);

        assertEquals("TRAINER", claims.get("role", String.class));
    }

    @Test
    @DisplayName("extractClaims - issuedAt is not null")
    void extractClaims_issuedAtIsPresent() {
        String token = jwtUtil.generateToken("user@gympro.com", "MEMBER");
        Claims claims = jwtUtil.extractClaims(token);
        assertNotNull(claims.getIssuedAt());
    }

    @Test
    @DisplayName("extractClaims - expiration is set in the future")
    void extractClaims_expirationIsInFuture() {
        String token = jwtUtil.generateToken("user@gympro.com", "MEMBER");
        Claims claims = jwtUtil.extractClaims(token);
        assertNotNull(claims.getExpiration());
        assertTrue(claims.getExpiration().getTime() > System.currentTimeMillis(),
                "Token expiration should be in the future");
    }

    @Test
    @DisplayName("extractClaims - all three roles (MEMBER, TRAINER, ADMIN) round-trip correctly")
    void extractClaims_allRolesRoundTrip() {
        for (String role : new String[]{"MEMBER", "TRAINER", "ADMIN"}) {
            String token = jwtUtil.generateToken("user@gympro.com", role);
            Claims claims = jwtUtil.extractClaims(token);
            assertEquals(role, claims.get("role", String.class),
                    "Role mismatch for: " + role);
        }
    }

    @Test
    @DisplayName("extractClaims - tampered token throws an exception")
    void extractClaims_tamperedTokenThrowsException() {
        String token = jwtUtil.generateToken("user@gympro.com", "MEMBER");
        // Corrupt the signature segment
        String tampered = token.substring(0, token.lastIndexOf('.') + 1) + "INVALIDSIGNATURE";
        assertThrows(Exception.class, () -> jwtUtil.extractClaims(tampered));
    }
}
