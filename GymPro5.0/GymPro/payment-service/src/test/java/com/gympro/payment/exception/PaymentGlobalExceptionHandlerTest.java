package com.gympro.payment.exception;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class PaymentGlobalExceptionHandlerTest {

    private GlobalExceptionHandler handler;

    @BeforeEach
    void setUp() {
        handler = new GlobalExceptionHandler();
    }

    // ─── PaymentNotFoundException → 404 ───────────────────────────────────

    @Test
    void testHandleNotFound_Returns404() {
        PaymentNotFoundException ex = new PaymentNotFoundException("Payment not found with id: 42");
        ResponseEntity<Map<String, Object>> response = handler.handleNotFound(ex);

        assertEquals(404, response.getStatusCodeValue());
        assertEquals(404, response.getBody().get("status"));
        assertEquals("Payment not found with id: 42", response.getBody().get("message"));
        assertEquals("Not Found", response.getBody().get("error"));
        assertNotNull(response.getBody().get("timestamp"));
    }

    // ─── PaymentException → 400 ───────────────────────────────────────────

    @Test
    void testHandlePaymentException_Returns400() {
        PaymentException ex = new PaymentException("amount must be greater than 0");
        ResponseEntity<Map<String, Object>> response = handler.handlePaymentException(ex);

        assertEquals(400, response.getStatusCodeValue());
        assertEquals("amount must be greater than 0", response.getBody().get("message"));
        assertEquals("Bad Request", response.getBody().get("error"));
    }

    // ─── IllegalArgumentException → 400 ──────────────────────────────────

    @Test
    void testHandleIllegalArg_Returns400() {
        IllegalArgumentException ex = new IllegalArgumentException("Invalid input provided");
        ResponseEntity<Map<String, Object>> response = handler.handleIllegalArg(ex);

        assertEquals(400, response.getStatusCodeValue());
        assertEquals("Invalid input provided", response.getBody().get("message"));
    }

    // ─── SecurityException → 403 ──────────────────────────────────────────

    @Test
    void testHandleSecurity_Returns403() {
        SecurityException ex = new SecurityException("Access denied – only ADMIN can issue refunds");
        ResponseEntity<Map<String, Object>> response = handler.handleSecurity(ex);

        assertEquals(403, response.getStatusCodeValue());
        assertEquals("Forbidden", response.getBody().get("error"));
        assertTrue(response.getBody().get("message").toString().contains("ADMIN"));
    }

    // ─── General Exception → 500 ──────────────────────────────────────────

    @Test
    void testHandleGeneral_Returns500() {
        Exception ex = new Exception("Something catastrophic happened");
        ResponseEntity<Map<String, Object>> response = handler.handleGeneral(ex);

        assertEquals(500, response.getStatusCodeValue());
        assertEquals("Internal Server Error", response.getBody().get("error"));
        assertTrue(response.getBody().get("message").toString().contains("catastrophic"));
    }

    // ─── Response body always has timestamp ───────────────────────────────

    @Test
    void testResponseBody_AlwaysHasTimestamp() {
        PaymentException ex = new PaymentException("error");
        ResponseEntity<Map<String, Object>> response = handler.handlePaymentException(ex);

        assertTrue(response.getBody().containsKey("timestamp"));
        assertNotNull(response.getBody().get("timestamp"));
    }
}
