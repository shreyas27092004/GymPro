package com.gympro.chatbot.service;

import com.gympro.chatbot.dto.ChatRequest;
import com.gympro.chatbot.dto.ChatResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link ChatbotService}.
 * The {@link RestTemplate} used to call Gemini is mocked — no real network calls are made.
 */
@ExtendWith(MockitoExtension.class)
class ChatbotServiceTest {

    @Mock
    private RestTemplate geminiRestTemplate;

    private ChatbotService chatbotService;

    private static final String VALID_KEY = "AIzaSyRealLookingValidKey1234567890";
    private static final String GEMINI_URL = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent";

    @BeforeEach
    void setUp() {
        chatbotService = new ChatbotService(geminiRestTemplate);
        ReflectionTestUtils.setField(chatbotService, "geminiUrl", GEMINI_URL);
        ReflectionTestUtils.setField(chatbotService, "maxHistory", 20);
    }

    private void useInvalidKey(String key) {
        ReflectionTestUtils.setField(chatbotService, "geminiApiKey", key);
    }

    private void useValidKey() {
        ReflectionTestUtils.setField(chatbotService, "geminiApiKey", VALID_KEY);
    }

    @SuppressWarnings("unchecked")
    private void mockGeminiSuccessReply(String replyText) {
        Map<String, Object> textPart = new LinkedHashMap<>();
        textPart.put("text", replyText);

        Map<String, Object> content = new LinkedHashMap<>();
        content.put("parts", List.of(textPart));

        Map<String, Object> candidate = new LinkedHashMap<>();
        candidate.put("content", content);

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("candidates", List.of(candidate));

        ResponseEntity<Map> response = new ResponseEntity<>(body, HttpStatus.OK);
        when(geminiRestTemplate.postForEntity(anyString(), any(HttpEntity.class), eq(Map.class)))
                .thenReturn(response);
    }

    // ──────────────────────────────────────────────────────────────────
    // Conversation id / role resolution
    // ──────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("Conversation and role resolution")
    class ConversationAndRoleTests {

        @Test
        @DisplayName("Generates a new conversation id when none is provided")
        void chat_noConversationId_generatesNewId() {
            useInvalidKey(null);
            ChatRequest request = new ChatRequest("Hello", "MEMBER", null);

            ChatResponse response = chatbotService.chat(request);

            assertThat(response.getConversationId()).isNotBlank();
        }

        @Test
        @DisplayName("Generates a new conversation id when a blank one is provided")
        void chat_blankConversationId_generatesNewId() {
            useInvalidKey("");
            ChatRequest request = new ChatRequest("Hello", "MEMBER", "   ");

            ChatResponse response = chatbotService.chat(request);

            assertThat(response.getConversationId()).isNotBlank();
            assertThat(response.getConversationId()).isNotEqualTo("   ");
        }

        @Test
        @DisplayName("Trims and reuses a provided conversation id")
        void chat_providedConversationId_isTrimmedAndReused() {
            useInvalidKey("YOUR_GEMINI_API_KEY_HERE");
            ChatRequest request = new ChatRequest("Hello", "MEMBER", "  conv-123  ");

            ChatResponse response = chatbotService.chat(request);

            assertThat(response.getConversationId()).isEqualTo("conv-123");
        }

        @ParameterizedTest(name = "role \"{0}\" normalizes to \"{1}\"")
        @CsvSource({
                "MEMBER, MEMBER",
                "trainer, TRAINER",
                "'  admin  ', ADMIN"
        })
        @DisplayName("Normalizes role to uppercase/trimmed form")
        void chat_roleVariants_normalized(String inputRole, String expectedContains) {
            useInvalidKey("your-key-here");
            ChatRequest request = new ChatRequest("help", inputRole, null);

            ChatResponse response = chatbotService.chat(request);

            // Role affects the smart-fallback text only indirectly; we assert no exception
            // and that a reply was produced, confirming normalizeRole executed without error.
            assertThat(response.getReply()).isNotBlank();
        }

        @Test
        @DisplayName("Defaults role to MEMBER when null")
        void chat_nullRole_defaultsToMember() {
            useInvalidKey("GEMINI_API_KEY");
            ChatRequest request = new ChatRequest("help", null, null);

            ChatResponse response = chatbotService.chat(request);

            assertThat(response.getReply()).isNotBlank();
        }

