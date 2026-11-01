package com.gympro.chatbot.service;

import com.gympro.chatbot.dto.ChatRequest;
import com.gympro.chatbot.dto.ChatResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Core chatbot service for GymPro — powered by Google Gemini.
 *
 * <p>Responsibilities:
 * <ol>
 *   <li>Maintain per-session conversation history in a ConcurrentHashMap (thread-safe).</li>
 *   <li>Prepend a rich GymPro system prompt so Gemini stays on topic.</li>
 *   <li>Call the Gemini generateContent REST API with the full history.</li>
 *   <li>Detect placeholder/missing API key and return smart predefined answers.</li>
 *   <li>Trim history to MAX_HISTORY messages to avoid token overflow.</li>
 *   <li>Log all requests, replies, and errors at appropriate levels.</li>
 *   <li>Never throw — always return a graceful response.</li>
 * </ol>
 *
 * <h3>Gemini API request shape</h3>
 * <pre>
 * POST https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent?key=API_KEY
 * {
 *   "systemInstruction": { "parts": [{ "text": "..." }] },
 *   "contents": [
 *     { "role": "user",  "parts": [{ "text": "Hello" }] },
 *     { "role": "model", "parts": [{ "text": "Hi there!" }] },
 *     ...
 *   ],
 *   "generationConfig": { "temperature": 0.7, "maxOutputTokens": 600 }
 * }
 * </pre>
 *
 * <h3>Gemini role mapping</h3>
 * Gemini uses "user" and "model" (NOT "assistant" like OpenAI).
 * The history list stores the Gemini-native role strings from the start.
 */
@Service
public class ChatbotService {

    private static final Logger log = LoggerFactory.getLogger(ChatbotService.class);

    // ── Gemini config (injected from application.properties) ─────────────────
    @Value("${gemini.api.url}")
    private String geminiUrl;

    @Value("${gemini.api.key}")
    private String geminiApiKey;

    @Value("${chatbot.max.history:20}")
    private int maxHistory;

    private final RestTemplate geminiRestTemplate;

    // ── In-memory conversation store: conversationId → ordered Gemini content list ──
    // Each entry is a Map with "role" ("user" | "model") and "parts" (List of Map).
    // ConcurrentHashMap ensures thread-safety without explicit locking.
    private final Map<String, List<Map<String, Object>>> conversationHistories =
            new ConcurrentHashMap<>();

    // ── GymPro System Prompt ──────────────────────────────────────────────────
    // Passed as systemInstruction to Gemini (separate from the conversation turns).
    private static final String GYMPRO_SYSTEM_PROMPT = """
            You are GymBot, the official AI assistant for GymPro — a gym management platform.

            === ABOUT GYMPRO ===
            GymPro connects members, trainers, and admins in a unified platform.

            MEMBERS can:
              • View and subscribe to gym plans (Basic, Standard, Premium)
              • Book training sessions with trainers by selecting a time slot
              • Pay via Razorpay (cards, UPI, net banking, wallets)
              • View booking history and payment history
              • Update their profile, profile photo, and personal info

            TRAINERS can:
              • Set their weekly availability schedule
              • View their assigned sessions and upcoming bookings
              • Update their profile, specializations, and bio
              • View the members they are training

            ADMINS can:
              • Manage all members (activate, deactivate, view details)
              • Manage all trainers (add, remove, update)
              • Create, edit, and delete gym plans
              • View all bookings and payments across the platform
              • Access the overview dashboard with stats and analytics

            === NAVIGATION GUIDE ===
            Member navigation: Dashboard → Plans (subscribe) → Bookings (book session) → Payments (pay/history) → Profile
            Trainer navigation: Dashboard → My Schedule → My Sessions → My Members → Profile
            Admin navigation: Dashboard → Overview → Manage Members → Manage Trainers → Manage Plans → Bookings → Payments

            === GYM PLANS ===
            Plans define how many free sessions a member gets per month before additional charges apply.
            Members must have an active plan to book trainer sessions.
            Plans are managed by admins and can be created/modified in the Admin → Plans page.

            === BOOKING FLOW ===
            1. Member navigates to Bookings page
            2. Selects a trainer from the available trainer list
            3. Picks an available time slot (set by the trainer in their schedule)
            4. Confirms the booking
            5. If sessions exceed the plan quota, a payment is triggered via Razorpay

            === PAYMENT FLOW ===
            GymPro integrates with Razorpay for secure payments.
            Members can pay for plan subscriptions and session fees.
            Full payment history is accessible in the Payments page.

            === NOTIFICATIONS ===
            GymPro sends notifications for booking confirmations, payment receipts, and session reminders.
            Notifications appear in the bell icon at the top of the dashboard.

            === RULES FOR YOUR RESPONSES ===
            - ONLY answer about GymPro features, gym/fitness topics, platform navigation, or general wellness.
            - If a question is completely off-topic (politics, coding, unrelated services), politely decline and redirect.
            - Be friendly, professional, and concise. Use bullet points for multi-step answers.
            - Do not invent features or make up prices. If you don't know a specific value, say so.
            - Format longer answers with clear headings or emoji bullets for readability.
            """;

