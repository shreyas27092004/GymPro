-- GymPro MySQL Initialization
-- This runs automatically when MySQL container starts for the first time

CREATE DATABASE IF NOT EXISTS gympro_auth;
CREATE DATABASE IF NOT EXISTS gympro_member;
CREATE DATABASE IF NOT EXISTS gympro_trainer;
CREATE DATABASE IF NOT EXISTS gympro_plan;
CREATE DATABASE IF NOT EXISTS gympro_booking;
CREATE DATABASE IF NOT EXISTS gympro_payment;
CREATE DATABASE IF NOT EXISTS gympro_notification;

-- Grant root access to all DBs (already default for root, but explicit)
GRANT ALL PRIVILEGES ON gympro_auth.* TO 'root'@'%';
GRANT ALL PRIVILEGES ON gympro_member.* TO 'root'@'%';
GRANT ALL PRIVILEGES ON gympro_trainer.* TO 'root'@'%';
GRANT ALL PRIVILEGES ON gympro_plan.* TO 'root'@'%';
GRANT ALL PRIVILEGES ON gympro_booking.* TO 'root'@'%';
GRANT ALL PRIVILEGES ON gympro_payment.* TO 'root'@'%';
GRANT ALL PRIVILEGES ON gympro_notification.* TO 'root'@'%';
FLUSH PRIVILEGES;
