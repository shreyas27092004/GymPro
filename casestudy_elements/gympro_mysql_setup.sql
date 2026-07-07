-- ================================================================
--  GymPro – Complete MySQL Setup  (Updated with payment-service)
--
--  Databases:
--    gympro_auth     → auth-service     (port 8081)
--    gympro_member   → member-service   (port 8082)
--    gympro_trainer  → trainer-service  (port 8083)
--    gympro_plan     → plan-service     (port 8084)
--    gympro_booking  → booking-service  (port 8085)
--    gympro_payment  → payment-service  (port 8087)  ← NEW
--
--  notification-service (port 8086) has NO database.
--
--  HOW TO RUN:
--    mysql -u root -p < gympro_mysql_setup.sql
-- ================================================================

DROP DATABASE IF EXISTS gympro_auth;
DROP DATABASE IF EXISTS gympro_member;
DROP DATABASE IF EXISTS gympro_trainer;
DROP DATABASE IF EXISTS gympro_plan;
DROP DATABASE IF EXISTS gympro_booking;
DROP DATABASE IF EXISTS gympro_payment;

CREATE DATABASE gympro_auth    CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE gympro_member  CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE gympro_trainer CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE gympro_plan    CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE gympro_booking CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE gympro_payment CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

-- ================================================================
--  1. gympro_auth  →  auth-service (port 8081)
--     Entity: com.gympro.auth.entity.User
-- ================================================================
USE gympro_auth;

CREATE TABLE users (
    id        BIGINT        NOT NULL AUTO_INCREMENT,
    name      VARCHAR(255),
    email     VARCHAR(255)  NOT NULL UNIQUE,   -- @Column(unique=true, nullable=false)
    password  VARCHAR(255)  NOT NULL,           -- BCrypt hash
    role      VARCHAR(20)   NOT NULL DEFAULT 'MEMBER',  -- MEMBER | TRAINER | ADMIN
    PRIMARY KEY (id)
);

CREATE INDEX idx_users_email ON users(email);
CREATE INDEX idx_users_role  ON users(role);

-- ================================================================
--  2. gympro_member  →  member-service (port 8082)
--     Entity: com.gympro.member.entity.Member
-- ================================================================
USE gympro_member;

CREATE TABLE members (
    id      BIGINT        NOT NULL AUTO_INCREMENT,
    name    VARCHAR(255),
    email   VARCHAR(255)  NOT NULL UNIQUE,   -- links to auth user
    phone   VARCHAR(255),
    address VARCHAR(255),
    gender  VARCHAR(20),
    status  VARCHAR(20)   DEFAULT 'ACTIVE',  -- ACTIVE | INACTIVE
    PRIMARY KEY (id)
);

CREATE INDEX idx_members_email  ON members(email);
CREATE INDEX idx_members_status ON members(status);

-- ================================================================
--  3. gympro_trainer  →  trainer-service (port 8083)
--     Entities: Trainer, TrainerSchedule
-- ================================================================
USE gympro_trainer;

CREATE TABLE trainers (
    id                BIGINT        NOT NULL AUTO_INCREMENT,
    name              VARCHAR(255),
    email             VARCHAR(255)  NOT NULL UNIQUE,
    phone             VARCHAR(255),
    specialization    VARCHAR(255),
    experience_years  INT           DEFAULT 0,
    status            VARCHAR(20)   DEFAULT 'ACTIVE',  -- ACTIVE | INACTIVE
    PRIMARY KEY (id)
);

CREATE INDEX idx_trainers_email  ON trainers(email);
CREATE INDEX idx_trainers_status ON trainers(status);

CREATE TABLE trainer_schedules (
    id           BIGINT        NOT NULL AUTO_INCREMENT,
    trainer_id   BIGINT        NOT NULL,
    day_of_week  VARCHAR(10)   NOT NULL,   -- MON | TUE | WED | THU | FRI | SAT | SUN
    start_time   VARCHAR(20)   NOT NULL,   -- e.g. "09:00"
    end_time     VARCHAR(20)   NOT NULL,   -- e.g. "11:00"
    available    TINYINT(1)    NOT NULL DEFAULT 1,  -- 1=open, 0=booked
    PRIMARY KEY (id)
);