    public ChatbotService(@Qualifier("geminiRestTemplate") RestTemplate geminiRestTemplate) {
        this.geminiRestTemplate = geminiRestTemplate;
    }

    // ── Public API ─────────────────────────────────────────────────────────────

    /**
     * Process a chat request and return a response.
     * This method NEVER throws — it always returns a valid ChatResponse.
     */
    public ChatResponse chat(ChatRequest request) {
        String conversationId = resolveConversationId(request.getConversationId());
        String role = normalizeRole(request.getRole());

        log.info("Chat request | convId={} | role={} | message='{}'",
                conversationId, role, truncate(request.getMessage(), 120));

        // Get or create conversation history for this session
        List<Map<String, Object>> history = conversationHistories
                .computeIfAbsent(conversationId, id -> new ArrayList<>());

        // Append the user turn (Gemini role = "user")
        history.add(geminiTurn("user", request.getMessage()));

        // Call Gemini or use smart fallback
        String reply = callGeminiWithFallback(history, conversationId, role);

        // Append model reply to history for future turns (Gemini role = "model")
        history.add(geminiTurn("model", reply));

        // Trim history to prevent token overflow
        trimHistory(history);

        log.info("Chat response | convId={} | role={} | replyLen={} | historySize={}",
                conversationId, role, reply.length(), history.size());

        return new ChatResponse(reply, conversationId, LocalDateTime.now());
    }

    /**
     * Clear the in-memory conversation history for a session.
     */
    public void clearConversation(String conversationId) {
        boolean existed = conversationHistories.remove(conversationId) != null;
        log.info("Clear conversation | convId={} | existed={}", conversationId, existed);
    }

    /**
     * Returns the number of active conversations (for health/monitoring).
     */
    public int getActiveConversationCount() {
        return conversationHistories.size();
    }

    // ── Private helpers ────────────────────────────────────────────────────────

    private String resolveConversationId(String requested) {
        if (requested == null || requested.isBlank()) {
            String newId = UUID.randomUUID().toString();
            log.debug("New conversation created | convId={}", newId);
            return newId;
        }
        return requested.trim();
    }

    private String normalizeRole(String role) {
        if (role == null || role.isBlank()) return "MEMBER";
        return role.trim().toUpperCase();
    }

    /**
     * Builds a role-aware system instruction string.
     * This is passed as {@code systemInstruction} in the Gemini request, NOT as a
     * conversation turn (Gemini handles system instructions separately from history).
     */
    private String buildSystemInstruction(String role) {
        return GYMPRO_SYSTEM_PROMPT +
                "\n\n=== CURRENT USER ROLE ===\n" +
                "The user is a " + role + ". " +
                "Tailor your navigation instructions and feature explanations to this role. " +
                "For MEMBER: focus on plans, bookings, payments. " +
                "For TRAINER: focus on schedule, sessions, members. " +
                "For ADMIN: focus on management, overview, analytics.";
    }

