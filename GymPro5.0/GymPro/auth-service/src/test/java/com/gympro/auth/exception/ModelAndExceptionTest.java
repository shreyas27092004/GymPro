package com.gympro.auth.exception;

import com.gympro.auth.dto.AuthResponse;
import com.gympro.auth.dto.LoginRequest;
import com.gympro.auth.entity.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests covering:
 *  - All three custom exception classes
 *  - ErrorResponse
 *  - AuthResponse DTO
 *  - LoginRequest DTO
 *  - User entity
 *
 * These classes have no Spring dependencies, so plain JUnit5 is enough.
 */
class ModelAndExceptionTest {

    // ================================================================
    // EmailAlreadyExistsException
    // ================================================================

    @Test
    @DisplayName("EmailAlreadyExistsException - extends RuntimeException")
    void emailAlreadyExists_extendsRuntimeException() {
        EmailAlreadyExistsException ex = new EmailAlreadyExistsException("a@b.com");
        assertInstanceOf(RuntimeException.class, ex);
    }

    @Test
    @DisplayName("EmailAlreadyExistsException - message contains the email")
    void emailAlreadyExists_messageContainsEmail() {
        EmailAlreadyExistsException ex = new EmailAlreadyExistsException("dup@test.com");
        assertTrue(ex.getMessage().contains("dup@test.com"));
    }

    // ================================================================
    // UserNotFoundException
    // ================================================================

    @Test
    @DisplayName("UserNotFoundException - extends RuntimeException")
    void userNotFound_extendsRuntimeException() {
        UserNotFoundException ex = new UserNotFoundException("nobody@test.com");
        assertInstanceOf(RuntimeException.class, ex);
    }

    @Test
    @DisplayName("UserNotFoundException - message contains the email")
    void userNotFound_messageContainsEmail() {
        UserNotFoundException ex = new UserNotFoundException("ghost@test.com");
        assertTrue(ex.getMessage().contains("ghost@test.com"));
    }

    // ================================================================
    // InvalidCredentialsException
    // ================================================================

    @Test
    @DisplayName("InvalidCredentialsException - extends RuntimeException")
    void invalidCredentials_extendsRuntimeException() {
        InvalidCredentialsException ex = new InvalidCredentialsException();
        assertInstanceOf(RuntimeException.class, ex);
    }

    @Test
    @DisplayName("InvalidCredentialsException - message is not blank")
    void invalidCredentials_messageNotBlank() {
        InvalidCredentialsException ex = new InvalidCredentialsException();
        assertFalse(ex.getMessage().isBlank());
    }

    // ================================================================
    // ErrorResponse
    // ================================================================

    @Test
    @DisplayName("ErrorResponse - constructor sets status, error, message correctly")
    void errorResponse_constructorSetsFields() {
        ErrorResponse er = new ErrorResponse(404, "Not Found", "User not found");
        assertEquals(404, er.getStatus());
        assertEquals("Not Found", er.getError());
        assertEquals("User not found", er.getMessage());
    }

    @Test
    @DisplayName("ErrorResponse - timestamp is automatically set and not null")
    void errorResponse_timestampIsSetAutomatically() {
        ErrorResponse er = new ErrorResponse(500, "Error", "Oops");
        assertNotNull(er.getTimestamp());
    }

    // ================================================================
    // AuthResponse DTO
    // ================================================================

    @Test
    @DisplayName("AuthResponse - all-args constructor stores all fields")
    void authResponse_allArgsConstructor() {
        AuthResponse resp = new AuthResponse("tok", null, "e@e.com", "ADMIN", "ok");
        assertEquals("tok", resp.getToken());
        assertEquals("e@e.com", resp.getEmail());
        assertEquals("ADMIN", resp.getRole());
        assertEquals("ok", resp.getMessage());
    }

    @Test
    @DisplayName("AuthResponse - setters update fields (Lombok @Data)")
    void authResponse_settersWork() {
        AuthResponse resp = new AuthResponse("t", null, "e", "r", "m");
        resp.setToken("newToken");
        resp.setRole("TRAINER");
        assertEquals("newToken", resp.getToken());
        assertEquals("TRAINER", resp.getRole());
    }

    // ================================================================
    // LoginRequest DTO
    // ================================================================

    @Test
    @DisplayName("LoginRequest - setters and getters work (Lombok @Data)")
    void loginRequest_settersAndGetters() {
        LoginRequest req = new LoginRequest();
        req.setEmail("user@gympro.com");
        req.setPassword("secret");
        assertEquals("user@gympro.com", req.getEmail());
        assertEquals("secret", req.getPassword());
    }

    // ================================================================
    // User entity
    // ================================================================

    @Test
    @DisplayName("User - no-args constructor creates empty object")
    void user_noArgsConstructor() {
        User user = new User();
        assertNull(user.getId());
        assertNull(user.getEmail());
    }

    @Test
    @DisplayName("User - all-args constructor stores all fields")
    void user_allArgsConstructor() {
        User user = new User(1L, "Shreyas", "s@gympro.com", "pass", "MEMBER");
        assertEquals(1L, user.getId());
        assertEquals("Shreyas", user.getName());
        assertEquals("s@gympro.com", user.getEmail());
        assertEquals("pass", user.getPassword());
        assertEquals("MEMBER", user.getRole());
    }

    @Test
    @DisplayName("User - setters update all fields")
    void user_settersWork() {
        User user = new User();
        user.setId(5L);
        user.setName("Alice");
        user.setEmail("alice@gympro.com");
        user.setPassword("hashed");
        user.setRole("TRAINER");

        assertEquals(5L, user.getId());
        assertEquals("Alice", user.getName());
        assertEquals("alice@gympro.com", user.getEmail());
        assertEquals("hashed", user.getPassword());
        assertEquals("TRAINER", user.getRole());
    }

    @Test
    @DisplayName("User - equals and hashCode work for same data (Lombok @Data)")
    void user_equalsHashCode() {
        User u1 = new User(1L, "Shreyas", "s@gympro.com", "pass", "MEMBER");
        User u2 = new User(1L, "Shreyas", "s@gympro.com", "pass", "MEMBER");
        assertEquals(u1, u2);
        assertEquals(u1.hashCode(), u2.hashCode());
    }
}
