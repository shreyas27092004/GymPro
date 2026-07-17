-- ============================================================
-- GymPro MySQL Setup Script
-- Run this once before starting the services
-- ============================================================

-- Create all databases
CREATE DATABASE IF NOT EXISTS gympro_auth;
CREATE DATABASE IF NOT EXISTS gympro_member;
CREATE DATABASE IF NOT EXISTS gympro_trainer;
CREATE DATABASE IF NOT EXISTS gympro_plan;
CREATE DATABASE IF NOT EXISTS gympro_booking;

-- ──────────────────────────────────────────────────────────
-- gympro_auth  (auth-service)
-- ──────────────────────────────────────────────────────────
USE gympro_auth;

CREATE TABLE IF NOT EXISTS users (
    id       BIGINT AUTO_INCREMENT PRIMARY KEY,
    name     VARCHAR(100),
    email    VARCHAR(100) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    role     VARCHAR(20)  NOT NULL   -- MEMBER | TRAINER | ADMIN
);

-- Sample admin user (password = 'admin123' hashed with BCrypt)
INSERT IGNORE INTO users (name, email, password, role) VALUES
('Admin User',   'admin@gympro.com',   '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'ADMIN'),
('Test Trainer', 'trainer@gympro.com', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'TRAINER'),
('Test Member',  'member@gympro.com',  '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'MEMBER');
-- All sample passwords = admin123

-- ──────────────────────────────────────────────────────────
-- gympro_member  (member-service)
-- ──────────────────────────────────────────────────────────
USE gympro_member;

CREATE TABLE IF NOT EXISTS members (
    id      BIGINT AUTO_INCREMENT PRIMARY KEY,
    name    VARCHAR(100),
    email   VARCHAR(100) NOT NULL UNIQUE,
    phone   VARCHAR(15),
    address VARCHAR(255),
    gender  VARCHAR(10),
    status  VARCHAR(20)   -- ACTIVE | INACTIVE
);

INSERT IGNORE INTO members (name, email, phone, gender, status) VALUES
('Test Member', 'member@gympro.com', '9876543210', 'MALE', 'ACTIVE');

-- ──────────────────────────────────────────────────────────
-- gympro_trainer  (trainer-service)
-- ──────────────────────────────────────────────────────────
USE gympro_trainer;

CREATE TABLE IF NOT EXISTS trainers (
    id                BIGINT AUTO_INCREMENT PRIMARY KEY,
    name              VARCHAR(100),
    email             VARCHAR(100) NOT NULL UNIQUE,
    phone             VARCHAR(15),
    specialization    VARCHAR(100),
    experience_years  INT,
    status            VARCHAR(20)  -- ACTIVE | INACTIVE
);

CREATE TABLE IF NOT EXISTS trainer_schedules (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    trainer_id  BIGINT NOT NULL,
    day_of_week VARCHAR(10),    -- MON, TUE, WED...
    start_time  VARCHAR(10),    -- 09:00
    end_time    VARCHAR(10),    -- 11:00
    available   TINYINT(1) DEFAULT 1
);

INSERT IGNORE INTO trainers (name, email, phone, specialization, experience_years, status) VALUES
('Test Trainer', 'trainer@gympro.com', '9123456789', 'Weight Loss', 5, 'ACTIVE');

INSERT IGNORE INTO trainer_schedules (trainer_id, day_of_week, start_time, end_time, available) VALUES
(1, 'MON', '09:00', '11:00', 1),
(1, 'WED', '09:00', '11:00', 1),
(1, 'FRI', '14:00', '16:00', 1);

-- ──────────────────────────────────────────────────────────
-- gympro_plan  (plan-service)
-- ──────────────────────────────────────────────────────────
USE gympro_plan;

CREATE TABLE IF NOT EXISTS membership_plans (
    id            BIGINT AUTO_INCREMENT PRIMARY KEY,
    plan_name     VARCHAR(100),
    description   VARCHAR(255),
    duration_type VARCHAR(20),    -- MONTHLY | QUARTERLY | YEARLY
    price         DOUBLE,
    duration_days INT,
    active        TINYINT(1) DEFAULT 1
);

CREATE TABLE IF NOT EXISTS member_subscriptions (
    id           BIGINT AUTO_INCREMENT PRIMARY KEY,
    member_id    BIGINT,
    member_email VARCHAR(100),
    plan_id      BIGINT,
    plan_name    VARCHAR(100),
    start_date   DATE,
    end_date     DATE,
    status       VARCHAR(20)    -- ACTIVE | EXPIRED | CANCELLED
);

INSERT IGNORE INTO membership_plans (plan_name, description, duration_type, price, duration_days, active) VALUES
('Silver Monthly',   'Basic gym access for 1 month',    'MONTHLY',    999,  30,  1),
('Gold Quarterly',   'Full access for 3 months',        'QUARTERLY',  2499, 90,  1),
('Platinum Yearly',  'Premium access for full year',    'YEARLY',     7999, 365, 1);

-- ──────────────────────────────────────────────────────────
-- gympro_booking  (booking-service)
-- ──────────────────────────────────────────────────────────
USE gympro_booking;

CREATE TABLE IF NOT EXISTS bookings (
    id            BIGINT AUTO_INCREMENT PRIMARY KEY,
    member_id     BIGINT,
    member_email  VARCHAR(100),
    trainer_id    BIGINT,
    trainer_email VARCHAR(100),
    schedule_id   BIGINT,
    session_day   VARCHAR(10),
    session_time  VARCHAR(30),
    booking_date  DATE,          -- the actual calendar date of the session (member-chosen, cannot be in the past)
    created_at    DATETIME,      -- when the booking record was created (audit trail)
    status        VARCHAR(20),   -- CONFIRMED | CANCELLED | COMPLETED
    notes         VARCHAR(255)
);

-- ============================================================
-- Setup complete! All databases and tables created.
-- ============================================================
