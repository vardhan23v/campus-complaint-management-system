-- ---------------------------------------------------------------------------
-- Nitte Resolve - campus complaint management schema
--
-- Run it with:
--   mysql -u root -p < src/main/resources/schema.sql
--
-- Safe to run more than once: every statement uses IF NOT EXISTS and nothing
-- is dropped. To wipe the database and start over, run reset-database.sql
-- (destructive) instead.
-- ---------------------------------------------------------------------------

CREATE DATABASE IF NOT EXISTS campus_complaint_db
  CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci;

USE campus_complaint_db;

-- Everybody who can log in.
--   STUDENT - students and faculty/college staff who report complaints
--   STAFF   - maintenance / support staff who fix them
--   ADMIN   - the complaint cell; never created through the public form
CREATE TABLE IF NOT EXISTS users (
  id            INT AUTO_INCREMENT PRIMARY KEY,
  name          VARCHAR(100) NOT NULL,
  email         VARCHAR(150) NOT NULL,
  password_hash VARCHAR(255) NOT NULL,                       -- pbkdf2_sha256$iterations$salt$hash
  role          ENUM('STUDENT','ADMIN','STAFF') NOT NULL DEFAULT 'STUDENT',
  phone         VARCHAR(20)  NULL,
  department    VARCHAR(100) NULL,                           -- branch for students, trade for staff
  is_active     BOOLEAN      NOT NULL DEFAULT TRUE,          -- deactivated accounts cannot log in
  created_at    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  CONSTRAINT uq_users_email UNIQUE (email),
  INDEX idx_users_role (role, is_active)
) ENGINE=InnoDB;

-- Complaint categories, managed by the administrator.
CREATE TABLE IF NOT EXISTS categories (
  id          INT AUTO_INCREMENT PRIMARY KEY,
  name        VARCHAR(80)  NOT NULL,
  description VARCHAR(255) NULL,
  is_active   BOOLEAN      NOT NULL DEFAULT TRUE,            -- inactive categories are hidden from the form
  created_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT uq_categories_name UNIQUE (name)
) ENGINE=InnoDB;

-- One row per complaint. status is the CURRENT status only; the full
-- lifecycle lives in complaint_status_history.
CREATE TABLE IF NOT EXISTS complaints (
  id                INT AUTO_INCREMENT PRIMARY KEY,
  complaint_code    VARCHAR(20)  NOT NULL,                   -- e.g. NIT-2026-00001
  user_id           INT          NOT NULL,                   -- who reported it
  category_id       INT          NOT NULL,
  title             VARCHAR(150) NOT NULL,
  description       TEXT         NOT NULL,
  block             VARCHAR(60)  NOT NULL,
  floor             VARCHAR(30)  NOT NULL,
  location          VARCHAR(120) NOT NULL,                   -- room / lab / spot
  priority          ENUM('LOW','MEDIUM','HIGH','URGENT') NOT NULL DEFAULT 'MEDIUM',
  status            ENUM('SUBMITTED','UNDER_REVIEW','ASSIGNED','IN_PROGRESS','RESOLVED','CLOSED')
                    NOT NULL DEFAULT 'SUBMITTED',
  attachment_path   VARCHAR(255) NULL,                       -- generated file name in the private upload folder
  assigned_staff_id INT          NULL,
  created_at        DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at        DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  resolved_at       DATETIME     NULL,
  closed_at         DATETIME     NULL,
  CONSTRAINT uq_complaints_code UNIQUE (complaint_code),
  CONSTRAINT fk_complaints_user     FOREIGN KEY (user_id)           REFERENCES users (id)      ON DELETE RESTRICT,
  CONSTRAINT fk_complaints_category FOREIGN KEY (category_id)       REFERENCES categories (id) ON DELETE RESTRICT,
  CONSTRAINT fk_complaints_staff    FOREIGN KEY (assigned_staff_id) REFERENCES users (id)      ON DELETE SET NULL,
  INDEX idx_complaints_status   (status),
  INDEX idx_complaints_user     (user_id, created_at),
  INDEX idx_complaints_staff    (assigned_staff_id, status),
  INDEX idx_complaints_category (category_id),
  INDEX idx_complaints_priority (priority),
  INDEX idx_complaints_created  (created_at)
) ENGINE=InnoDB;

