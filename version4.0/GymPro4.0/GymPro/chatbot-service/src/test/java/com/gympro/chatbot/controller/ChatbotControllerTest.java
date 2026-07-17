package com.gympro.chatbot.controller;

import com.gympro.chatbot.dto.ChatRequest;
import com.gympro.chatbot.dto.ChatResponse;
import com.gympro.chatbot.service.ChatbotService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.LocalDateTime;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link ChatbotController}.
 * The {@link ChatbotService} dependency is mocked — no Gemini calls are made.
 */
@ExtendWith(MockitoExtension.class)
class ChatbotControllerTest {

    @Mock
    private ChatbotService chatbotService;

    private ChatbotController controller;

    @BeforeEach
    void setUp() {
        controller = new ChatbotController(chatbotService);
    }

    // ──────────────────────────────────────────────────────────────────
    // POST /chatbot/chat
    // ──────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("Returns 400 with a friendly message when message is null")
    void chat_nullMessage_returnsBadRequest() {
        ChatRequest request = new ChatRequest(null, "MEMBER", "conv-1");

        ResponseEntity<ChatResponse> response = controller.chat(request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody().getReply()).contains("enter a message");
        assertThat(response.getBody().getConversationId()).isEqualTo("conv-1");
        verify(chatbotService, never()).chat(any());
    }

    @Test
    @DisplayName("Returns 400 with a friendly message when message is blank")
    void chat_blankMessage_returnsBadRequest() {
        ChatRequest request = new ChatRequest("   ", "MEMBER", "conv-2");

        ResponseEntity<ChatResponse> response = controller.chat(request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        verify(chatbotService, never()).chat(any());
    }

    @Test
    @DisplayName("Truncates messages longer than 1000 characters before delegating to the service")
    void chat_longMessage_isTruncatedTo1000Chars() {
        String longMessage = "a".repeat(1500);
        ChatRequest request = new ChatRequest(longMessage, "MEMBER", "conv-3");
        when(chatbotService.chat(any(ChatRequest.class)))
                .thenReturn(new ChatResponse("ok", "conv-3", LocalDateTime.now()));

        controller.chat(request);

        ArgumentCaptor<ChatRequest> captor = ArgumentCaptor.forClass(ChatRequest.class);
        verify(chatbotService).chat(captor.capture());
        assertThat(captor.getValue().getMessage()).hasSize(1000);
    }

    @Test
    @DisplayName("Delegates a valid message to the service and returns 200 OK")
    void chat_validMessage_returnsOkWithServiceResponse() {
        ChatRequest request = new ChatRequest("How do I book a session?", "MEMBER", "conv-4");
        ChatResponse serviceResponse = new ChatResponse("Here's how...", "conv-4", LocalDateTime.now());
        when(chatbotService.chat(request)).thenReturn(serviceResponse);

        ResponseEntity<ChatResponse> response = controller.chat(request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(serviceResponse);
    }

    @Test
    @DisplayName("Handles a null reply from the service gracefully when logging")
    void chat_serviceReturnsNullReply_doesNotThrow() {
        ChatRequest request = new ChatRequest("Hello", "MEMBER", "conv-5");
        ChatResponse serviceResponse = new ChatResponse(null, "conv-5", LocalDateTime.now());
        when(chatbotService.chat(request)).thenReturn(serviceResponse);

        ResponseEntity<ChatResponse> response = controller.chat(request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().getReply()).isNull();
    }

    @Test
    @DisplayName("Exactly 1000-character messages are not truncated")
    void chat_exactly1000Chars_notTruncated() {
        String message = "b".repeat(1000);
        ChatRequest request = new ChatRequest(message, "MEMBER", "conv-6");
        when(chatbotService.chat(any(ChatRequest.class)))
                .thenReturn(new ChatResponse("ok", "conv-6", LocalDateTime.now()));

        controller.chat(request);

        ArgumentCaptor<ChatRequest> captor = ArgumentCaptor.forClass(ChatRequest.class);
        verify(chatbotService).chat(captor.capture());
        assertThat(captor.getValue().getMessage()).hasSize(1000);
    }

    // ──────────────────────────────────────────────────────────────────
    // DELETE /chatbot/conversation/{id}
    // ──────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("Clears the conversation and returns a confirmation body")
    void clearConversation_returnsClearedStatus() {
        ResponseEntity<Map<String, String>> response = controller.clearConversation("conv-123");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).containsEntry("status", "cleared");
        assertThat(response.getBody()).containsEntry("conversationId", "conv-123");
        verify(chatbotService, times(1)).clearConversation("conv-123");
    }

    // ──────────────────────────────────────────────────────────────────
    // GET /chatbot/health
    // ──────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("Returns UP status with active conversation count")
    void health_returnsUpStatusAndActiveConversationCount() {
        when(chatbotService.getActiveConversationCount()).thenReturn(5);

        ResponseEntity<Map<String, Object>> response = controller.health();

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).containsEntry("service", "chatbot-service");
        assertThat(response.getBody()).containsEntry("status", "UP");
        assertThat(response.getBody()).containsEntry("activeConversations", 5);
        assertThat(response.getBody()).containsKey("timestamp");
    }
}