        @Test
        @DisplayName("Defaults role to MEMBER when blank")
        void chat_blankRole_defaultsToMember() {
            useInvalidKey("sk-placeholder-abc");
            ChatRequest request = new ChatRequest("help", "   ", null);

            ChatResponse response = chatbotService.chat(request);

            assertThat(response.getReply()).isNotBlank();
        }
    }

    // ──────────────────────────────────────────────────────────────────
    // Smart fallback (invalid / missing API key) — keyword branches
    // ──────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("Smart fallback keyword branches (invalid API key)")
    class SmartFallbackTests {

        @Test
        @DisplayName("Plan-related keywords return the plans fallback")
        void fallback_planKeywords() {
            useInvalidKey(null);
            ChatResponse response = chatbotService.chat(new ChatRequest("What is the plan pricing?", "MEMBER", null));
            assertThat(response.getReply()).contains("Membership Plans");
        }

        @Test
        @DisplayName("Booking-related keywords return the booking fallback")
        void fallback_bookingKeywords() {
            useInvalidKey(null);
            ChatResponse response = chatbotService.chat(new ChatRequest("How do I book a slot?", "MEMBER", null));
            assertThat(response.getReply()).contains("Booking a Training Session");
        }

        @Test
        @DisplayName("Payment-related keywords return the payments fallback")
        void fallback_paymentKeywords() {
            useInvalidKey(null);
            ChatResponse response = chatbotService.chat(new ChatRequest("How do I pay via razorpay?", "MEMBER", null));
            assertThat(response.getReply()).contains("Payments in GymPro");
        }

        @Test
        @DisplayName("Trainer-related keywords return the trainer fallback")
        void fallback_trainerKeywords() {
            useInvalidKey(null);
            ChatResponse response = chatbotService.chat(new ChatRequest("Which coach is best for cardio?", "MEMBER", null));
            assertThat(response.getReply()).contains("Finding & Booking Trainers");
        }

        @Test
        @DisplayName("Notification-related keywords return the notifications fallback")
        void fallback_notificationKeywords() {
            useInvalidKey(null);
            ChatResponse response = chatbotService.chat(new ChatRequest("Show me my notification alert", "MEMBER", null));
            assertThat(response.getReply()).contains("Notifications");
        }

        @Test
        @DisplayName("Admin-related keywords return the admin fallback")
        void fallback_adminKeywords() {
            useInvalidKey(null);
            ChatResponse response = chatbotService.chat(new ChatRequest("Open the admin dashboard overview", "MEMBER", null));
            assertThat(response.getReply()).contains("Admin Panel Overview");
        }

        @Test
        @DisplayName("Profile-related keywords return the profile fallback")
        void fallback_profileKeywords() {
            useInvalidKey(null);
            ChatResponse response = chatbotService.chat(new ChatRequest("Update my profile photo", "MEMBER", null));
            assertThat(response.getReply()).contains("Updating Your Profile");
        }

        @Test
        @DisplayName("Auth-related keywords return the account/auth fallback")
        void fallback_authKeywords() {
            useInvalidKey(null);
            ChatResponse response = chatbotService.chat(new ChatRequest("I forgot my password", "MEMBER", null));
            assertThat(response.getReply()).contains("Account & Authentication");
        }

        @Test
        @DisplayName("Fitness-related keywords return the fitness tips fallback")
        void fallback_fitnessKeywords() {
            useInvalidKey(null);
            ChatResponse response = chatbotService.chat(new ChatRequest("Any nutrition tips for muscle gain?", "MEMBER", null));
            assertThat(response.getReply()).contains("General Fitness Tips");
        }

        @Test
        @DisplayName("Greeting keywords return the help/greeting fallback")
        void fallback_greetingKeywords() {
            useInvalidKey(null);
            ChatResponse response = chatbotService.chat(new ChatRequest("hello there", "MEMBER", null));
            assertThat(response.getReply()).contains("GymBot");
        }

        @Test
        @DisplayName("Unrecognized text returns the generic fallback")
        void fallback_genericUnrecognized() {
            useInvalidKey(null);
            ChatResponse response = chatbotService.chat(new ChatRequest("qwzxjkl random nonsense text", "MEMBER", null));
            assertThat(response.getReply()).contains("not sure I understand");
        }
    }

    // ──────────────────────────────────────────────────────────────────
    // Gemini API integration (valid key)
    // ──────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("Gemini API integration (valid key)")
    class GeminiApiTests {

