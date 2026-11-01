package com.gympro.notification;

import com.gympro.notification.dto.EmailRequest;
import com.gympro.notification.exception.EmailException;
import com.gympro.notification.exception.GlobalExceptionHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class NotificationModelsAndExceptionsTest {

    private GlobalExceptionHandler handler;

    @BeforeEach
    void setUp() {
        handler = new GlobalExceptionHandler();
    }

    // ─── GlobalExceptionHandler: EmailException → 500 ─────────────────────

    @Test
    void testHandleEmailException_Returns500() {
        EmailException ex = new EmailException("SMTP connection failed");
        ResponseEntity<Map<String, Object>> response = handler.handleEmailException(ex);

        assertEquals(500, response.getStatusCodeValue());
        assertEquals("Internal Server Error", response.getBody().get("error"));
        assertEquals("SMTP connection failed", response.getBody().get("message"));
    }

    // ─── GlobalExceptionHandler: IllegalArgumentException → 400 ──────────

    @Test
    void testHandleIllegalArg_Returns400() {
        IllegalArgumentException ex = new IllegalArgumentException("Recipient email cannot be empty");
        ResponseEntity<Map<String, Object>> response = handler.handleIllegalArg(ex);

        assertEquals(400, response.getStatusCodeValue());
        assertEquals("Bad Request", response.getBody().get("error"));
        assertEquals("Recipient email cannot be empty", response.getBody().get("message"));
    }

    // ─── GlobalExceptionHandler: General Exception → 500 ─────────────────

    @Test
    void testHandleGeneral_Returns500() {
        Exception ex = new Exception("Unexpected runtime failure");
        ResponseEntity<Map<String, Object>> response = handler.handleGeneral(ex);

        assertEquals(500, response.getStatusCodeValue());
        assertTrue(response.getBody().get("message").toString().contains("Unexpected"));
    }

    // ─── Response always contains timestamp ───────────────────────────────

    @Test
    void testResponseBody_HasTimestamp() {
        EmailException ex = new EmailException("error");
        ResponseEntity<Map<String, Object>> response = handler.handleEmailException(ex);

        assertTrue(response.getBody().containsKey("timestamp"));
        assertNotNull(response.getBody().get("timestamp"));
    }

    @Test
    void testResponseBody_HasStatusField() {
        IllegalArgumentException ex = new IllegalArgumentException("bad input");
        ResponseEntity<Map<String, Object>> response = handler.handleIllegalArg(ex);

        assertEquals(400, response.getBody().get("status"));
    }

    // ─── EmailException ────────────────────────────────────────────────────

    @Test
    void testEmailException_MessageConstructor() {
        EmailException ex = new EmailException("Failed to send");
        assertEquals("Failed to send", ex.getMessage());
        assertNull(ex.getCause());
    }

    @Test
    void testEmailException_MessageAndCauseConstructor() {
        RuntimeException cause = new RuntimeException("SMTP error");
        EmailException ex = new EmailException("Send failed", cause);

        assertEquals("Send failed", ex.getMessage());
        assertEquals(cause, ex.getCause());
    }

    @Test
    void testEmailException_IsRuntimeException() {
        EmailException ex = new EmailException("msg");
        assertInstanceOf(RuntimeException.class, ex);
    }

    // ─── EmailRequest DTO ─────────────────────────────────────────────────

    @Test
    void testEmailRequest_SettersAndGetters() {
        EmailRequest req = new EmailRequest();
        req.setTo("recipient@test.com");
        req.setSubject("Test Subject");
        req.setBody("Test body content");

        assertEquals("recipient@test.com", req.getTo());
        assertEquals("Test Subject", req.getSubject());
        assertEquals("Test body content", req.getBody());
    }

    @Test
    void testEmailRequest_DefaultsToNull() {
        EmailRequest req = new EmailRequest();
        assertNull(req.getTo());
        assertNull(req.getSubject());
        assertNull(req.getBody());
    }

    @Test
    void testEmailRequest_EqualsAndHashCode() {
        EmailRequest r1 = new EmailRequest();
        r1.setTo("test@test.com");
        r1.setSubject("Subject");
        r1.setBody("Body");

        EmailRequest r2 = new EmailRequest();
        r2.setTo("test@test.com");
        r2.setSubject("Subject");
        r2.setBody("Body");

        assertEquals(r1, r2);
        assertEquals(r1.hashCode(), r2.hashCode());
    }

    @Test
    void testEmailRequest_ToString() {
        EmailRequest req = new EmailRequest();
        req.setTo("a@b.com");
        String str = req.toString();
        assertNotNull(str);
        assertTrue(str.contains("EmailRequest"));
    }
}