-- Append-only audit trail: one row per status change or remark.
-- Rows are inserted, never updated or deleted by the application.
-- previous_status is NULL for the very first SUBMITTED row; it equals
-- new_status when a remark was added without changing the status.
CREATE TABLE IF NOT EXISTS complaint_status_history (
  id              INT AUTO_INCREMENT PRIMARY KEY,
  complaint_id    INT NOT NULL,
  previous_status ENUM('SUBMITTED','UNDER_REVIEW','ASSIGNED','IN_PROGRESS','RESOLVED','CLOSED') NULL,
  new_status      ENUM('SUBMITTED','UNDER_REVIEW','ASSIGNED','IN_PROGRESS','RESOLVED','CLOSED') NOT NULL,
  updated_by      INT NOT NULL,
  remarks         VARCHAR(1000) NULL,
  created_at      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT fk_history_complaint FOREIGN KEY (complaint_id) REFERENCES complaints (id) ON DELETE CASCADE,
  CONSTRAINT fk_history_user      FOREIGN KEY (updated_by)   REFERENCES users (id)      ON DELETE RESTRICT,
  INDEX idx_history_complaint (complaint_id, created_at)
) ENGINE=InnoDB;

-- Every (re)assignment of a complaint to a maintenance staff member.
CREATE TABLE IF NOT EXISTS complaint_assignments (
  id           INT AUTO_INCREMENT PRIMARY KEY,
  complaint_id INT NOT NULL,
  staff_id     INT NOT NULL,
  assigned_by  INT NOT NULL,
  assigned_at  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  remarks      VARCHAR(500) NULL,
  CONSTRAINT fk_assign_complaint FOREIGN KEY (complaint_id) REFERENCES complaints (id) ON DELETE CASCADE,
  CONSTRAINT fk_assign_staff     FOREIGN KEY (staff_id)     REFERENCES users (id)      ON DELETE RESTRICT,
  CONSTRAINT fk_assign_admin     FOREIGN KEY (assigned_by)  REFERENCES users (id)      ON DELETE RESTRICT,
  INDEX idx_assign_complaint (complaint_id, assigned_at),
  INDEX idx_assign_staff     (staff_id)
) ENGINE=InnoDB;

-- One feedback entry per resolved complaint, written by the reporter.
CREATE TABLE IF NOT EXISTS feedback (
  id           INT AUTO_INCREMENT PRIMARY KEY,
  complaint_id INT NOT NULL,
  student_id   INT NOT NULL,
  rating       TINYINT NOT NULL,
  comments     VARCHAR(1000) NULL,
  created_at   DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT uq_feedback_complaint UNIQUE (complaint_id),
  CONSTRAINT chk_feedback_rating CHECK (rating BETWEEN 1 AND 5),
  CONSTRAINT fk_feedback_complaint FOREIGN KEY (complaint_id) REFERENCES complaints (id) ON DELETE CASCADE,
  CONSTRAINT fk_feedback_student   FOREIGN KEY (student_id)   REFERENCES users (id)      ON DELETE RESTRICT
) ENGINE=InnoDB;

-- In-app notifications.
CREATE TABLE IF NOT EXISTS notifications (
  id           INT AUTO_INCREMENT PRIMARY KEY,
  user_id      INT NOT NULL,
  complaint_id INT NULL,
  title        VARCHAR(150) NOT NULL,
  message      VARCHAR(500) NOT NULL,
  is_read      BOOLEAN NOT NULL DEFAULT FALSE,
  created_at   DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT fk_notifications_user      FOREIGN KEY (user_id)      REFERENCES users (id)      ON DELETE CASCADE,
  CONSTRAINT fk_notifications_complaint FOREIGN KEY (complaint_id) REFERENCES complaints (id) ON DELETE CASCADE,
  INDEX idx_notifications_user (user_id, is_read, created_at)
) ENGINE=InnoDB;
