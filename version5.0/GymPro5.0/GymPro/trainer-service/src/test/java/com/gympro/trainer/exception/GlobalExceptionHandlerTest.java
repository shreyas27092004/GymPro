package com.gympro.trainer.exception;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("GlobalExceptionHandler Unit Tests")
class GlobalExceptionHandlerTest {

    private GlobalExceptionHandler handler;

    @BeforeEach
    void setUp() {
        handler = new GlobalExceptionHandler();
    }

    // ── TrainerNotFoundException → 404 ─────────────────────────

    @Test
    @DisplayName("handleTrainerNotFound - should return 404 with message")
    void handleTrainerNotFound_shouldReturn404() {
        TrainerNotFoundException ex = new TrainerNotFoundException(42L);

        ResponseEntity<Map<String, Object>> response = handler.handleTrainerNotFound(ex);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(404, response.getBody().get("status"));
        assertEquals("Not Found", response.getBody().get("error"));
        assertTrue(response.getBody().get("message").toString().contains("42"));
        assertNotNull(response.getBody().get("timestamp"));
    }

    @Test
    @DisplayName("handleTrainerNotFound - string constructor variant")
    void handleTrainerNotFound_stringMessage_shouldReturn404() {
        TrainerNotFoundException ex = new TrainerNotFoundException("Custom trainer message");

        ResponseEntity<Map<String, Object>> response = handler.handleTrainerNotFound(ex);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertEquals("Custom trainer message", response.getBody().get("message"));
    }

    // ── ScheduleNotFoundException → 404 ────────────────────────

    @Test
    @DisplayName("handleScheduleNotFound - should return 404 with message")
    void handleScheduleNotFound_shouldReturn404() {
        ScheduleNotFoundException ex = new ScheduleNotFoundException(10L);

        ResponseEntity<Map<String, Object>> response = handler.handleScheduleNotFound(ex);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(404, response.getBody().get("status"));
        assertTrue(response.getBody().get("message").toString().contains("10"));
    }

    @Test
    @DisplayName("handleScheduleNotFound - string constructor variant")
    void handleScheduleNotFound_stringMessage_shouldReturn404() {
        ScheduleNotFoundException ex = new ScheduleNotFoundException("Slot not available");

        ResponseEntity<Map<String, Object>> response = handler.handleScheduleNotFound(ex);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertEquals("Slot not available", response.getBody().get("message"));
    }

    // ── AccessDeniedException → 403 ────────────────────────────

    @Test
    @DisplayName("handleAccessDenied - role variant should return 403")
    void handleAccessDenied_withRole_shouldReturn403() {
        AccessDeniedException ex = new AccessDeniedException("MEMBER");

        ResponseEntity<Map<String, Object>> response = handler.handleAccessDenied(ex);

        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(403, response.getBody().get("status"));
        assertEquals("Forbidden", response.getBody().get("error"));
        assertTrue(response.getBody().get("message").toString().contains("MEMBER"));
    }

    @Test
    @DisplayName("handleAccessDenied - no-arg constructor should return 403")
    void handleAccessDenied_noRole_shouldReturn403() {
        AccessDeniedException ex = new AccessDeniedException();

        ResponseEntity<Map<String, Object>> response = handler.handleAccessDenied(ex);

        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        assertTrue(response.getBody().get("message").toString().contains("insufficient permissions"));
    }

    // ── DuplicateScheduleException → 409 ───────────────────────

    @Test
    @DisplayName("handleDuplicateSchedule - should return 409 with message")
    void handleDuplicateSchedule_shouldReturn409() {
        DuplicateScheduleException ex = new DuplicateScheduleException("Trainer #1 already has a session on 2026-07-15 at 10:00");

        ResponseEntity<Map<String, Object>> response = handler.handleDuplicateSchedule(ex);

        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(409, response.getBody().get("status"));
        assertEquals("Conflict", response.getBody().get("error"));
        assertTrue(response.getBody().get("message").toString().contains("already has a session"));
    }

    // ── IllegalStateException → 409 ─────────────────────────────

    @Test
    @DisplayName("handleIllegalState - should return 409 with message")
    void handleIllegalState_shouldReturn409() {
        IllegalStateException ex = new IllegalStateException("Cannot delete a booked slot");

        ResponseEntity<Map<String, Object>> response = handler.handleIllegalState(ex);

        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
        assertEquals("Cannot delete a booked slot", response.getBody().get("message"));
    }

    // ── IllegalArgumentException → 400 ─────────────────────────

    @Test
    @DisplayName("handleIllegalArgument - should return 400")
    void handleIllegalArgument_shouldReturn400() {
        IllegalArgumentException ex = new IllegalArgumentException("Invalid input provided");

        ResponseEntity<Map<String, Object>> response = handler.handleIllegalArgument(ex);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(400, response.getBody().get("status"));
        assertEquals("Invalid input provided", response.getBody().get("message"));
    }

    @Test
    @DisplayName("handleIllegalArgument - null message should still return 400")
    void handleIllegalArgument_nullMessage_shouldReturn400() {
        IllegalArgumentException ex = new IllegalArgumentException((String) null);

        ResponseEntity<Map<String, Object>> response = handler.handleIllegalArgument(ex);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }

    // ── Generic Exception → 500 ────────────────────────────────

    @Test
    @DisplayName("handleGeneric - should return 500 with wrapped message")
    void handleGeneric_shouldReturn500() {
        Exception ex = new RuntimeException("Something went wrong");

        ResponseEntity<Map<String, Object>> response = handler.handleGeneric(ex);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(500, response.getBody().get("status"));
        assertTrue(response.getBody().get("message").toString().contains("Something went wrong"));
    }

    @Test
    @DisplayName("handleGeneric - NullPointerException should return 500")
    void handleGeneric_nullPointer_shouldReturn500() {
        Exception ex = new NullPointerException("Null reference");

        ResponseEntity<Map<String, Object>> response = handler.handleGeneric(ex);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertEquals("Internal Server Error", response.getBody().get("error"));
    }

    // ── Response body structure ─────────────────────────────────

    @Test
    @DisplayName("Response body always contains timestamp, status, error, message keys")
    void responseBody_alwaysContainsRequiredKeys() {
        TrainerNotFoundException ex = new TrainerNotFoundException(1L);

        ResponseEntity<Map<String, Object>> response = handler.handleTrainerNotFound(ex);
        Map<String, Object> body = response.getBody();

        assertNotNull(body);
        assertTrue(body.containsKey("timestamp"));
        assertTrue(body.containsKey("status"));
        assertTrue(body.containsKey("error"));
        assertTrue(body.containsKey("message"));
    }
}
