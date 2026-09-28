-- ============================================================
-- GymPro — STEP 1 of 3: RESET
--
-- Run:  mysql -u root -p < 01-reset-databases.sql
-- Then: start all microservices once (eureka -> gateway -> auth ->
--       member -> trainer -> plan -> booking -> payment ->
--       notification -> chatbot) so Hibernate (ddl-auto=update)
--       creates the tables.
-- Then: mysql -u root -p < 02-post-hibernate.sql
--
-- WARNING: deletes ALL data in these 7 databases. Never re-run
-- this AFTER the tables were created, or you wipe them.
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

-- No GRANTs needed: root already has full privileges, and
-- GRANT to 'root'@'%' errors if that account doesn't exist.
