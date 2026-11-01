package com.gympro.chatbot.dto;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link ChatRequest}.
 * Verifies Lombok-generated constructors, getters, setters, equals, hashCode and toString.
 */
class ChatRequestTest {

    @Test
    @DisplayName("No-args constructor creates an instance with null fields")
    void noArgsConstructor_createsEmptyInstance() {
        ChatRequest request = new ChatRequest();

        assertThat(request.getMessage()).isNull();
        assertThat(request.getRole()).isNull();
        assertThat(request.getConversationId()).isNull();
    }

    @Test
    @DisplayName("All-args constructor sets all fields correctly")
    void allArgsConstructor_setsAllFields() {
        ChatRequest request = new ChatRequest("Hello", "MEMBER", "conv-123");

        assertThat(request.getMessage()).isEqualTo("Hello");
        assertThat(request.getRole()).isEqualTo("MEMBER");
        assertThat(request.getConversationId()).isEqualTo("conv-123");
    }

    @Test
    @DisplayName("Setters update field values")
    void setters_updateFields() {
        ChatRequest request = new ChatRequest();

        request.setMessage("How do I book a session?");
        request.setRole("TRAINER");
        request.setConversationId("conv-999");

        assertThat(request.getMessage()).isEqualTo("How do I book a session?");
        assertThat(request.getRole()).isEqualTo("TRAINER");
        assertThat(request.getConversationId()).isEqualTo("conv-999");
    }

    @Test
    @DisplayName("equals and hashCode are consistent for identical field values")
    void equalsAndHashCode_consistentForEqualObjects() {
        ChatRequest a = new ChatRequest("msg", "MEMBER", "conv-1");
        ChatRequest b = new ChatRequest("msg", "MEMBER", "conv-1");

        assertThat(a).isEqualTo(b);
        assertThat(a.hashCode()).isEqualTo(b.hashCode());
    }

    @Test
    @DisplayName("equals returns false for objects with different field values")
    void equals_falseForDifferentValues() {
        ChatRequest a = new ChatRequest("msg1", "MEMBER", "conv-1");
        ChatRequest b = new ChatRequest("msg2", "TRAINER", "conv-2");

        assertThat(a).isNotEqualTo(b);
    }

    @Test
    @DisplayName("equals returns false when compared to null or a different type")
    void equals_falseForNullOrDifferentType() {
        ChatRequest a = new ChatRequest("msg", "MEMBER", "conv-1");

        assertThat(a).isNotEqualTo(null);
        assertThat(a).isNotEqualTo("some string");
    }

    @Test
    @DisplayName("equals returns true when compared to itself")
    void equals_trueForSameInstance() {
        ChatRequest a = new ChatRequest("msg", "MEMBER", "conv-1");

        assertThat(a).isEqualTo(a);
    }

    @Test
    @DisplayName("toString includes field values")
    void toString_includesFieldValues() {
        ChatRequest request = new ChatRequest("Hello", "MEMBER", "conv-123");

        String result = request.toString();

        assertThat(result).contains("Hello", "MEMBER", "conv-123");
    }
}
