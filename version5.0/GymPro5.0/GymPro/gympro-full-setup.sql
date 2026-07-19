-- ============================================================
-- GymPro — COMPLETE MySQL SETUP (all-in-one)
-- Merges: 01-reset-databases.sql + 03-fix-phone-nullable.sql
--         + 02-seed-data.sql
--
-- HOW TO RUN (3 steps — this file alone is not enough, because
-- table creation happens via Hibernate, not SQL):
--
--   STEP A. Run PART 1 below now:
--             mysql -u root -p < 00-gympro-full-setup.sql --init-command="SOURCE part1 only"
--           (Simplest: just run this whole file once — PART 1
--            executes fine standalone. PART 2 and PART 3 contain
--            ALTER/INSERT statements that need tables to exist
--            first, so see Step B before they'll succeed.)
--
--   STEP B. Start every microservice ONCE, in order:
--             eureka -> gateway -> auth -> member -> trainer ->
--             plan -> booking -> payment -> notification -> chatbot
--           Each has spring.jpa.hibernate.ddl-auto=update, so
--           Hibernate creates every table fresh from the current
--           entity classes.
--
--   STEP C. Run this SAME file again in full:
--             mysql -u root -p < 00-gympro-full-setup.sql
--           PART 1 is safe to re-run (DROP/CREATE DATABASE is
--           idempotent). PART 2 and PART 3 will now succeed
--           because the tables exist.
--
-- ⚠️ PART 1 deletes ALL existing data in these 7 databases.
-- Back up first if needed, e.g.:
--   mysqldump -u root -p gympro_trainer > trainer_backup.sql
-- ============================================================


-- ============================================================
-- PART 1 — RESET (drop + recreate all 7 databases, empty)
-- ============================================================

DROP DATABASE IF EXISTS gympro_auth;
DROP DATABASE IF EXISTS gympro_member;
DROP DATABASE IF EXISTS gympro_trainer;
DROP DATABASE IF EXISTS gympro_plan;
DROP DATABASE IF EXISTS gympro_booking;
DROP DATABASE IF EXISTS gympro_payment;
DROP DATABASE IF EXISTS gympro_notification;

CREATE DATABASE gympro_auth         CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE gympro_member       CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE gympro_trainer      CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE gympro_plan         CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE gympro_booking      CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE gympro_payment      CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE gympro_notification CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

GRANT ALL PRIVILEGES ON gympro_auth.*         TO 'root'@'%';
GRANT ALL PRIVILEGES ON gympro_member.*       TO 'root'@'%';
GRANT ALL PRIVILEGES ON gympro_trainer.*      TO 'root'@'%';
GRANT ALL PRIVILEGES ON gympro_plan.*         TO 'root'@'%';
GRANT ALL PRIVILEGES ON gympro_booking.*      TO 'root'@'%';
GRANT ALL PRIVILEGES ON gympro_payment.*      TO 'root'@'%';
GRANT ALL PRIVILEGES ON gympro_notification.* TO 'root'@'%';
FLUSH PRIVILEGES;

-- >>> STOP HERE the first time. Now go start every microservice
-- >>> once so Hibernate creates the tables (STEP B above), then
-- >>> re-run this whole file so PART 2 and PART 3 below can run.


-- ============================================================
-- PART 2 — SCHEMA FIX: allow NULL phone
--
-- Why: ddl-auto=update only ADDS new columns/tables, it never
-- relaxes an existing column's NOT NULL constraint. The
-- Trainer/Member entities allow optional phone (so self-
-- registered trainers and new members can be auto-created
-- before a phone number is collected), but the MySQL columns
-- are still NOT NULL from when the tables were first created.
-- Fixing this before seeding so the seed inserts below always
-- succeed even if phone values are ever omitted later.
-- ============================================================

ALTER TABLE gympro_member.members   MODIFY phone  VARCHAR(255) NULL;
ALTER TABLE gympro_trainer.trainers MODIFY phone  VARCHAR(255) NULL;


-- ============================================================
-- PART 3 — SEED DATA
-- All sample logins use password: admin123
-- ============================================================

-- ──────────────────────────────────────────────────────────
-- gympro_auth  (auth-service)
-- ──────────────────────────────────────────────────────────
USE gympro_auth;

INSERT IGNORE INTO users (name, email, password, role) VALUES
('Admin User',   'admin@gympro.com',   '$2a$10$amI4WQ4jR1GvL6xq0mpDDeIr8gohtUgedbpFE1RbhDzkrOPC8A/YO', 'ADMIN'),
('Test Trainer', 'trainer@gympro.com', '$2a$10$amI4WQ4jR1GvL6xq0mpDDeIr8gohtUgedbpFE1RbhDzkrOPC8A/YO', 'TRAINER'),
('Test Member',  'member@gympro.com',  '$2a$10$amI4WQ4jR1GvL6xq0mpDDeIr8gohtUgedbpFE1RbhDzkrOPC8A/YO', 'MEMBER'),

-- Your real accounts
('Shreyas',  'shreyasshreyu405@gmail.com',  '$2a$10$amI4WQ4jR1GvL6xq0mpDDeIr8gohtUgedbpFE1RbhDzkrOPC8A/YO', 'ADMIN'),
('Vishwa',   'whatever17092003@gmail.com',  '$2a$10$amI4WQ4jR1GvL6xq0mpDDeIr8gohtUgedbpFE1RbhDzkrOPC8A/YO', 'MEMBER'),
('Jayanth',  'jayanthvvo395@gmail.com',     '$2a$10$amI4WQ4jR1GvL6xq0mpDDeIr8gohtUgedbpFE1RbhDzkrOPC8A/YO', 'MEMBER'),
('Chethan',  'chethshivu07@gmail.com',      '$2a$10$amI4WQ4jR1GvL6xq0mpDDeIr8gohtUgedbpFE1RbhDzkrOPC8A/YO', 'TRAINER'),
('Admin2',   'yourqmail27@gmail.com',       '$2a$10$amI4WQ4jR1GvL6xq0mpDDeIr8gohtUgedbpFE1RbhDzkrOPC8A/YO', 'ADMIN'),
('Andrew',   'andrewfake27092004@gmail.com','$2a$10$amI4WQ4jR1GvL6xq0mpDDeIr8gohtUgedbpFE1RbhDzkrOPC8A/YO', 'TRAINER');

-- ──────────────────────────────────────────────────────────
-- gympro_member  (member-service)
-- ──────────────────────────────────────────────────────────
USE gympro_member;

INSERT IGNORE INTO members (name, email, phone, address, gender, status) VALUES
('Test Member', 'member@gympro.com', '9876543210', 'Mysuru, Karnataka', 'MALE', 'ACTIVE');

-- ──────────────────────────────────────────────────────────
-- gympro_trainer  (trainer-service)
-- Current model: trainer_schedules uses dated sessions
-- (session_date), NOT the old day_of_week weekly model.
-- ──────────────────────────────────────────────────────────
USE gympro_trainer;

INSERT IGNORE INTO trainers (name, email, phone, specialization, experience_years, status, session_fee) VALUES
('Test Trainer', 'trainer@gympro.com', '9123456789', 'Weight Loss', 5, 'ACTIVE', 500.00);

-- A handful of upcoming dated slots for trainer id 1.
-- (Adjust dates if you're seeding this on a different day —
-- these are just relative "next few days" examples.)
INSERT INTO trainer_schedules
  (trainer_id, session_date, start_time, end_time, max_capacity, booked_count, cancelled, available)
VALUES
  (1, DATE_ADD(CURDATE(), INTERVAL 1 DAY), '09:00', '10:00', 1,  0, 0, 1),
  (1, DATE_ADD(CURDATE(), INTERVAL 2 DAY), '09:00', '10:00', 1,  0, 0, 1),
  (1, DATE_ADD(CURDATE(), INTERVAL 3 DAY), '14:00', '15:00', 10, 0, 0, 1);

-- ──────────────────────────────────────────────────────────
-- gympro_plan  (plan-service)
-- Current model requires priority_level to be set correctly,
-- or every plan looks like "the highest tier" (see prior fix).
-- ──────────────────────────────────────────────────────────
USE gympro_plan;

INSERT IGNORE INTO membership_plans
  (plan_name, description, duration_type, price, duration_days, active,
   sessions_included, priority_level, trainer_discount_percent, dedicated_trainer, priority_booking)
VALUES
  ('Silver Monthly',  'Basic gym access for 1 month', 'MONTHLY',   999,  30,  1, 0,  1, 0,  0, 0),
  ('Gold Quarterly',  'Full access for 3 months',     'QUARTERLY', 2499, 90,  1, 2, 2, 10, 0, 1),
  ('Platinum Yearly', 'Premium access for full year',  'YEARLY',    7999, 365, 1, 6, 3, 25, 1, 1);

-- ============================================================
-- Notes:
--  * gympro_booking, gympro_payment, gympro_notification are
--    left empty on purpose — bookings/payments/notifications
--    are created naturally as you use the app.
--  * If chatbot-service uses a database, it manages its own
--    schema too (nothing to seed here).
--  * PART 1 is idempotent (safe to re-run). PART 2 (ALTER
--    TABLE ... MODIFY) is also safe to re-run — it's a no-op
--    if the column is already nullable. PART 3 uses INSERT
--    IGNORE for the tables where duplicate re-seeding could
--    happen, so re-running the full file is safe overall
--    except that trainer_schedules will insert duplicate slot
--    rows on repeat runs (delete PART 3's trainer_schedules
--    block before re-running if that matters to you).
-- ============================================================
