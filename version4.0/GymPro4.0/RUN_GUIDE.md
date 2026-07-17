# GymPro — Run Guide

This is the full project (backend microservices + React frontend), including the
**trainer approval workflow** (admin must approve a new trainer before they can log in).

## 1. Prerequisites to install

| Tool | Version | Check |
|---|---|---|
| Java JDK | 17+ | `java -version` |
| Maven | 3.8+ | `mvn -version` (or just use Eclipse's bundled Maven) |
| Node.js | 18+ | `node -v` |
| npm | 9+ | `npm -v` |
| MySQL | 8.x, running on `localhost:3306`, user `root` / pass `root` | — |
| RabbitMQ | running on `localhost:5672` (default guest/guest) | — |

No Docker needed — everything already points at `localhost` (Docker config was removed earlier).

MySQL databases are auto-created on first run (`createDatabaseIfNotExist=true`), so you
don't need to create them manually — just make sure the MySQL server itself is running
and the `root` user/password matches (edit each service's
`src/main/resources/application.properties` if yours differs).

RabbitMQ: if you don't have it, easiest is:
```bash
docker run -d --name rabbitmq -p 5672:5672 -p 15672:15672 rabbitmq:3-management
```
(this is just for RabbitMQ itself — the app services still run natively, not in Docker)

## 2. Install dependencies

**Backend** — each service is a normal Maven project. If using Eclipse: import all 10
folders under `GymPro/` as "Existing Maven Projects" and let Eclipse resolve dependencies.
From the command line instead:
```bash
cd GymPro/eureka-server        && mvn clean install -DskipTests
cd ../auth-service              && mvn clean install -DskipTests
cd ../trainer-service           && mvn clean install -DskipTests
cd ../member-service            && mvn clean install -DskipTests
cd ../plan-service              && mvn clean install -DskipTests
cd ../booking-service           && mvn clean install -DskipTests
cd ../payment-service           && mvn clean install -DskipTests
cd ../notification-service      && mvn clean install -DskipTests
cd ../chatbot-service            && mvn clean install -DskipTests
cd ../gateway-service            && mvn clean install -DskipTests
```

**Frontend:**
```bash
cd gympro-frontend
npm install
```

## 3. Start order (important — Eureka first)

1. **eureka-server** (port 8761) — start this first, wait for it to come up
   (visit http://localhost:8761 to confirm)
2. **gateway-service** (port 8080)
3. The rest, any order: **auth-service** (8081), **member-service** (8082),
   **trainer-service** (8083), **plan-service** (8084), **booking-service** (8085),
   **notification-service** (8086), **payment-service** (8087), **chatbot-service** (8088)

In Eclipse: right-click each `*Application.java` → Run As → Java Application (or Spring
Boot App). From the command line, in separate terminals:
```bash
cd GymPro/eureka-server   && mvn spring-boot:run
cd GymPro/gateway-service && mvn spring-boot:run
cd GymPro/auth-service    && mvn spring-boot:run
cd GymPro/trainer-service && mvn spring-boot:run
cd GymPro/member-service  && mvn spring-boot:run
cd GymPro/plan-service    && mvn spring-boot:run
cd GymPro/booking-service && mvn spring-boot:run
cd GymPro/notification-service && mvn spring-boot:run
cd GymPro/payment-service && mvn spring-boot:run
cd GymPro/chatbot-service && mvn spring-boot:run
```

4. **Frontend:**
```bash
cd gympro-frontend
npm run dev
```
Then open the URL Vite prints (usually http://localhost:5173).

## 4. Quick smoke test of the new trainer-approval feature

1. Register a user with role `TRAINER` on the frontend (or `POST /auth/register` via the
   gateway, `http://localhost:8080/auth/register`).
2. Try logging in as that trainer immediately → should be **blocked** with a
   "pending admin approval" message.
3. Register/login as an `ADMIN` (first admin registers instantly, no approval needed).
4. Go to Admin → Trainers → you should see a "⏳ Awaiting Approval" panel with that
   trainer → click **Approve**.
5. Log in as the trainer again → should now succeed.

## 5. Running tests

```bash
# any backend service, e.g.
cd GymPro/auth-service && mvn test

# frontend
cd gympro-frontend && npm test
```

## 6. What's new in this drop (trainer approval workflow)

- `trainer-service`: trainers can now have status `PENDING` / `REJECTED` in addition to
  `ACTIVE` / `INACTIVE`. New endpoints: `GET /trainers/pending`,
  `PATCH /trainers/{id}/approve`, `PATCH /trainers/{id}/reject` (admin-only).
- `auth-service`: self-registered trainers start as `PENDING`; login is blocked for any
  trainer whose status isn't `ACTIVE`.
- Frontend: Admin → Trainers shows a pending-approval queue; the member-facing trainer
  list only shows approved (`ACTIVE`) trainers.

No database migration needed — `status` was already a plain string column.
