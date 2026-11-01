package com.gympro.plan.exception;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class GlobalExceptionHandlerTest {

    private GlobalExceptionHandler handler;

    @BeforeEach
    void setUp() {
        handler = new GlobalExceptionHandler();
    }

    // ── PlanNotFoundException ──────────────────────────────

    @Test
    void handlePlanNotFound_ShouldReturn404() {
        PlanNotFoundException ex = new PlanNotFoundException(1L);
        ResponseEntity<Map<String, Object>> response = handler.handlePlanNotFound(ex);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(404, response.getBody().get("status"));
        assertEquals("Not Found", response.getBody().get("error"));
        assertNotNull(response.getBody().get("timestamp"));
        assertNotNull(response.getBody().get("message"));
    }

    @Test
    void handlePlanNotFound_ShouldContainMessage() {
        PlanNotFoundException ex = new PlanNotFoundException(42L);
        ResponseEntity<Map<String, Object>> response = handler.handlePlanNotFound(ex);

        String message = (String) response.getBody().get("message");
        assertNotNull(message);
        assertTrue(message.contains("42"));
    }

    // ── SubscriptionNotFoundException ─────────────────────

    @Test
    void handleSubscriptionNotFound_ShouldReturn404() {
        SubscriptionNotFoundException ex = new SubscriptionNotFoundException(5L);
        ResponseEntity<Map<String, Object>> response = handler.handleSubscriptionNotFound(ex);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertEquals(404, response.getBody().get("status"));
    }

    @Test
    void handleSubscriptionNotFound_ShouldContainMessage() {
        SubscriptionNotFoundException ex = new SubscriptionNotFoundException(7L);
        ResponseEntity<Map<String, Object>> response = handler.handleSubscriptionNotFound(ex);

        String message = (String) response.getBody().get("message");
        assertNotNull(message);
        assertTrue(message.contains("7"));
    }

    // ── AccessDeniedException ──────────────────────────────

    @Test
    void handleAccessDenied_ShouldReturn403() {
        AccessDeniedException ex = new AccessDeniedException("Access denied ❌ ADMIN only");
        ResponseEntity<Map<String, Object>> response = handler.handleAccessDenied(ex);

        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        assertEquals(403, response.getBody().get("status"));
        assertEquals("Forbidden", response.getBody().get("error"));
    }

    @Test
    void handleAccessDenied_ShouldContainMessage() {
        AccessDeniedException ex = new AccessDeniedException("ADMIN only");
        ResponseEntity<Map<String, Object>> response = handler.handleAccessDenied(ex);

        assertEquals("ADMIN only", response.getBody().get("message"));
    }

    // ── IllegalArgumentException ───────────────────────────

    @Test
    void handleIllegalArgument_ShouldReturn400() {
        IllegalArgumentException ex = new IllegalArgumentException("Invalid input");
        ResponseEntity<Map<String, Object>> response = handler.handleIllegalArgument(ex);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals(400, response.getBody().get("status"));
        assertEquals("Bad Request", response.getBody().get("error"));
    }

    @Test
    void handleIllegalArgument_ShouldContainMessage() {
        IllegalArgumentException ex = new IllegalArgumentException("Bad data");
        ResponseEntity<Map<String, Object>> response = handler.handleIllegalArgument(ex);

        assertEquals("Bad data", response.getBody().get("message"));
    }

    // ── General Exception ──────────────────────────────────

    @Test
    void handleGeneral_ShouldReturn500() {
        Exception ex = new Exception("Unexpected failure");
        ResponseEntity<Map<String, Object>> response = handler.handleGeneral(ex);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertEquals(500, response.getBody().get("status"));
        assertEquals("Internal Server Error", response.getBody().get("error"));
    }

    @Test
    void handleGeneral_ShouldContainExceptionMessage() {
        Exception ex = new Exception("DB connection lost");
        ResponseEntity<Map<String, Object>> response = handler.handleGeneral(ex);

        String message = (String) response.getBody().get("message");
        assertTrue(message.contains("DB connection lost"));
    }

    @Test
    void handleGeneral_ShouldContainTimestamp() {
        Exception ex = new Exception("error");
        ResponseEntity<Map<String, Object>> response = handler.handleGeneral(ex);

        assertNotNull(response.getBody().get("timestamp"));
    }
}
