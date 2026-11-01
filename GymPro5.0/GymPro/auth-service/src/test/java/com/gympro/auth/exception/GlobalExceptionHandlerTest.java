package com.gympro.auth.exception;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link GlobalExceptionHandler}.
 * Called directly as a plain Java object — no Spring context needed.
 */
class GlobalExceptionHandlerTest {

    private GlobalExceptionHandler handler;

    @BeforeEach
    void setUp() {
        handler = new GlobalExceptionHandler();
    }

    // ================================================================
    // EmailAlreadyExistsException → 409 Conflict
    // ================================================================

    @Test
    @DisplayName("handleEmailExists - returns 409 status")
    void handleEmailExists_returns409() {
        EmailAlreadyExistsException ex = new EmailAlreadyExistsException("test@gympro.com");
        ResponseEntity<ErrorResponse> response = handler.handleEmailExists(ex);

        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
    }

    @Test
    @DisplayName("handleEmailExists - body contains error 'Conflict'")
    void handleEmailExists_bodyHasConflictError() {
        EmailAlreadyExistsException ex = new EmailAlreadyExistsException("test@gympro.com");
        ResponseEntity<ErrorResponse> response = handler.handleEmailExists(ex);

        assertNotNull(response.getBody());
        assertEquals(409, response.getBody().getStatus());
        assertEquals("Conflict", response.getBody().getError());
    }

    @Test
    @DisplayName("handleEmailExists - body message contains the email")
    void handleEmailExists_bodyMessageContainsEmail() {
        EmailAlreadyExistsException ex = new EmailAlreadyExistsException("dup@gympro.com");
        ResponseEntity<ErrorResponse> response = handler.handleEmailExists(ex);

        assertTrue(response.getBody().getMessage().contains("dup@gympro.com"));
    }

    // ================================================================
    // UserNotFoundException → 404 Not Found
    // ================================================================

    @Test
    @DisplayName("handleUserNotFound - returns 404 status")
    void handleUserNotFound_returns404() {
        UserNotFoundException ex = new UserNotFoundException("missing@gympro.com");
        ResponseEntity<ErrorResponse> response = handler.handleUserNotFound(ex);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    }

    @Test
    @DisplayName("handleUserNotFound - body contains error 'Not Found'")
    void handleUserNotFound_bodyHasNotFoundError() {
        UserNotFoundException ex = new UserNotFoundException("missing@gympro.com");
        ResponseEntity<ErrorResponse> response = handler.handleUserNotFound(ex);

        assertNotNull(response.getBody());
        assertEquals(404, response.getBody().getStatus());
        assertEquals("Not Found", response.getBody().getError());
    }

    @Test
    @DisplayName("handleUserNotFound - body message contains the email")
    void handleUserNotFound_bodyMessageContainsEmail() {
        UserNotFoundException ex = new UserNotFoundException("nobody@gympro.com");
        ResponseEntity<ErrorResponse> response = handler.handleUserNotFound(ex);

        assertTrue(response.getBody().getMessage().contains("nobody@gympro.com"));
    }

    // ================================================================
    // InvalidCredentialsException → 401 Unauthorized
    // ================================================================

    @Test
    @DisplayName("handleInvalidCredentials - returns 401 status")
    void handleInvalidCredentials_returns401() {
        InvalidCredentialsException ex = new InvalidCredentialsException();
        ResponseEntity<ErrorResponse> response = handler.handleInvalidCredentials(ex);

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
    }

    @Test
    @DisplayName("handleInvalidCredentials - body contains error 'Unauthorized'")
    void handleInvalidCredentials_bodyHasUnauthorizedError() {
        InvalidCredentialsException ex = new InvalidCredentialsException();
        ResponseEntity<ErrorResponse> response = handler.handleInvalidCredentials(ex);

        assertNotNull(response.getBody());
        assertEquals(401, response.getBody().getStatus());
        assertEquals("Unauthorized", response.getBody().getError());
    }

    // ================================================================
    // IllegalArgumentException → 400 Bad Request
    // ================================================================

    @Test
    @DisplayName("handleIllegalArg - returns 400 status")
    void handleIllegalArg_returns400() {
        IllegalArgumentException ex = new IllegalArgumentException("bad input");
        ResponseEntity<ErrorResponse> response = handler.handleIllegalArg(ex);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }

    @Test
    @DisplayName("handleIllegalArg - body contains error 'Bad Request'")
    void handleIllegalArg_bodyHasBadRequestError() {
        IllegalArgumentException ex = new IllegalArgumentException("bad input");
        ResponseEntity<ErrorResponse> response = handler.handleIllegalArg(ex);

        assertNotNull(response.getBody());
        assertEquals(400, response.getBody().getStatus());
        assertEquals("Bad Request", response.getBody().getError());
        assertEquals("bad input", response.getBody().getMessage());
    }

    // ================================================================
    // Generic Exception → 500 Internal Server Error
    // ================================================================

    @Test
    @DisplayName("handleGeneric - returns 500 status")
    void handleGeneric_returns500() {
        Exception ex = new RuntimeException("something broke");
        ResponseEntity<ErrorResponse> response = handler.handleGeneric(ex);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
    }

    @Test
    @DisplayName("handleGeneric - body contains error 'Internal Server Error'")
    void handleGeneric_bodyHasInternalServerError() {
        Exception ex = new RuntimeException("oops");
        ResponseEntity<ErrorResponse> response = handler.handleGeneric(ex);

        assertNotNull(response.getBody());
        assertEquals(500, response.getBody().getStatus());
        assertEquals("Internal Server Error", response.getBody().getError());
    }

    @Test
    @DisplayName("handleGeneric - body message is a safe generic string, not the internal exception message")
    void handleGeneric_bodyMessageIsGenericSafeString() {
        Exception ex = new RuntimeException("internal detail that should not leak");
        ResponseEntity<ErrorResponse> response = handler.handleGeneric(ex);

        // The handler must NOT expose the raw exception message to the client
        assertFalse(response.getBody().getMessage().contains("internal detail"));
        assertTrue(response.getBody().getMessage().toLowerCase().contains("unexpected")
                || response.getBody().getMessage().toLowerCase().contains("error"));
    }
}