CREATE INDEX idx_schedules_trainer   ON trainer_schedules(trainer_id);
CREATE INDEX idx_schedules_day       ON trainer_schedules(day_of_week);
CREATE INDEX idx_schedules_available ON trainer_schedules(available);

-- ================================================================
--  4. gympro_plan  →  plan-service (port 8084)
--     Entities: MembershipPlan, MemberSubscription
-- ================================================================
USE gympro_plan;

CREATE TABLE membership_plans (
    id             BIGINT          NOT NULL AUTO_INCREMENT,
    plan_name      VARCHAR(255),
    description    TEXT,
    duration_type  VARCHAR(20),    -- MONTHLY | QUARTERLY | YEARLY
    price          DOUBLE          NOT NULL DEFAULT 0.0,
    duration_days  INT             NOT NULL DEFAULT 30,
    active         TINYINT(1)      NOT NULL DEFAULT 1,
    PRIMARY KEY (id)
);

CREATE TABLE member_subscriptions (
    id            BIGINT        NOT NULL AUTO_INCREMENT,
    member_id     BIGINT        NOT NULL,
    member_email  VARCHAR(255),
    plan_id       BIGINT        NOT NULL,
    plan_name     VARCHAR(255),
    start_date    DATE,
    end_date      DATE,
    status        VARCHAR(20)   DEFAULT 'ACTIVE',  -- ACTIVE | EXPIRED | CANCELLED
    PRIMARY KEY (id)
);

CREATE INDEX idx_plans_active       ON membership_plans(active);
CREATE INDEX idx_subs_member_id     ON member_subscriptions(member_id);
CREATE INDEX idx_subs_status        ON member_subscriptions(status);

-- ================================================================
--  5. gympro_booking  →  booking-service (port 8085)
--     Entity: com.gympro.booking.entity.Booking
-- ================================================================
USE gympro_booking;

CREATE TABLE bookings (
    id             BIGINT        NOT NULL AUTO_INCREMENT,
    member_id      BIGINT,
    member_email   VARCHAR(255),
    trainer_id     BIGINT,
    trainer_email  VARCHAR(255),
    schedule_id    BIGINT,
    session_day    VARCHAR(10),   -- MON | TUE | ...
    session_time   VARCHAR(50),   -- e.g. "09:00 - 11:00"
    booking_date   DATE,
    status         VARCHAR(20)   DEFAULT 'CONFIRMED',  -- CONFIRMED | CANCELLED | COMPLETED
    notes          TEXT,
    PRIMARY KEY (id)
);

CREATE INDEX idx_bookings_member_id  ON bookings(member_id);
CREATE INDEX idx_bookings_trainer_id ON bookings(trainer_id);
CREATE INDEX idx_bookings_status     ON bookings(status);

-- ================================================================
--  6. gympro_payment  →  payment-service (port 8087)  ← NEW
--     Entity: com.gympro.payment.entity.Payment
-- ================================================================
USE gympro_payment;

CREATE TABLE payments (
    id              BIGINT          NOT NULL AUTO_INCREMENT,

    -- Who is paying
    member_id       BIGINT,
    member_email    VARCHAR(255),

    -- What they are paying for (one will be set, the other null)
    booking_id      BIGINT,           -- for booking session payments
    subscription_id BIGINT,           -- for plan subscription payments

    -- Payment details
    amount          DOUBLE          NOT NULL,
    payment_method  VARCHAR(30),      -- CREDIT_CARD | DEBIT_CARD | UPI | QR_CODE | CASH
    status          VARCHAR(20)     DEFAULT 'PENDING',  -- PENDING | SUCCESS | FAILED | REFUNDED
    transaction_id  VARCHAR(100)    UNIQUE,             -- e.g. "TXN-A1B2C3D4"
    description     TEXT,
    paid_at         DATETIME,

    PRIMARY KEY (id)
);

