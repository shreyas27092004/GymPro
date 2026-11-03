-- ============================================================
-- GymPro — DUMMY DATA
--
--   ADMIN   shreyasshreyu405@gmail.com      / Admin123
--   TRAINER andrewfake27092004@gmail.com    / Admin123
--   MEMBER  whatever17092003@gmail.com      / Admin123
--
-- HOW TO RUN
--   1. Start every microservice at least once so Hibernate has
--      created the tables (ddl-auto=update).
--   2. mysql -u root -p < gympro-dummy-data.sql
--
-- Safe to re-run: users / members / trainers are upserted (so the
-- password is reset to Admin123 even if the old seed already
-- inserted these emails with admin123), plans are only added if
-- missing, and schedule slots use INSERT IGNORE.
--
-- This script does NOT drop anything.
-- BCrypt hash below = "Admin123" (strength 10, $2a$).
-- ============================================================


-- ──────────────────────────────────────────────────────────
-- gympro_auth
-- ──────────────────────────────────────────────────────────
USE gympro_auth;

INSERT INTO users (name, email, password, role) VALUES
('Shreyas', 'shreyasshreyu405@gmail.com',   '$2a$10$gpwW7nrXl5L7yFQrqP8FTeR7VYMODfZrHvhAUvhmE8wsehEYChrkG', 'ADMIN'),
('Andrew',  'andrewfake27092004@gmail.com', '$2a$10$gpwW7nrXl5L7yFQrqP8FTeR7VYMODfZrHvhAUvhmE8wsehEYChrkG', 'TRAINER'),
('Vishwa',  'whatever17092003@gmail.com',   '$2a$10$gpwW7nrXl5L7yFQrqP8FTeR7VYMODfZrHvhAUvhmE8wsehEYChrkG', 'MEMBER')
ON DUPLICATE KEY UPDATE
  name     = VALUES(name),
  password = VALUES(password),
  role     = VALUES(role);


-- ──────────────────────────────────────────────────────────
-- gympro_member
-- ──────────────────────────────────────────────────────────
USE gympro_member;

ALTER TABLE members MODIFY phone VARCHAR(255) NULL;

INSERT INTO members (name, email, phone, address, gender, status) VALUES
('Vishwa', 'whatever17092003@gmail.com', '9876501234', 'Whitefield, Bengaluru, Karnataka', 'MALE', 'ACTIVE')
ON DUPLICATE KEY UPDATE
  name    = VALUES(name),
  phone   = VALUES(phone),
  address = VALUES(address),
  gender  = VALUES(gender),
  status  = VALUES(status);


-- ──────────────────────────────────────────────────────────
-- gympro_trainer
-- Status must be ACTIVE or the trainer can't log in
-- (new self-registered trainers start as PENDING).
-- ──────────────────────────────────────────────────────────
USE gympro_trainer;

ALTER TABLE trainers MODIFY phone VARCHAR(255) NULL;

INSERT INTO trainers (name, email, phone, specialization, experience_years, status, session_fee) VALUES
('Andrew', 'andrewfake27092004@gmail.com', '9123450001', 'Strength & Conditioning', 6, 'ACTIVE', 600.00)
ON DUPLICATE KEY UPDATE
  name             = VALUES(name),
  phone            = VALUES(phone),
  specialization   = VALUES(specialization),
  experience_years = VALUES(experience_years),
  status           = VALUES(status),
  session_fee      = VALUES(session_fee);

-- Next 7 days x 3 slots (two 1-on-1 slots + one group slot) for Andrew.
INSERT IGNORE INTO trainer_schedules
  (trainer_id, session_date, start_time, end_time, max_capacity, booked_count, cancelled, available)
SELECT t.id, DATE_ADD(CURDATE(), INTERVAL d.n DAY), s.st, s.et, s.cap, 0, 0, 1
FROM trainers t
JOIN (SELECT 1 AS n UNION ALL SELECT 2 UNION ALL SELECT 3 UNION ALL SELECT 4
      UNION ALL SELECT 5 UNION ALL SELECT 6 UNION ALL SELECT 7) d
