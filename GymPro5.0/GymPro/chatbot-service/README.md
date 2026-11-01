# chatbot-service – Setup Guide

## 1. Import into Eclipse

File → Import → Maven → Existing Maven Projects → select the `chatbot-service` folder.

---

## 2. Project structure

```
chatbot-service/
├── pom.xml
└── src/main/
    ├── java/com/gympro/chatbot/
    │   ├── ChatbotServiceApplication.java   ← @SpringBootApplication + @EnableDiscoveryClient
    │   ├── aop/
    │   │   └── LoggingAspect.java           ← AOP logging for controller + service layers
    │   ├── config/
    │   │   ├── OpenAIConfig.java            ← RestTemplate bean with Bearer token interceptor
    │   │   └── SecurityConfig.java          ← permit all /chatbot/** without auth
    │   ├── controller/
    │   │   └── ChatbotController.java       ← POST /chatbot/chat, GET /chatbot/health
    │   ├── dto/
    │   │   ├── ChatRequest.java             ← { message, role, conversationId }
    │   │   └── ChatResponse.java            ← { reply, conversationId, timestamp }
    │   └── service/
    │       └── ChatbotService.java          ← OpenAI call, history management, fallback
    └── resources/
        └── application.properties
```

---

## 3. Gateway – two things to update

### 3a. Replace `gateway-service/src/main/resources/application.properties`
Copy the contents of `gateway-application.properties` (included in this zip)
into the gateway's `application.properties`. It adds:

```properties
spring.cloud.gateway.routes[7].id=chatbot-service
spring.cloud.gateway.routes[7].uri=lb://chatbot-service
spring.cloud.gateway.routes[7].predicates[0]=Path=/chatbot/**
```

### 3b. Add `/chatbot` to the public URLs list in `JwtAuthFilter.java`

Open `gateway-service/.../filter/JwtAuthFilter.java` and add `"/chatbot"` to
the `PUBLIC_URLS` list:

```java
// BEFORE
private static final List<String> PUBLIC_URLS = List.of(
    "/auth/register", "/auth/login", "/auth/test",
    "/auth/forgot-password", "/auth/verify-otp", "/auth/reset-password"
);

// AFTER
private static final List<String> PUBLIC_URLS = List.of(
    "/auth/register", "/auth/login", "/auth/test",
    "/auth/forgot-password", "/auth/verify-otp", "/auth/reset-password",
    "/chatbot"   // ← ADD THIS LINE
);
```

Without this change the gateway will reject chatbot requests with 401
before they ever reach chatbot-service.

---

## 4. Environment variable for OpenAI

Set `OPENAI_API_KEY` before starting the service:

**Windows (PowerShell):**
```powershell
$env:OPENAI_API_KEY="sk-your-real-key-here"
mvn spring-boot:run
```

**Linux / macOS:**
```bash
export OPENAI_API_KEY=sk-your-real-key-here
mvn spring-boot:run
```

**Eclipse Run Configuration:**
Run → Run Configurations → Environment tab → New → `OPENAI_API_KEY` = your key

If the key is missing or set to the placeholder (`sk-placeholder`), the service
starts normally and returns helpful static answers about GymPro features.
No crash, no 500 errors.

---

## 5. Startup order

```
1. eureka-service   (port 8761)
2. auth-service     (port 8085)
3. chatbot-service  (port 8088)   ← new; order relative to others doesn't matter
4. gateway-service  (port 8080)
5. other microservices ...
```

---

## 6. Test the chatbot

**Health check:**
```
GET http://localhost:8080/chatbot/health
```

**Chat (via gateway):**
```
POST http://localhost:8080/chatbot/chat
Content-Type: application/json

{
  "message": "How do I book a session?",
  "role": "MEMBER",
  "conversationId": null
}
```

Response:
```json
{
  "reply": "📅 Head to Bookings in your dashboard ...",
  "conversationId": "550e8400-e29b-41d4-a716-446655440000",
  "timestamp": "2026-05-18T10:30:00"
}
```

Store the returned `conversationId` and pass it back with every subsequent
message to maintain the conversation history.

**Clear a conversation:**
```
DELETE http://localhost:8080/chatbot/conversation/{conversationId}
```

---

## 7. Frontend integration (React)

```js
// services/chatbotService.js
import axios from 'axios';

const BASE = 'http://localhost:8080/chatbot';

export const sendMessage = (message, role, conversationId) =>
  axios.post(`${BASE}/chat`, { message, role, conversationId });
```

Generate a UUID on first message (`crypto.randomUUID()`), store it in
`useState`, and pass it with every subsequent call.
