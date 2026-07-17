package com.gympro.chatbot.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Incoming chat request from the client.
 *
 * <ul>
 *   <li>{@code message}        — the user's message text (required, max 1 000 chars after controller truncation)</li>
 *   <li>{@code role}           — MEMBER | TRAINER | ADMIN (used for context-aware Gemini system instruction)</li>
 *   <li>{@code conversationId} — UUID session token; null/absent = new conversation</li>
 * </ul>
 *
 * <p>This DTO is UNCHANGED from the Groq version. The same JSON contract is preserved.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChatRequest {

    private String message;

    /** MEMBER | TRAINER | ADMIN — defaults to MEMBER if null/empty */
    private String role;

    /** UUID-style session token; null = new conversation */
    private String conversationId;
}