JOIN (SELECT '06:00' AS st, '07:00' AS et, 1  AS cap
      UNION ALL SELECT '09:00', '10:00', 1
      UNION ALL SELECT '18:00', '19:00', 10) s
WHERE t.email = 'andrewfake27092004@gmail.com';


-- ──────────────────────────────────────────────────────────
-- gympro_plan  (only inserts plans that don't already exist)
-- ──────────────────────────────────────────────────────────
USE gympro_plan;

INSERT INTO membership_plans
  (plan_name, description, duration_type, price, duration_days, active,
   sessions_included, priority_level, trainer_discount_percent, dedicated_trainer, priority_booking)
SELECT * FROM (
  SELECT 'Silver Monthly'  AS plan_name, 'Basic gym access for 1 month'     AS description, 'MONTHLY'   AS duration_type,  999 AS price,  30 AS duration_days, 1 AS active, 0 AS sessions_included, 1 AS priority_level,  0 AS trainer_discount_percent, 0 AS dedicated_trainer, 0 AS priority_booking
  UNION ALL
  SELECT 'Gold Quarterly',  'Full access for 3 months',                      'QUARTERLY', 2499,  90, 1, 2, 2, 10, 0, 1
  UNION ALL
  SELECT 'Platinum Yearly', 'Premium access for the full year',              'YEARLY',    7999, 365, 1, 6, 3, 25, 1, 1
) p
WHERE NOT EXISTS (SELECT 1 FROM membership_plans m WHERE m.plan_name = p.plan_name);


-- ============================================================
-- OPTIONAL — give Vishwa an active Gold Quarterly subscription
-- plus a payment record, so the member / admin dashboards
-- aren't empty. Left commented out on purpose: a member with a
-- plan no longer gets the 1 free session, so keep it commented
-- if you want to demo the free-session flow.
-- Un-comment (remove the leading "-- ") to use it.
-- ============================================================

-- INSERT INTO gympro_plan.member_subscriptions
--   (member_id, member_email, plan_id, plan_name, start_date, end_date, status,
--    sessions_used, plan_price_at_purchase, amount_paid)
-- SELECT m.id, m.email, p.id, p.plan_name, CURDATE(), DATE_ADD(CURDATE(), INTERVAL 90 DAY), 'ACTIVE',
--        0, p.price, p.price
-- FROM gympro_member.members m
-- JOIN gympro_plan.membership_plans p ON p.plan_name = 'Gold Quarterly'
-- WHERE m.email = 'whatever17092003@gmail.com'
--   AND NOT EXISTS (SELECT 1 FROM gympro_plan.member_subscriptions s
--                   WHERE s.member_email = m.email AND s.status = 'ACTIVE');

-- INSERT INTO gympro_payment.payments
--   (member_id, member_email, subscription_id, amount, payment_method, status,
--    transaction_id, description, paid_at, payment_type)
-- SELECT s.member_id, s.member_email, s.id, s.amount_paid, 'RAZORPAY', 'SUCCESS',
--        'pay_DUMMY0001', 'Gold Quarterly subscription', NOW(), 'REGULAR'
-- FROM gympro_plan.member_subscriptions s
-- WHERE s.member_email = 'whatever17092003@gmail.com' AND s.status = 'ACTIVE'
--   AND NOT EXISTS (SELECT 1 FROM gympro_payment.payments x WHERE x.transaction_id = 'pay_DUMMY0001');


-- ──────────────────────────────────────────────────────────
-- Quick check
-- ──────────────────────────────────────────────────────────
SELECT name, email, role FROM gympro_auth.users
WHERE email IN ('shreyasshreyu405@gmail.com','andrewfake27092004@gmail.com','whatever17092003@gmail.com');
