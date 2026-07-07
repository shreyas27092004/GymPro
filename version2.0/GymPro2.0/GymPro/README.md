# GymPro – Microservice Backend
## Candidate: Shreyas V

---

## Services & Ports

| Service               | Port | Purpose                                      |
|-----------------------|------|----------------------------------------------|
| eureka-server         | 8761 | Service registry (all services register here)|
| gateway-service       | 8080 | Single entry point. JWT validation here.     |
| auth-service          | 8081 | Register / Login / JWT issue                 |
| member-service        | 8082 | Member CRUD (profile management)             |
| trainer-service       | 8083 | Trainer CRUD + Schedule management           |
| plan-service          | 8084 | Membership plans (Monthly/Quarterly/Yearly)  |
| booking-service       | 8085 | Book trainer sessions + notify via email     |
| notification-service  | 8086 | Send emails (Gmail SMTP)                     |

---

## 3 Roles & What They Can Do

| Role    | Can Do                                                          |
|---------|-----------------------------------------------------------------|
| ADMIN   | Manage members, create trainers, create plans, view everything  |
| TRAINER | Manage own schedule, view own bookings, mark sessions complete  |
| MEMBER  | Subscribe to plans, book trainer sessions, view own data        |

---

## Startup Order (MUST follow this order)

```
1. Start MySQL
2. Run mysql-setup.sql  (creates all databases + sample data)
3. cd eureka-server  →  mvn spring-boot:run
4. cd gateway-service → mvn spring-boot:run
5. cd auth-service   → mvn spring-boot:run
6. cd member-service → mvn spring-boot:run
7. cd trainer-service → mvn spring-boot:run
8. cd plan-service   → mvn spring-boot:run
9. cd booking-service → mvn spring-boot:run
10. cd notification-service → mvn spring-boot:run
```

---

## How JWT Works (explain in review)

1. Client calls  POST /auth/login  with email + password
2. auth-service verifies credentials, returns a JWT token
3. JWT contains: email, role, expiry time
4. Client sends token in every request:  Authorization: Bearer <token>
5. gateway-service intercepts, validates the token
6. If valid → forwards request + adds X-User-Email and X-User-Role headers
7. Business service reads X-User-Role to decide if access is allowed

---

## Postman Testing Guide

### Step 1 – Register (no token needed)
POST http://localhost:8080/auth/register
Body (JSON):
{
  "name": "John",
  "email": "john@gym.com",
  "password": "pass123",
  "role": "MEMBER"
}

### Step 2 – Login (get token)
POST http://localhost:8080/auth/login
Body:  { "email": "john@gym.com", "password": "pass123" }
→ Copy the "token" from response

### Step 3 – Use token in all other requests
Header:  Authorization: Bearer <your_token>

### Step 4 – Create member profile
POST http://localhost:8080/members
{ "name": "John", "email": "john@gym.com", "phone": "9876543210" }

### Step 5 – View plans
GET http://localhost:8080/plans

### Step 6 – Subscribe to a plan
POST http://localhost:8080/plans/subscribe?memberId=1&memberEmail=john@gym.com&planId=1

### Step 7 – View trainers
GET http://localhost:8080/trainers

### Step 8 – View trainer schedule / available slots
GET http://localhost:8080/trainers/1/available-slots

### Step 9 – Book a session
POST http://localhost:8080/bookings/create
{
  "memberId": 1,
  "memberEmail": "john@gym.com",
  "trainerId": 1,
  "trainerEmail": "trainer@gympro.com",
  "scheduleId": 1,
  "sessionDay": "MON",
  "sessionTime": "09:00 - 11:00",
  "notes": "First session"
}
→ This automatically sends an email to member + trainer

---

## Key Design Decisions (for review questions)

Q: Why microservices?
A: Each service is independent – we can scale booking-service separately if needed.
   Each has its own database (DB-per-service pattern).

Q: Why Eureka?
A: Services don't hardcode each other's URLs.
   They register with Eureka and discover each other dynamically.

Q: Why API Gateway?
A: Single entry point. JWT is validated HERE, not in every service.
   Routing rules are in application.properties.

Q: How does JWT auth work?
A: Login → get token → send token in every request → gateway validates → passes role via header.

Q: How does Feign work?
A: booking-service has a NotificationClient interface.
   Spring Cloud creates the HTTP client automatically.
   When booking is created, it calls notification-service/notify/send.

Q: What are the 3 roles?
A: MEMBER – uses the gym, books sessions, subscribes to plans.
   TRAINER – manages own schedule, handles booked sessions.
   ADMIN   – manages everything (create trainers, plans, view all data).