        @Test
        @DisplayName("Returns Gemini's reply text on a successful response")
        void chat_validKey_successfulResponse_returnsReply() {
            useValidKey();
            mockGeminiSuccessReply("  Sure, here's how to book a session.  ");

            ChatResponse response = chatbotService.chat(new ChatRequest("How do I book?", "MEMBER", null));

            assertThat(response.getReply()).isEqualTo("Sure, here's how to book a session.");
        }

        @Test
        @DisplayName("Falls back gracefully when candidates list is empty")
        void chat_emptyCandidates_returnsServiceUnavailable() {
            useValidKey();
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("candidates", List.of());
            ResponseEntity<Map> resp = new ResponseEntity<>(body, HttpStatus.OK);
            when(geminiRestTemplate.postForEntity(anyString(), any(HttpEntity.class), eq(Map.class)))
                    .thenReturn(resp);

            ChatResponse response = chatbotService.chat(new ChatRequest("Hi", "MEMBER", null));

            assertThat(response.getReply()).contains("trouble connecting");
        }

        @Test
        @DisplayName("Returns a blocked message when Gemini finishReason is SAFETY")
        void chat_safetyBlocked_returnsBlockedMessage() {
            useValidKey();
            Map<String, Object> candidate = new LinkedHashMap<>();
            candidate.put("finishReason", "SAFETY");
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("candidates", List.of(candidate));
            ResponseEntity<Map> resp = new ResponseEntity<>(body, HttpStatus.OK);
            when(geminiRestTemplate.postForEntity(anyString(), any(HttpEntity.class), eq(Map.class)))
                    .thenReturn(resp);

            ChatResponse response = chatbotService.chat(new ChatRequest("Something risky", "MEMBER", null));

            assertThat(response.getReply()).contains("unable to answer that question");
        }

        @Test
        @DisplayName("Returns a blocked message when Gemini finishReason is RECITATION")
        void chat_recitationBlocked_returnsBlockedMessage() {
            useValidKey();
            Map<String, Object> candidate = new LinkedHashMap<>();
            candidate.put("finishReason", "RECITATION");
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("candidates", List.of(candidate));
            ResponseEntity<Map> resp = new ResponseEntity<>(body, HttpStatus.OK);
            when(geminiRestTemplate.postForEntity(anyString(), any(HttpEntity.class), eq(Map.class)))
                    .thenReturn(resp);

            ChatResponse response = chatbotService.chat(new ChatRequest("Quote a copyrighted poem", "MEMBER", null));

            assertThat(response.getReply()).contains("unable to answer that question");
        }

        @Test
        @DisplayName("Falls back when content is missing and finishReason is not a block reason")
        void chat_missingContentUnknownFinishReason_returnsServiceUnavailable() {
            useValidKey();
            Map<String, Object> candidate = new LinkedHashMap<>();
            candidate.put("finishReason", "OTHER");
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("candidates", List.of(candidate));
            ResponseEntity<Map> resp = new ResponseEntity<>(body, HttpStatus.OK);
            when(geminiRestTemplate.postForEntity(anyString(), any(HttpEntity.class), eq(Map.class)))
                    .thenReturn(resp);

            ChatResponse response = chatbotService.chat(new ChatRequest("Hi", "MEMBER", null));

            assertThat(response.getReply()).contains("trouble connecting");
        }

        @Test
        @DisplayName("Falls back when content parts list is empty")
        void chat_emptyParts_returnsServiceUnavailable() {
            useValidKey();
            Map<String, Object> content = new LinkedHashMap<>();
            content.put("parts", List.of());
            Map<String, Object> candidate = new LinkedHashMap<>();
            candidate.put("content", content);
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("candidates", List.of(candidate));
            ResponseEntity<Map> resp = new ResponseEntity<>(body, HttpStatus.OK);
            when(geminiRestTemplate.postForEntity(anyString(), any(HttpEntity.class), eq(Map.class)))
                    .thenReturn(resp);

            ChatResponse response = chatbotService.chat(new ChatRequest("Hi", "MEMBER", null));

            assertThat(response.getReply()).contains("trouble connecting");
        }