    /**
     * Calls the Gemini generateContent API.
     * Falls back to smart predefined answers if the key is invalid or the call fails.
     * Never throws.
     */
    @SuppressWarnings("unchecked")
    private String callGeminiWithFallback(List<Map<String, Object>> history,
                                           String conversationId,
                                           String role) {
        // ── Check for placeholder or missing API key ──────────────────────
        if (!isValidApiKey(geminiApiKey)) {
            log.warn("Gemini API key is missing or placeholder — using smart fallback | convId={}", conversationId);
            return buildSmartFallback(history, role);
        }

        // ── Attempt the real Gemini call ──────────────────────────────────
        try {
            // Build the request body following Gemini's REST schema
            Map<String, Object> requestBody = new LinkedHashMap<>();

            // systemInstruction — role-personalised GymPro prompt
            Map<String, Object> systemInstruction = new LinkedHashMap<>();
            systemInstruction.put("parts", List.of(Map.of("text", buildSystemInstruction(role))));
            requestBody.put("systemInstruction", systemInstruction);

            // contents — the conversation history (user/model turns)
            requestBody.put("contents", history);

            // generationConfig — temperature and token cap
            Map<String, Object> generationConfig = new LinkedHashMap<>();
            generationConfig.put("temperature", 0.7);
            generationConfig.put("maxOutputTokens", 600);
            requestBody.put("generationConfig", generationConfig);

            // Gemini authenticates via ?key= query param
            String urlWithKey = geminiUrl + "?key=" + geminiApiKey;

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

            log.debug("Calling Gemini | historyTurns={} | convId={}", history.size(), conversationId);

            ResponseEntity<Map> response = geminiRestTemplate.postForEntity(urlWithKey, entity, Map.class);

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                // Gemini response: { "candidates": [{ "content": { "parts": [{ "text": "..." }] } }] }
                List<Map<String, Object>> candidates =
                        (List<Map<String, Object>>) response.getBody().get("candidates");

                if (candidates != null && !candidates.isEmpty()) {
                    Map<String, Object> content =
                            (Map<String, Object>) candidates.get(0).get("content");
                    if (content != null) {
                        List<Map<String, Object>> parts =
                                (List<Map<String, Object>>) content.get("parts");
                        if (parts != null && !parts.isEmpty()) {
                            Object text = parts.get(0).get("text");
                            if (text != null) {
                                String reply = text.toString().trim();
                                log.debug("Gemini success | convId={} | replyLen={}", conversationId, reply.length());
                                return reply;
                            }
                        }
                    }
                }

                // Check for content filter block
                List<Map<String, Object>> candidates2 =
                        (List<Map<String, Object>>) response.getBody().get("candidates");
                if (candidates2 != null && !candidates2.isEmpty()) {
                    Object finishReason = candidates2.get(0).get("finishReason");
                    if ("SAFETY".equals(finishReason) || "RECITATION".equals(finishReason)) {
                        log.warn("Gemini blocked response | reason={} | convId={}", finishReason, conversationId);
                        return "⚠️ I'm unable to answer that question. Please ask something related to GymPro or fitness.";
                    }
                }
            }

            log.warn("Unexpected Gemini response structure | status={} | convId={}",
                    response.getStatusCode(), conversationId);
            return serviceUnavailableFallback();

        } catch (ResourceAccessException ex) {
            // Network timeout, connection refused, DNS failure
            log.error("Gemini network error | convId={} | error={}", conversationId, ex.getMessage());
            return serviceUnavailableFallback();
        } catch (org.springframework.web.client.HttpClientErrorException ex) {
            int statusCode = ex.getStatusCode().value();
            if (statusCode == 400) {
                log.error("Gemini 400 Bad Request | convId={} | body={}", conversationId, ex.getResponseBodyAsString());
                return serviceUnavailableFallback();
            } else if (statusCode == 401 || statusCode == 403) {
                log.error("Gemini API key invalid or forbidden | convId={} | status={}", conversationId, statusCode);
                return invalidKeyFallback();
            } else if (statusCode == 429) {
                log.warn("Gemini rate limit hit | convId={}", conversationId);
                return rateLimitFallback();
            }
            log.error("Gemini HTTP error | convId={} | status={} | error={}", conversationId, statusCode, ex.getMessage());
            return serviceUnavailableFallback();
        } catch (Exception ex) {
            log.error("Gemini call failed | convId={} | error={}", conversationId, ex.getMessage(), ex);
            return serviceUnavailableFallback();
        }
    }

    /**
     * Smart keyword-based fallback for when Gemini is unavailable.
     * Covers all major GymPro features with detailed predefined answers.
     */
    private String buildSmartFallback(List<Map<String, Object>> history, String role) {
        String lastMsg = getLastUserMessage(history).toLowerCase();

        // ── Plans ──────────────────────────────────────────────────────────
        if (containsAny(lastMsg, "plan", "subscri", "membership", "pricing", "cost", "fee")) {
            return "📋 **GymPro Membership Plans**\n\n" +
                    "GymPro offers tiered plans managed by your gym admin. Each plan includes:\n" +
                    "• A set number of **free training sessions per month**\n" +
                    "• Additional sessions are charged per booking\n\n" +
                    "**How to subscribe:**\n" +
                    "1. Go to your **Dashboard → Plans**\n" +
                    "2. Browse available plans and their details\n" +
                    "3. Click **Subscribe** on your chosen plan\n" +
                    "4. Complete payment via Razorpay\n\n" +
                    "Your active plan appears in the Plans section. " +
                    "You must have an active plan to book trainer sessions.";
        }

        // ── Booking ────────────────────────────────────────────────────────
        if (containsAny(lastMsg, "book", "session", "appoint", "slot", "schedule a")) {
            return "📅 **Booking a Training Session**\n\n" +
                    "**Steps to book:**\n" +
                    "1. Go to **Dashboard → Bookings**\n" +
                    "2. Browse available trainers and their specializations\n" +
                    "3. Select a trainer and view their open time slots\n" +
                    "4. Pick your preferred slot and click **Book**\n" +
                    "5. If you've used your plan's free sessions, a payment screen appears\n" +
                    "6. Confirm your booking — you'll see it in **My Bookings**\n\n" +
                    "💡 **Tip:** You need an active membership plan before you can book sessions.";
        }

        // ── Payment ────────────────────────────────────────────────────────
        if (containsAny(lastMsg, "pay", "payment", "razorpay", "upi", "invoice", "receipt", "transaction")) {
            return "💳 **Payments in GymPro**\n\n" +
                    "GymPro uses **Razorpay** for secure payments. Supported methods:\n" +
                    "• Credit & Debit Cards\n" +
                    "• UPI (Google Pay, PhonePe, etc.)\n" +
                    "• Net Banking\n" +
                    "• Digital Wallets\n\n" +
                    "**To make a payment:**\n" +
                    "1. Go to **Dashboard → Payments**\n" +
                    "2. View pending or past payments\n" +
                    "3. Click **Pay Now** for any pending amount\n" +
                    "4. Complete via the Razorpay popup\n\n" +
                    "All payment history and receipts are stored in the Payments section.";
        }

        // ── Trainers ───────────────────────────────────────────────────────
        if (containsAny(lastMsg, "trainer", "coach", "instructor", "trainer available", "find trainer")) {
            return "🏋️ **Finding & Booking Trainers**\n\n" +
                    "**As a Member:**\n" +
                    "1. Go to **Dashboard → Bookings**\n" +
                    "2. You'll see a list of available trainers with specializations\n" +
                    "3. Click on a trainer to see their schedule and open slots\n" +
                    "4. Select a slot to book a session\n\n" +
                    "**Trainer profiles show:**\n" +
                    "• Specialization (e.g., Strength, Cardio, Yoga)\n" +
                    "• Available time slots set by the trainer\n" +
                    "• Session fee (if applicable beyond your plan quota)\n\n" +
                    "Trainers manage their own availability in the **Trainer Dashboard → My Schedule**.";
        }

        // ── Notifications ──────────────────────────────────────────────────
        if (containsAny(lastMsg, "notif", "alert", "bell", "reminder")) {
            return "🔔 **Notifications**\n\n" +
                    "GymPro sends real-time notifications for:\n" +
                    "• ✅ Booking confirmations\n" +
                    "• 💳 Payment receipts\n" +
                    "• ⏰ Session reminders\n" +
                    "• 📋 Plan expiry alerts\n\n" +
                    "Access notifications via the **bell icon** 🔔 at the top of your dashboard. " +
                    "Unread notifications show a badge count. Click any notification to see details.";
        }

        // ── Admin ──────────────────────────────────────────────────────────
        if (containsAny(lastMsg, "admin", "manage", "dashboard", "overview", "report", "analytics")) {
            return "⚙️ **Admin Panel Overview**\n\n" +
                    "The Admin Dashboard provides full platform control:\n\n" +
                    "• **Overview** — Active members, bookings, revenue stats\n" +
                    "• **Manage Members** — View, activate/deactivate member accounts\n" +
                    "• **Manage Trainers** — Add, edit, or remove trainers\n" +
                    "• **Manage Plans** — Create, edit, price, and delete gym plans\n" +
                    "• **Bookings** — View all bookings across the platform\n" +
                    "• **Payments** — Full payment ledger with Razorpay transaction IDs\n\n" +
                    "Access via **Admin Dashboard** after logging in with an Admin account.";
        }

        // ── Profile ────────────────────────────────────────────────────────
        if (containsAny(lastMsg, "profile", "photo", "name", "email", "update account", "edit account")) {
            return "👤 **Updating Your Profile**\n\n" +
                    "1. Click your profile icon or go to **Dashboard → Profile**\n" +
                    "2. You can update:\n" +
                    "   • Display name\n" +
                    "   • Profile photo\n" +
                    "   • Contact information\n" +
                    "   • (Trainers) Specializations and bio\n" +
                    "3. Click **Save** to apply changes\n\n" +
                    "Email and role changes require admin assistance.";
        }

        // ── Login / Auth ────────────────────────────────────────────────────
        if (containsAny(lastMsg, "login", "logout", "password", "forgot", "reset", "register", "sign up", "sign in")) {
            return "🔐 **Account & Authentication**\n\n" +
                    "• **Login** — Use your registered email and password at the Login page\n" +
                    "• **Forgot Password** — Click *Forgot Password* on the login screen, " +
                    "enter your email, verify the OTP, then reset your password\n" +
                    "• **Register** — New users can register with email, name, and password\n" +
                    "• **Logout** — Click the logout option in the top navigation bar\n\n" +
                    "Sessions are secured with JWT tokens. If your session expires, you'll be redirected to login automatically.";
        }

        // ── Fitness / Workout ───────────────────────────────────────────────
        if (containsAny(lastMsg, "workout", "exercise", "fitness", "gym tip", "nutrition", "diet", "muscle", "cardio")) {
            return "💪 **General Fitness Tips**\n\n" +
                    "• **Consistency** — Aim for at least 3 sessions per week\n" +
                    "• **Warm up** — Always start with 5–10 minutes of light cardio\n" +
                    "• **Progressive overload** — Gradually increase weights or reps over time\n" +
                    "• **Rest days** — Muscles grow during recovery; aim for 1–2 rest days/week\n" +
                    "• **Nutrition** — Adequate protein (1.6–2g/kg body weight) supports muscle building\n" +
                    "• **Hydration** — Drink at least 2–3 litres of water daily\n\n" +
                    "💡 Your GymPro trainer can build a **personalised workout plan** tailored to your goals. Book a session today!";
        }

        // ── Help / Hello ────────────────────────────────────────────────────
        if (containsAny(lastMsg, "help", "hello", "hi", "hey", "what can", "how do", "guide", "start")) {
            return "👋 Hi! I'm **GymBot**, your GymPro assistant.\n\n" +
                    "Here's what I can help you with:\n\n" +
                    "• 📋 **Plans** — Browse and subscribe to membership plans\n" +
                    "• 📅 **Bookings** — Schedule training sessions with trainers\n" +
                    "• 💳 **Payments** — Pay for plans & view payment history\n" +
                    "• 🏋️ **Trainers** — Find the right trainer for your goals\n" +
                    "• 🔔 **Notifications** — Stay updated with booking alerts\n" +
                    "• ⚙️ **Admin** — Manage members, trainers, and plans\n" +
                    "• 💪 **Fitness FAQs** — Workout and nutrition advice\n\n" +
                    "What would you like to know?";
        }

        // ── Generic fallback ────────────────────────────────────────────────
        return "🤔 I'm not sure I understand your question fully. I'm trained to help with:\n\n" +
                "• **Plans & Subscriptions**\n" +
                "• **Booking Sessions with Trainers**\n" +
                "• **Payments via Razorpay**\n" +
                "• **Trainer Availability & Schedules**\n" +
                "• **Notifications & Alerts**\n" +
                "• **Account & Profile Management**\n" +
                "• **Fitness & Workout Tips**\n\n" +
                "Could you rephrase your question or pick one of the topics above?";
    }

    private String serviceUnavailableFallback() {
        return "⚠️ I'm having trouble connecting to the AI service right now. " +
                "Please try again in a moment.\n\n" +
                "In the meantime, you can navigate directly to:\n" +
                "• **Plans** — to subscribe to a membership\n" +
                "• **Bookings** — to schedule a session\n" +
                "• **Payments** — to manage your payments";
    }

    private String invalidKeyFallback() {
        return "⚠️ The AI service is not configured correctly (invalid API key). " +
                "Please contact your administrator.\n\n" +
                "You can still use GymPro — navigate to:\n" +
                "• **Plans**, **Bookings**, and **Payments** for core features.";
    }

    private String rateLimitFallback() {
        return "⚠️ The AI assistant is temporarily busy (rate limit reached). " +
                "Please wait a moment and try again.";
    }

    /**
     * Trims history: removes the oldest turns while keeping even count
     * (Gemini requires alternating user/model turns, so we always remove in pairs).
     */
    private void trimHistory(List<Map<String, Object>> history) {
        // maxHistory is the cap for user+model turns combined
        if (history.size() > maxHistory) {
            // Remove oldest 2 turns (1 user + 1 model) to maintain alternating order
            int excess = history.size() - maxHistory;
            // Round up to even so we don't break user/model pairing
            if (excess % 2 != 0) excess++;
            if (excess > 0 && excess <= history.size()) {
                history.subList(0, excess).clear();
                log.debug("Trimmed {} old turns from history (max={})", excess, maxHistory);
            }
        }
    }

    private boolean isValidApiKey(String key) {
        return key != null && !key.isBlank()
                && !key.equals("YOUR_GEMINI_API_KEY_HERE")
                && !key.startsWith("sk-placeholder")
                && !key.equals("your-key-here")
                && !key.equals("GEMINI_API_KEY");
    }

    /**
     * Gets the last user message text from Gemini-format history
     * (each entry has "role" and "parts" → List of {"text": "..."}).
     */
    @SuppressWarnings("unchecked")
    private String getLastUserMessage(List<Map<String, Object>> history) {
        for (int i = history.size() - 1; i >= 0; i--) {
            Map<String, Object> turn = history.get(i);
            if ("user".equals(turn.get("role"))) {
                List<Map<String, Object>> parts = (List<Map<String, Object>>) turn.get("parts");
                if (parts != null && !parts.isEmpty()) {
                    Object text = parts.get(0).get("text");
                    return text != null ? text.toString() : "";
                }
            }
        }
        return "";
    }

    private boolean containsAny(String text, String... keywords) {
        for (String kw : keywords) {
            if (text.contains(kw)) return true;
        }
        return false;
    }

    /**
     * Builds a single Gemini conversation turn.
     * Format: { "role": "user"|"model", "parts": [{ "text": "..." }] }
     */
    private Map<String, Object> geminiTurn(String role, String text) {
        Map<String, Object> turn = new LinkedHashMap<>();
        turn.put("role", role);
        turn.put("parts", List.of(Map.of("text", text)));
        return turn;
    }

    private String truncate(String s, int maxLen) {
        if (s == null) return "null";
        return s.length() <= maxLen ? s : s.substring(0, maxLen) + "…";
    }
}
