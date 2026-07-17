package com.gympro.chatbot.dto;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link ChatResponse}.
 * Verifies Lombok-generated constructors, getters, setters, equals, hashCode and toString.
 */
class ChatResponseTest {

    @Test
    @DisplayName("No-args constructor creates an instance with null fields")
    void noArgsConstructor_createsEmptyInstance() {
        ChatResponse response = new ChatResponse();

        assertThat(response.getReply()).isNull();
        assertThat(response.getConversationId()).isNull();
        assertThat(response.getTimestamp()).isNull();
    }

    @Test
    @DisplayName("All-args constructor sets all fields correctly")
    void allArgsConstructor_setsAllFields() {
        LocalDateTime now = LocalDateTime.of(2026, 1, 1, 12, 0);

        ChatResponse response = new ChatResponse("Hi there!", "conv-123", now);

        assertThat(response.getReply()).isEqualTo("Hi there!");
        assertThat(response.getConversationId()).isEqualTo("conv-123");
        assertThat(response.getTimestamp()).isEqualTo(now);
    }

    @Test
    @DisplayName("Setters update field values")
    void setters_updateFields() {
        ChatResponse response = new ChatResponse();
        LocalDateTime now = LocalDateTime.now();

        response.setReply("Sure, here's how...");
        response.setConversationId("conv-456");
        response.setTimestamp(now);

        assertThat(response.getReply()).isEqualTo("Sure, here's how...");
        assertThat(response.getConversationId()).isEqualTo("conv-456");
        assertThat(response.getTimestamp()).isEqualTo(now);
    }

    @Test
    @DisplayName("equals and hashCode are consistent for identical field values")
    void equalsAndHashCode_consistentForEqualObjects() {
        LocalDateTime now = LocalDateTime.of(2026, 1, 1, 12, 0);
        ChatResponse a = new ChatResponse("reply", "conv-1", now);
        ChatResponse b = new ChatResponse("reply", "conv-1", now);

        assertThat(a).isEqualTo(b);
        assertThat(a.hashCode()).isEqualTo(b.hashCode());
    }

    @Test
    @DisplayName("equals returns false for objects with different field values")
    void equals_falseForDifferentValues() {
        LocalDateTime now = LocalDateTime.now();
        ChatResponse a = new ChatResponse("reply1", "conv-1", now);
        ChatResponse b = new ChatResponse("reply2", "conv-2", now.plusMinutes(1));

        assertThat(a).isNotEqualTo(b);
    }

    @Test
    @DisplayName("equals returns false when compared to null or a different type")
    void equals_falseForNullOrDifferentType() {
        ChatResponse a = new ChatResponse("reply", "conv-1", LocalDateTime.now());

        assertThat(a).isNotEqualTo(null);
        assertThat(a).isNotEqualTo("some string");
    }

    @Test
    @DisplayName("equals returns true when compared to itself")
    void equals_trueForSameInstance() {
        ChatResponse a = new ChatResponse("reply", "conv-1", LocalDateTime.now());

        assertThat(a).isEqualTo(a);
    }

    @Test
    @DisplayName("toString includes field values")
    void toString_includesFieldValues() {
        ChatResponse response = new ChatResponse("Hello back!", "conv-123", LocalDateTime.of(2026, 1, 1, 12, 0));

        String result = response.toString();

        assertThat(result).contains("Hello back!", "conv-123");
    }
}
