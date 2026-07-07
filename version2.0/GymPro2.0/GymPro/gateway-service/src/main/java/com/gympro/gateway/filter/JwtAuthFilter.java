package com.gympro.gateway.filter;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * Global JWT authentication filter for the API Gateway.
 *
 * Runs on EVERY inbound request (order = -1, highest priority).
 * Validates the Bearer token and forwards user identity headers to downstream services.
 *
 * PUBLIC_URLS — paths that bypass JWT validation entirely:
 *   • All /chatbot/** routes (chatbot-service handles unauthenticated requests)
 *   • Auth endpoints (login, register, forgot-password, OTP flow)
 *   • Actuator endpoints for all services
 *
 * SSE TOKEN FALLBACK:
 *   Paths matching SSE_TOKEN_PATHS accept the JWT via ?token= query parameter
 *   when no Authorization header is present.  This is required because the
 *   browser-native EventSource API cannot set custom headers.
 */
@Component
public class JwtAuthFilter implements GlobalFilter, Ordered {

    private static final Logger log = LoggerFactory.getLogger(JwtAuthFilter.class);

    @Value("${jwt.secret}")
    private String secret;

    /**
     * Paths that do NOT require a JWT token.
     * Uses startsWith matching — order matters for specificity.
     *
     * IMPORTANT: /chatbot is listed first to ensure ALL chatbot routes
     * (including /chatbot/chat, /chatbot/health, /chatbot/conversation/*) 
     * are publicly accessible without a token.
     */
    private static final List<String> PUBLIC_URLS = List.of(
        // ── Chatbot (fully public — no JWT required) ────────────────────────
        "/chatbot",               // covers /chatbot, /chatbot/**, /chatbot/chat, /chatbot/health

        // ── Auth service ─────────────────────────────────────────────────────
        "/auth/register",
        "/auth/login",
        "/auth/test",
        "/auth/forgot-password",
        "/auth/verify-otp",
        "/auth/reset-password",

        // ── Actuator endpoints (health probes via gateway) ───────────────────
        "/actuator",             // gateway's own actuator
        "/auth/actuator",
        "/members/actuator",
        "/trainers/actuator",
        "/plans/actuator",
        "/bookings/actuator",
        "/notify/actuator",
        "/payments/actuator",
        "/chatbot/actuator",

        // ── Swagger / OpenAPI (gateway's own aggregated UI + per-service docs) ──
        // Without these, this filter returns 401 for every Swagger/OpenAPI
        // request before it ever reaches the routing layer.
        "/swagger-ui",
        "/swagger-ui.html",
        "/v3/api-docs",
        "/webjars/swagger-ui",
        "/auth-service/v3/api-docs",
        "/auth-service/swagger-ui",
        "/member-service/v3/api-docs",
        "/member-service/swagger-ui",
        "/trainer-service/v3/api-docs",
        "/trainer-service/swagger-ui",
        "/plan-service/v3/api-docs",
        "/plan-service/swagger-ui",
        "/booking-service/v3/api-docs",
        "/booking-service/swagger-ui",
        "/notification-service/v3/api-docs",
        "/notification-service/swagger-ui",
        "/payment-service/v3/api-docs",
        "/payment-service/swagger-ui",
        "/chatbot-service/v3/api-docs",
        "/chatbot-service/swagger-ui"
    );

    /**
     * Paths that may carry the JWT as a ?token= query parameter instead of
     * (or as a fallback to) the Authorization header.
     *
     * The browser EventSource API cannot set custom headers, so SSE stream
     * endpoints must accept the token via query param.
     *
     * Pattern: /notify/inapp/{userId}/stream
     * Uses startsWith("/notify/inapp/") + endsWith("/stream") check below.
     */
    private static final String SSE_STREAM_PREFIX = "/notify/inapp/";
    private static final String SSE_STREAM_SUFFIX = "/stream";

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String path   = request.getURI().getPath();
        String method = request.getMethod() != null ? request.getMethod().name() : "UNKNOWN";

        // ── Preflight CORS requests always pass through ──────────────────────
        if ("OPTIONS".equalsIgnoreCase(method)) {
            return chain.filter(exchange);
        }

        // ── Allow public URLs without a token ───────────────────────────────
        if (PUBLIC_URLS.stream().anyMatch(path::startsWith)) {
            log.debug("Public URL — skipping JWT check | {} {}", method, path);
            return chain.filter(exchange);
        }

        // ── Resolve token: Authorization header first, then ?token= fallback ─
        //
        // The fallback is intentionally restricted to SSE stream paths to
        // minimise the attack surface of query-param tokens (they appear in
        // server logs and browser history).  All other endpoints must continue
        // to use the Authorization header.
        String token = null;

        String authHeader = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            // Happy path — standard Bearer header present
            token = authHeader.substring(7);
        } else if (isSseStreamPath(path)) {
            // Fallback: SSE endpoint — EventSource cannot send headers, so we
            // accept the JWT via ?token= query parameter instead.
            String queryToken = request.getQueryParams().getFirst("token");
            if (queryToken != null && !queryToken.isBlank()) {
                log.debug("SSE path — using ?token= query param fallback | {} {}", method, path);
                token = queryToken;
            }
        }

        if (token == null) {
            log.warn("Missing or malformed Authorization header | {} {}", method, path);
            exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
            return exchange.getResponse().setComplete();
        }

        // ── Validate JWT ─────────────────────────────────────────────────────
        try {
            Claims claims = Jwts.parserBuilder()
                .setSigningKey(Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8)))
                .build()
                .parseClaimsJws(token)
                .getBody();

            String userEmail  = claims.getSubject();
            String userRole   = claims.get("role", String.class);
            // userId is embedded as a Long claim — may be absent in older tokens
            Object userIdObj  = claims.get("userId");
            String userIdStr  = userIdObj != null ? String.valueOf(userIdObj) : "";

            log.debug("JWT valid | user={} | role={} | userId={} | {} {}",
                userEmail, userRole, userIdStr, method, path);

            // Forward user identity to downstream services via custom headers
            ServerWebExchange mutated = exchange.mutate()
                .request(r -> r
                    .header("X-User-Email",  userEmail  != null ? userEmail  : "")
                    .header("X-User-Role",   userRole   != null ? userRole   : "")
                    .header("X-User-Id",     userIdStr)   // new: numeric ID for downstream use
                )
                .build();

            return chain.filter(mutated);

        } catch (Exception ex) {
            log.warn("JWT validation failed | {} {} | reason={}", method, path, ex.getMessage());
            exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
            return exchange.getResponse().setComplete();
        }
    }

    /**
     * Returns true for paths matching the SSE stream pattern:
     *   /notify/inapp/{anything}/stream
     *
     * Examples that match:
     *   /notify/inapp/42/stream
     *   /notify/inapp/101/stream
     *
     * Examples that do NOT match:
     *   /notify/inapp/42/history   ← wrong suffix
     *   /notify/other/42/stream    ← wrong prefix
     */
    private boolean isSseStreamPath(String path) {
        return path.startsWith(SSE_STREAM_PREFIX) && path.endsWith(SSE_STREAM_SUFFIX);
    }

    /** Run before all other filters (highest priority). */
    @Override
    public int getOrder() {
        return -1;
    }
}