CREATE INDEX idx_payments_member_id      ON payments(member_id);
CREATE INDEX idx_payments_booking_id     ON payments(booking_id);
CREATE INDEX idx_payments_subscription_id ON payments(subscription_id);
CREATE INDEX idx_payments_status         ON payments(status);
CREATE INDEX idx_payments_transaction_id ON payments(transaction_id);

-- ================================================================
--  SEED DATA
--  3 users (password = "admin123" BCrypt hashed)
--  2 trainers + 6 schedules
--  4 membership plans
-- ================================================================

USE gympro_auth;
INSERT INTO users (name, email, password, role) VALUES
  ('Super Admin',   'admin@gympro.com',   '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lh7y', 'ADMIN'),
  ('Ravi Trainer',  'ravi@gympro.com',    '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lh7y', 'TRAINER'),
  ('Shreyas Member','shreyas@gympro.com', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lh7y', 'MEMBER');

USE gympro_member;
INSERT INTO members (name, email, phone, address, gender, status) VALUES
  ('Shreyas Member', 'shreyas@gympro.com', '9876543210', 'Mysuru, Karnataka', 'Male', 'ACTIVE');

USE gympro_trainer;
INSERT INTO trainers (name, email, phone, specialization, experience_years, status) VALUES
  ('Ravi Trainer',  'ravi@gympro.com',  '9000000001', 'Weight Loss',   5, 'ACTIVE'),
  ('Priya Trainer', 'priya@gympro.com', '9000000002', 'Yoga & Cardio', 3, 'ACTIVE');

INSERT INTO trainer_schedules (trainer_id, day_of_week, start_time, end_time, available) VALUES
  (1, 'MON', '09:00', '11:00', 1),
  (1, 'WED', '09:00', '11:00', 1),
  (1, 'FRI', '14:00', '16:00', 1),
  (2, 'TUE', '07:00', '09:00', 1),
  (2, 'THU', '07:00', '09:00', 1),
  (2, 'SAT', '10:00', '12:00', 1);

USE gympro_plan;
INSERT INTO membership_plans (plan_name, description, duration_type, price, duration_days, active) VALUES
  ('Basic Monthly',        'Gym floor access',                            'MONTHLY',   999.00,  30,  1),
  ('Standard Quarterly',   '3 months + 2 trainer sessions/month',         'QUARTERLY', 2499.00, 90,  1),
  ('Premium Half-Yearly',  '6 months + 4 trainer sessions/month',         'QUARTERLY', 4499.00, 180, 1),
  ('Elite Annual',         'Full year + unlimited trainer access',         'YEARLY',    7999.00, 365, 1);

-- ================================================================
--  VERIFY
-- ================================================================
SELECT '=== DATABASES ===' AS '';
SHOW DATABASES LIKE 'gympro%';

SELECT '=== gympro_auth.users ===' AS '';
USE gympro_auth; SELECT id, name, email, role FROM users;

SELECT '=== gympro_member.members ===' AS '';
USE gympro_member; SELECT id, name, email, status FROM members;

SELECT '=== gympro_trainer.trainers ===' AS '';
USE gympro_trainer; SELECT id, name, specialization, status FROM trainers;

SELECT '=== gympro_trainer.trainer_schedules ===' AS '';
SELECT trainer_id, day_of_week, start_time, end_time, available FROM trainer_schedules;

SELECT '=== gympro_plan.membership_plans ===' AS '';
USE gympro_plan; SELECT id, plan_name, duration_type, price FROM membership_plans;

SELECT '=== gympro_payment.payments (empty at start) ===' AS '';
USE gympro_payment; SELECT COUNT(*) AS total_payments FROM payments;
