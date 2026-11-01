-- ============================================================
-- GymPro — STEP 2 of 3: SCHEMA FIX + SEED
-- Run AFTER all services have started once (tables must exist).
--
--   mysql -u root -p < 02-post-hibernate.sql
--
-- Fully idempotent: safe to re-run, no duplicate rows.
-- All sample logins use password: admin123
-- ============================================================

-- ------------------------------------------------------------
-- Schema fix: allow NULL phone
-- (ddl-auto=update never relaxes an existing NOT NULL.)
-- ------------------------------------------------------------
ALTER TABLE gympro_member.members   MODIFY phone VARCHAR(255) NULL;
ALTER TABLE gympro_trainer.trainers MODIFY phone VARCHAR(255) NULL;

SET NAMES utf8mb4 COLLATE utf8mb4_unicode_ci;

-- ------------------------------------------------------------
-- gympro_auth
-- ------------------------------------------------------------
USE gympro_auth;

SET @pw = '$2a$10$amI4WQ4jR1GvL6xq0mpDDeIr8gohtUgedbpFE1RbhDzkrOPC8A/YO';

INSERT INTO users (name, email, password, role)
SELECT v.name, v.email, v.password, v.role
FROM (
  SELECT 'Admin User' AS name, 'admin@gympro.com' AS email, @pw AS password, 'ADMIN' AS role
  UNION ALL SELECT 'Test Trainer', 'trainer@gympro.com',            @pw, 'TRAINER'
  UNION ALL SELECT 'Test Member',  'member@gympro.com',             @pw, 'MEMBER'
  UNION ALL SELECT 'Shreyas',      'shreyasshreyu405@gmail.com',    @pw, 'ADMIN'
  UNION ALL SELECT 'Vishwa',       'whatever17092003@gmail.com',    @pw, 'MEMBER'
  UNION ALL SELECT 'Jayanth',      'jayanthvvo395@gmail.com',       @pw, 'MEMBER'
  UNION ALL SELECT 'Chethan',      'chethshivu07@gmail.com',        @pw, 'TRAINER'
  UNION ALL SELECT 'Admin2',       'yourqmail27@gmail.com',         @pw, 'ADMIN'
  UNION ALL SELECT 'Andrew',       'andrewfake27092004@gmail.com',  @pw, 'TRAINER'
) v
WHERE NOT EXISTS (
  SELECT 1 FROM users u WHERE u.email COLLATE utf8mb4_unicode_ci = v.email COLLATE utf8mb4_unicode_ci
);

-- ------------------------------------------------------------
-- gympro_member
-- ------------------------------------------------------------
USE gympro_member;

INSERT INTO members (name, email, phone, address, gender, status)
SELECT 'Test Member', 'member@gympro.com', '9876543210', 'Mysuru, Karnataka', 'MALE', 'ACTIVE'
WHERE NOT EXISTS (SELECT 1 FROM members WHERE email = 'member@gympro.com');

-- ------------------------------------------------------------
-- gympro_trainer  (dated sessions model: session_date)
-- ------------------------------------------------------------
USE gympro_trainer;

INSERT INTO trainers (name, email, phone, specialization, experience_years, status, session_fee)
SELECT 'Test Trainer', 'trainer@gympro.com', '9123456789', 'Weight Loss', 5, 'ACTIVE', 500.00
WHERE NOT EXISTS (SELECT 1 FROM trainers WHERE email = 'trainer@gympro.com');

-- Next-few-days slots for the test trainer (looked up by email,
-- not a hard-coded id; skipped if the slot already exists).
INSERT INTO trainer_schedules
  (trainer_id, session_date, start_time, end_time, max_capacity, booked_count, cancelled, available)
SELECT t.id, DATE_ADD(CURDATE(), INTERVAL s.d DAY), s.st, s.et, s.cap, 0, 0, 1
FROM trainers t
JOIN (
  SELECT 1 AS d, '09:00' AS st, '10:00' AS et, 1  AS cap
  UNION ALL SELECT 2, '09:00', '10:00', 1
  UNION ALL SELECT 3, '14:00', '15:00', 10
) s
WHERE t.email = 'trainer@gympro.com'
  AND NOT EXISTS (
    SELECT 1 FROM trainer_schedules x
    WHERE x.trainer_id = t.id
      AND x.session_date = DATE_ADD(CURDATE(), INTERVAL s.d DAY)
      AND x.start_time = s.st
  );

-- ------------------------------------------------------------
-- gympro_plan  (priority_level must be set per tier)
-- ------------------------------------------------------------
USE gympro_plan;

INSERT INTO membership_plans
  (plan_name, description, duration_type, price, duration_days, active,
   sessions_included, priority_level, trainer_discount_percent, dedicated_trainer, priority_booking)
SELECT v.* FROM (
  SELECT 'Silver Monthly' AS plan_name, 'Basic gym access for 1 month' AS description,
         'MONTHLY' AS duration_type, 999 AS price, 30 AS duration_days, 1 AS active,
         0 AS sessions_included, 1 AS priority_level, 0 AS trainer_discount_percent,
         0 AS dedicated_trainer, 0 AS priority_booking
  UNION ALL SELECT 'Gold Quarterly',  'Full access for 3 months',    'QUARTERLY', 2499, 90,  1, 2, 2, 10, 0, 1
  UNION ALL SELECT 'Platinum Yearly', 'Premium access for full year', 'YEARLY',    7999, 365, 1, 6, 3, 25, 1, 1
) v
WHERE NOT EXISTS (
  SELECT 1 FROM membership_plans p
  WHERE p.plan_name COLLATE utf8mb4_unicode_ci = v.plan_name COLLATE utf8mb4_unicode_ci
);

-- booking / payment / notification stay empty on purpose.
