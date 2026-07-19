package com.gympro.member.model;

import com.gympro.member.entity.Member;
import com.gympro.member.exception.AccessDeniedException;
import com.gympro.member.exception.MemberNotFoundException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for Member entity (getters/setters via Lombok), and exception classes.
 * These ensure the model and exception layers contribute to coverage.
 */
public class MemberModelAndExceptionTest {

    // ─── Member entity ────────────────────────────────────────────────────────

    @Test
    void member_DefaultConstructor_ShouldCreateInstance() {
        Member member = new Member();
        assertNotNull(member);
        assertNull(member.getId());
        assertNull(member.getName());
        assertNull(member.getEmail());
        assertNull(member.getPhone());
        assertNull(member.getAddress());
        assertNull(member.getGender());
        assertNull(member.getStatus());
    }

    @Test
    void member_AllArgsConstructor_ShouldSetAllFields() {
        Member member = new Member(1L, "Alice", "alice@example.com", "9999999999", "456 Elm St", "Female", "ACTIVE");

        assertEquals(1L, member.getId());
        assertEquals("Alice", member.getName());
        assertEquals("alice@example.com", member.getEmail());
        assertEquals("9999999999", member.getPhone());
        assertEquals("456 Elm St", member.getAddress());
        assertEquals("Female", member.getGender());
        assertEquals("ACTIVE", member.getStatus());
    }

    @Test
    void member_SettersAndGetters_ShouldWorkCorrectly() {
        Member member = new Member();
        member.setId(10L);
        member.setName("Bob");
        member.setEmail("bob@example.com");
        member.setPhone("1234567890");
        member.setAddress("789 Oak Ave");
        member.setGender("Male");
        member.setStatus("INACTIVE");

        assertEquals(10L, member.getId());
        assertEquals("Bob", member.getName());
        assertEquals("bob@example.com", member.getEmail());
        assertEquals("1234567890", member.getPhone());
        assertEquals("789 Oak Ave", member.getAddress());
        assertEquals("Male", member.getGender());
        assertEquals("INACTIVE", member.getStatus());
    }

    @Test
    void member_EqualsAndHashCode_ShouldBeConsistent() {
        Member m1 = new Member(1L, "Alice", "alice@example.com", "9999999999", "456 Elm St", "Female", "ACTIVE");
        Member m2 = new Member(1L, "Alice", "alice@example.com", "9999999999", "456 Elm St", "Female", "ACTIVE");

        assertEquals(m1, m2);
        assertEquals(m1.hashCode(), m2.hashCode());
    }

    @Test
    void member_ToString_ShouldNotBeNull() {
        Member member = new Member(1L, "Alice", "alice@example.com", "9999999999", "456 Elm St", "Female", "ACTIVE");
        assertNotNull(member.toString());
        assertTrue(member.toString().contains("Alice"));
    }

    @Test
    void member_NotEqualToDifferentInstance() {
        Member m1 = new Member(1L, "Alice", "alice@example.com", null, null, null, "ACTIVE");
        Member m2 = new Member(2L, "Bob", "bob@example.com", null, null, null, "INACTIVE");

        assertNotEquals(m1, m2);
    }

    // ─── MemberNotFoundException ──────────────────────────────────────────────

    @Test
    void memberNotFoundException_WithId_ShouldContainIdInMessage() {
        MemberNotFoundException ex = new MemberNotFoundException(7L);

        assertNotNull(ex.getMessage());
        assertTrue(ex.getMessage().contains("7"));
        assertTrue(ex instanceof RuntimeException);
    }

    @Test
    void memberNotFoundException_WithString_ShouldUseProvidedMessage() {
        MemberNotFoundException ex = new MemberNotFoundException("Custom error message");

        assertEquals("Custom error message", ex.getMessage());
    }

    @Test
    void memberNotFoundException_ShouldBeThrowable() {
        assertThrows(MemberNotFoundException.class, () -> {
            throw new MemberNotFoundException(1L);
        });
    }

    // ─── AccessDeniedException ────────────────────────────────────────────────

    @Test
    void accessDeniedException_ShouldContainProvidedMessage() {
        AccessDeniedException ex = new AccessDeniedException("Access denied ❌ ADMIN only");

        assertEquals("Access denied ❌ ADMIN only", ex.getMessage());
        assertTrue(ex instanceof RuntimeException);
    }

    @Test
    void accessDeniedException_ShouldBeThrowable() {
        assertThrows(AccessDeniedException.class, () -> {
            throw new AccessDeniedException("No access");
        });
    }
}
