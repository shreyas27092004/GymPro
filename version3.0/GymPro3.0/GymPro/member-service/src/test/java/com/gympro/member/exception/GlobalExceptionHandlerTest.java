package com.gympro.member.exception;

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

    // ─── MemberNotFoundException ──────────────────────────────────────────────

    @Test
    void handleMemberNotFound_ShouldReturn404WithMessage() {
        MemberNotFoundException ex = new MemberNotFoundException(42L);

        ResponseEntity<Map<String, Object>> response = handler.handleMemberNotFound(ex);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        Map<String, Object> body = response.getBody();
        assertNotNull(body);
        assertEquals(404, body.get("status"));
        assertEquals("Not Found", body.get("error"));
        assertTrue(body.get("message").toString().contains("42"));
        assertNotNull(body.get("timestamp"));
    }

    @Test
    void handleMemberNotFound_WithStringConstructor_ShouldReturn404() {
        MemberNotFoundException ex = new MemberNotFoundException("Custom not found message");

        ResponseEntity<Map<String, Object>> response = handler.handleMemberNotFound(ex);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertEquals("Custom not found message", response.getBody().get("message"));
    }

    // ─── AccessDeniedException ────────────────────────────────────────────────

    @Test
    void handleAccessDenied_ShouldReturn403WithMessage() {
        AccessDeniedException ex = new AccessDeniedException("Access denied ❌ ADMIN only");

        ResponseEntity<Map<String, Object>> response = handler.handleAccessDenied(ex);

        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        Map<String, Object> body = response.getBody();
        assertNotNull(body);
        assertEquals(403, body.get("status"));
        assertEquals("Forbidden", body.get("error"));
        assertTrue(body.get("message").toString().contains("ADMIN only"));
        assertNotNull(body.get("timestamp"));
    }

    // ─── IllegalArgumentException ─────────────────────────────────────────────

    @Test
    void handleIllegalArgument_ShouldReturn400WithMessage() {
        IllegalArgumentException ex = new IllegalArgumentException("Member email must not be blank");

        ResponseEntity<Map<String, Object>> response = handler.handleIllegalArgument(ex);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        Map<String, Object> body = response.getBody();
        assertNotNull(body);
        assertEquals(400, body.get("status"));
        assertEquals("Bad Request", body.get("error"));
        assertEquals("Member email must not be blank", body.get("message"));
        assertNotNull(body.get("timestamp"));
    }

    // ─── Generic Exception ────────────────────────────────────────────────────

    @Test
    void handleGenericException_ShouldReturn500WithMessage() {
        Exception ex = new RuntimeException("Unexpected DB failure");

        ResponseEntity<Map<String, Object>> response = handler.handleGenericException(ex);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        Map<String, Object> body = response.getBody();
        assertNotNull(body);
        assertEquals(500, body.get("status"));
        assertEquals("Internal Server Error", body.get("error"));
        assertTrue(body.get("message").toString().contains("Unexpected DB failure"));
        assertNotNull(body.get("timestamp"));
    }

    @Test
    void handleGenericException_ShouldWrapNullMessageGracefully() {
        Exception ex = new RuntimeException((String) null);

        ResponseEntity<Map<String, Object>> response = handler.handleGenericException(ex);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertNotNull(response.getBody());
    }
}