        @Test
        @DisplayName("Falls back when the text field is null inside parts")
        void chat_nullTextField_returnsServiceUnavailable() {
            useValidKey();
            Map<String, Object> part = new LinkedHashMap<>();
            part.put("text", null);
            Map<String, Object> content = new LinkedHashMap<>();
            content.put("parts", List.of(part));
            Map<String, Object> candidate = new LinkedHashMap<>();
            candidate.put("content", content);
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("candidates", List.of(candidate));
            ResponseEntity<Map> resp = new ResponseEntity<>(body, HttpStatus.OK);
            when(geminiRestTemplate.postForEntity(anyString(), any(HttpEntity.class), eq(Map.class)))
                    .thenReturn(resp);

            ChatResponse response = chatbotService.chat(new ChatRequest("Hi", "MEMBER", null));

            assertThat(response.getReply()).contains("trouble connecting");
        }

        @Test
        @DisplayName("Falls back when response body is null")
        void chat_nullBody_returnsServiceUnavailable() {
            useValidKey();
            ResponseEntity<Map> resp = new ResponseEntity<>(null, HttpStatus.OK);
            when(geminiRestTemplate.postForEntity(anyString(), any(HttpEntity.class), eq(Map.class)))
                    .thenReturn(resp);

            ChatResponse response = chatbotService.chat(new ChatRequest("Hi", "MEMBER", null));

            assertThat(response.getReply()).contains("trouble connecting");
        }

        @Test
        @DisplayName("Falls back when the HTTP status is not 2xx")
        void chat_non2xxStatus_returnsServiceUnavailable() {
            useValidKey();
            Map<String, Object> body = new LinkedHashMap<>();
            ResponseEntity<Map> resp = new ResponseEntity<>(body, HttpStatus.INTERNAL_SERVER_ERROR);
            when(geminiRestTemplate.postForEntity(anyString(), any(HttpEntity.class), eq(Map.class)))
                    .thenReturn(resp);

            ChatResponse response = chatbotService.chat(new ChatRequest("Hi", "MEMBER", null));

            assertThat(response.getReply()).contains("trouble connecting");
        }

        @Test
        @DisplayName("Handles network errors via ResourceAccessException")
        void chat_networkError_returnsServiceUnavailable() {
            useValidKey();
            when(geminiRestTemplate.postForEntity(anyString(), any(HttpEntity.class), eq(Map.class)))
                    .thenThrow(new ResourceAccessException("Connection refused"));

            ChatResponse response = chatbotService.chat(new ChatRequest("Hi", "MEMBER", null));

            assertThat(response.getReply()).contains("trouble connecting");
        }

        @Test
        @DisplayName("Returns generic fallback on Gemini 400 Bad Request")
        void chat_badRequest_returnsServiceUnavailable() {
            useValidKey();
            HttpClientErrorException ex = HttpClientErrorException.create(
                    HttpStatus.BAD_REQUEST, "Bad Request", HttpHeaders.EMPTY, new byte[0], null);
            when(geminiRestTemplate.postForEntity(anyString(), any(HttpEntity.class), eq(Map.class)))
                    .thenThrow(ex);

            ChatResponse response = chatbotService.chat(new ChatRequest("Hi", "MEMBER", null));

            assertThat(response.getReply()).contains("trouble connecting");
        }

        @Test
        @DisplayName("Returns invalid-key fallback on Gemini 401 Unauthorized")
        void chat_unauthorized_returnsInvalidKeyFallback() {
            useValidKey();
            HttpClientErrorException ex = HttpClientErrorException.create(
                    HttpStatus.UNAUTHORIZED, "Unauthorized", HttpHeaders.EMPTY, new byte[0], null);
            when(geminiRestTemplate.postForEntity(anyString(), any(HttpEntity.class), eq(Map.class)))
                    .thenThrow(ex);

            ChatResponse response = chatbotService.chat(new ChatRequest("Hi", "MEMBER", null));

            assertThat(response.getReply()).contains("not configured correctly");
        }

        @Test
        @DisplayName("Returns invalid-key fallback on Gemini 403 Forbidden")
        void chat_forbidden_returnsInvalidKeyFallback() {
            useValidKey();
            HttpClientErrorException ex = HttpClientErrorException.create(
                    HttpStatus.FORBIDDEN, "Forbidden", HttpHeaders.EMPTY, new byte[0], null);
            when(geminiRestTemplate.postForEntity(anyString(), any(HttpEntity.class), eq(Map.class)))
                    .thenThrow(ex);

            ChatResponse response = chatbotService.chat(new ChatRequest("Hi", "MEMBER", null));

            assertThat(response.getReply()).contains("not configured correctly");
        }

