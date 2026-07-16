package com.gympro.chatbot.controller;

import com.gympro.chatbot.dto.ChatRequest;
import com.gympro.chatbot.dto.ChatResponse;
import com.gympro.chatbot.service.ChatbotService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * REST controller for the GymPro Chatbot.
 *
 * <p>All routes are under /chatbot/** which is:
 * <ul>
 *   <li>Declared permit-all in chatbot-service SecurityConfig</li>
 *   <li>Added to PUBLIC_URLS in gateway JwtAuthFilter (no JWT required at gateway level)</li>
 *   <li>Routed by API Gateway via: spring.cloud.gateway.routes[7] → lb://chatbot-service</li>
 * </ul>
 *
 * <p>Endpoints:
 * <pre>
 *   POST   /chatbot/chat                  — send a message, receive a Gemini reply
 *   DELETE /chatbot/conversation/{id}      — clear session history
 *   GET    /chatbot/health                 — custom liveness probe
 * </pre>
 *
 * <p>This controller is UNCHANGED from the Groq version — the AI provider swap is
 * entirely inside {@link ChatbotService} and {@link com.gympro.chatbot.config.GeminiConfig}.
 */
@RestController
@RequestMapping("/chatbot")
@CrossOrigin(origins = "*")   // Gateway handles CORS globally; this is a service-level safety net
public class ChatbotController {

    private static final Logger log = LoggerFactory.getLogger(ChatbotController.class);

    private final ChatbotService chatbotService;

    public ChatbotController(ChatbotService chatbotService) {
        this.chatbotService = chatbotService;
    }

    /**
     * Main chat endpoint.
     *
     * <p>Request body:
     * <pre>
     * {
     *   "message":        "How do I book a session?",   // required
     *   "role":           "MEMBER",                     // MEMBER | TRAINER | ADMIN (optional, defaults to MEMBER)
     *   "conversationId": "uuid-string"                 // optional; omit to start a new conversation
     * }
     * </pre>
     *
     * <p>Response:
     * <pre>
     * {
     *   "reply":          "...",
     *   "conversationId": "uuid-string",
     *   "timestamp":      "2025-01-01T12:00:00"
     * }
     * </pre>
     */
    @PostMapping("/chat")
    public ResponseEntity<ChatResponse> chat(@RequestBody ChatRequest request) {
        log.info("POST /chatbot/chat | role={} | convId={} | msgLen={}",
                request.getRole(),
                request.getConversationId(),
                request.getMessage() != null ? request.getMessage().length() : 0);

        // Validate: empty message
        if (request.getMessage() == null || request.getMessage().isBlank()) {
            log.warn("Empty message received | convId={}", request.getConversationId());
            ChatResponse emptyReply = new ChatResponse(
                    "Please enter a message so I can help you! 😊",
                    request.getConversationId(),
                    LocalDateTime.now()
            );
            return ResponseEntity.badRequest().body(emptyReply);
        }

        // Sanitize: cap at 1000 characters to prevent prompt injection / token abuse
        if (request.getMessage().length() > 1000) {
            request.setMessage(request.getMessage().substring(0, 1000));
            log.warn("Message truncated to 1000 chars | convId={}", request.getConversationId());
        }

        ChatResponse response = chatbotService.chat(request);

        log.info("POST /chatbot/chat | convId={} | replyLen={}",
                response.getConversationId(),
                response.getReply() != null ? response.getReply().length() : 0);

        return ResponseEntity.ok(response);
    }

    /**
     * Clears in-memory conversation history for the given session.
     * Call this when the user starts a "New Chat" or logs out.
     */
    @DeleteMapping("/conversation/{conversationId}")
    public ResponseEntity<Map<String, String>> clearConversation(
            @PathVariable String conversationId) {
        log.info("DELETE /chatbot/conversation/{}", conversationId);
        chatbotService.clearConversation(conversationId);
        Map<String, String> body = new LinkedHashMap<>();
        body.put("status", "cleared");
        body.put("conversationId", conversationId);
        return ResponseEntity.ok(body);
    }

    /**
     * Custom health check endpoint — also callable without a JWT from outside the gateway.
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> health() {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("service", "chatbot-service");
        body.put("status", "UP");
        body.put("aiProvider", "Google Gemini (gemini-2.5-flash)");
        body.put("activeConversations", chatbotService.getActiveConversationCount());
        body.put("timestamp", LocalDateTime.now().toString());
        return ResponseEntity.ok(body);
    }
}
