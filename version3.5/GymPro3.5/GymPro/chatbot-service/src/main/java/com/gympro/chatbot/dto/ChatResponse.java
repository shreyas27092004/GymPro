package com.gympro.chatbot.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Outgoing chat response sent back to the client.
 *
 * <ul>
 *   <li>{@code reply}          — the Gemini-generated (or fallback) answer text</li>
 *   <li>{@code conversationId} — echoed session ID; frontend must store and re-send this</li>
 *   <li>{@code timestamp}      — server time when the reply was generated</li>
 * </ul>
 *
 * <p>This DTO is UNCHANGED from the Groq version. The same JSON contract is preserved.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChatResponse {

    private String reply;
    private String conversationId;
    private LocalDateTime timestamp;
}