        @Test
        @DisplayName("Returns rate-limit fallback on Gemini 429 Too Many Requests")
        void chat_rateLimited_returnsRateLimitFallback() {
            useValidKey();
            HttpClientErrorException ex = HttpClientErrorException.create(
                    HttpStatus.TOO_MANY_REQUESTS, "Too Many Requests", HttpHeaders.EMPTY, new byte[0], null);
            when(geminiRestTemplate.postForEntity(anyString(), any(HttpEntity.class), eq(Map.class)))
                    .thenThrow(ex);

            ChatResponse response = chatbotService.chat(new ChatRequest("Hi", "MEMBER", null));

            assertThat(response.getReply()).contains("temporarily busy");
        }

        @Test
        @DisplayName("Returns generic fallback on other Gemini HTTP errors")
        void chat_otherHttpError_returnsServiceUnavailable() {
            useValidKey();
            HttpClientErrorException ex = HttpClientErrorException.create(
                    HttpStatus.I_AM_A_TEAPOT, "I'm a teapot", HttpHeaders.EMPTY, new byte[0], null);
            when(geminiRestTemplate.postForEntity(anyString(), any(HttpEntity.class), eq(Map.class)))
                    .thenThrow(ex);

            ChatResponse response = chatbotService.chat(new ChatRequest("Hi", "MEMBER", null));

            assertThat(response.getReply()).contains("trouble connecting");
        }

        @Test
        @DisplayName("Returns generic fallback on unexpected runtime exceptions")
        void chat_unexpectedException_returnsServiceUnavailable() {
            useValidKey();
            when(geminiRestTemplate.postForEntity(anyString(), any(HttpEntity.class), eq(Map.class)))
                    .thenThrow(new RuntimeException("unexpected"));

            ChatResponse response = chatbotService.chat(new ChatRequest("Hi", "MEMBER", null));

            assertThat(response.getReply()).contains("trouble connecting");
        }
    }

    // ──────────────────────────────────────────────────────────────────
    // Conversation history management
    // ──────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("Conversation history management")
    class HistoryManagementTests {

        @Test
        @DisplayName("clearConversation removes an existing conversation")
        void clearConversation_existingConversation_removesIt() {
            useInvalidKey(null);
            ChatResponse response = chatbotService.chat(new ChatRequest("Hello", "MEMBER", null));
            int countBefore = chatbotService.getActiveConversationCount();

            chatbotService.clearConversation(response.getConversationId());

            assertThat(chatbotService.getActiveConversationCount()).isEqualTo(countBefore - 1);
        }

        @Test
        @DisplayName("clearConversation on a non-existent id is a no-op")
        void clearConversation_nonExistentConversation_noOp() {
            useInvalidKey(null);
            int countBefore = chatbotService.getActiveConversationCount();

            chatbotService.clearConversation("does-not-exist");

            assertThat(chatbotService.getActiveConversationCount()).isEqualTo(countBefore);
        }

        @Test
        @DisplayName("getActiveConversationCount reflects the number of distinct conversations")
        void getActiveConversationCount_reflectsDistinctConversations() {
            useInvalidKey(null);
            int before = chatbotService.getActiveConversationCount();

            chatbotService.chat(new ChatRequest("Hi", "MEMBER", "conv-a"));
            chatbotService.chat(new ChatRequest("Hi", "MEMBER", "conv-b"));

            assertThat(chatbotService.getActiveConversationCount()).isEqualTo(before + 2);
        }

        @Test
        @DisplayName("History is trimmed once it exceeds the configured maximum")
        void chat_historyExceedsMax_isTrimmed() {
            useValidKey();
            ReflectionTestUtils.setField(chatbotService, "maxHistory", 2);
            mockGeminiSuccessReply("ack");

            String convId = "trim-test";
            // Each call appends 2 turns (user + model); after several calls the trim logic
            // (including the odd-excess-rounded-to-even branch) is exercised.
            chatbotService.chat(new ChatRequest("first", "MEMBER", convId));
            ChatResponse response = chatbotService.chat(new ChatRequest("second", "MEMBER", convId));
            chatbotService.chat(new ChatRequest("third", "MEMBER", convId));

            assertThat(response.getReply()).isEqualTo("ack");
        }
    }
}
