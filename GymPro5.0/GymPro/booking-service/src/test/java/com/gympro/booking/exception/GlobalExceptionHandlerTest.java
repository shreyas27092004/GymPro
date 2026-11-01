package com.gympro.booking.exception;

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

    // ── BookingNotFoundException ───────────────────────────

    @Test
    void handleBookingNotFound_ShouldReturn404() {
        BookingNotFoundException ex = new BookingNotFoundException(1L);
        ResponseEntity<Map<String, Object>> response = handler.handleBookingNotFound(ex);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(404, response.getBody().get("status"));
        assertEquals("Not Found", response.getBody().get("error"));
    }

    @Test
    void handleBookingNotFound_ShouldContainIdInMessage() {
        BookingNotFoundException ex = new BookingNotFoundException(42L);
        ResponseEntity<Map<String, Object>> response = handler.handleBookingNotFound(ex);

        String message = (String) response.getBody().get("message");
        assertNotNull(message);
        assertTrue(message.contains("42"));
    }

    @Test
    void handleBookingNotFound_ShouldContainTimestamp() {
        BookingNotFoundException ex = new BookingNotFoundException(1L);
        ResponseEntity<Map<String, Object>> response = handler.handleBookingNotFound(ex);

        assertNotNull(response.getBody().get("timestamp"));
    }

    // ── AccessDeniedException ──────────────────────────────

    @Test
    void handleAccessDenied_ShouldReturn403() {
        AccessDeniedException ex = new AccessDeniedException("Access denied ❌");
        ResponseEntity<Map<String, Object>> response = handler.handleAccessDenied(ex);

        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        assertEquals(403, response.getBody().get("status"));
        assertEquals("Forbidden", response.getBody().get("error"));
    }

    @Test
    void handleAccessDenied_ShouldContainMessage() {
        AccessDeniedException ex = new AccessDeniedException("TRAINER only");
        ResponseEntity<Map<String, Object>> response = handler.handleAccessDenied(ex);

        assertEquals("TRAINER only", response.getBody().get("message"));
    }

    @Test
    void handleAccessDenied_ShouldContainTimestamp() {
        AccessDeniedException ex = new AccessDeniedException("Denied");
        ResponseEntity<Map<String, Object>> response = handler.handleAccessDenied(ex);

        assertNotNull(response.getBody().get("timestamp"));
    }

    // ── IllegalArgumentException ───────────────────────────

    @Test
    void handleIllegalArgument_ShouldReturn400() {
        IllegalArgumentException ex = new IllegalArgumentException("Invalid data");
        ResponseEntity<Map<String, Object>> response = handler.handleIllegalArgument(ex);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals(400, response.getBody().get("status"));
        assertEquals("Bad Request", response.getBody().get("error"));
    }

    @Test
    void handleIllegalArgument_ShouldContainMessage() {
        IllegalArgumentException ex = new IllegalArgumentException("Bad booking data");
        ResponseEntity<Map<String, Object>> response = handler.handleIllegalArgument(ex);

        assertEquals("Bad booking data", response.getBody().get("message"));
    }

    // ── IllegalStateException → 409 ────────────────────────

    @Test
    void handleIllegalState_ShouldReturn409() {
        IllegalStateException ex = new IllegalStateException("This session is already full (capacity: 20)");
        ResponseEntity<Map<String, Object>> response = handler.handleIllegalState(ex);

        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
        assertEquals(409, response.getBody().get("status"));
        assertEquals("Conflict", response.getBody().get("error"));
    }

    @Test
    void handleIllegalState_ShouldContainMessage() {
        IllegalStateException ex = new IllegalStateException("You have already booked this session");
        ResponseEntity<Map<String, Object>> response = handler.handleIllegalState(ex);

        assertEquals("You have already booked this session", response.getBody().get("message"));
    }

    // ── General Exception ──────────────────────────────────

    @Test
    void handleGeneral_ShouldReturn500() {
        Exception ex = new Exception("Internal error");
        ResponseEntity<Map<String, Object>> response = handler.handleGeneral(ex);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertEquals(500, response.getBody().get("status"));
        assertEquals("Internal Server Error", response.getBody().get("error"));
    }

    @Test
    void handleGeneral_ShouldContainExceptionMessage() {
        Exception ex = new Exception("DB connection failed");
        ResponseEntity<Map<String, Object>> response = handler.handleGeneral(ex);

        String message = (String) response.getBody().get("message");
        assertTrue(message.contains("DB connection failed"));
    }

    @Test
    void handleGeneral_ShouldContainTimestamp() {
        Exception ex = new Exception("error");
        ResponseEntity<Map<String, Object>> response = handler.handleGeneral(ex);

        assertNotNull(response.getBody().get("timestamp"));
    }

    @Test
    void handleGeneral_ShouldContainUnexpectedErrorPrefix() {
        Exception ex = new Exception("some error");
        ResponseEntity<Map<String, Object>> response = handler.handleGeneral(ex);

        String message = (String) response.getBody().get("message");
        assertTrue(message.startsWith("An unexpected error occurred:"));
    }
}
