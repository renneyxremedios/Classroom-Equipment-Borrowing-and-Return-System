-- Classroom Equipment Borrowing and Return Management System
-- Execute as a MySQL administrator. Update the placeholder password before running.
CREATE DATABASE IF NOT EXISTS classroom_equipment_db
  CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

CREATE USER IF NOT EXISTS 'equipment_app'@'localhost'
  IDENTIFIED BY 'CHANGE_ME_TO_A_STRONG_PASSWORD';
GRANT SELECT, INSERT, UPDATE, DELETE, CREATE, ALTER, INDEX
  ON classroom_equipment_db.* TO 'equipment_app'@'localhost';

USE classroom_equipment_db;

CREATE TABLE IF NOT EXISTS users (
  id INT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  username VARCHAR(50) NOT NULL UNIQUE,
  display_name VARCHAR(100) NOT NULL,
  password_hash VARCHAR(255) NOT NULL,
  salt VARCHAR(80) NOT NULL,
  role VARCHAR(16) NOT NULL DEFAULT 'STAFF',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS students (
  student_id VARCHAR(32) NOT NULL PRIMARY KEY,
  full_name VARCHAR(120) NOT NULL,
  course VARCHAR(60) NOT NULL DEFAULT '',
  section VARCHAR(40) NOT NULL DEFAULT ''
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS equipment (
  equipment_id VARCHAR(32) NOT NULL PRIMARY KEY,
  name VARCHAR(120) NOT NULL,
  category VARCHAR(24) NOT NULL,
  notes VARCHAR(500) NOT NULL DEFAULT '',
  status VARCHAR(16) NOT NULL DEFAULT 'AVAILABLE'
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS borrowings (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  requisition_no VARCHAR(50) NOT NULL,
  student_id VARCHAR(32) NOT NULL,
  equipment_id VARCHAR(32) NOT NULL,
  borrowed_at DATETIME NOT NULL,
  due_at DATETIME NOT NULL,
  returned_at DATETIME NULL,
  issued_by INT NOT NULL,
  received_by INT NULL,
  INDEX idx_active (returned_at, due_at),
  INDEX idx_req (requisition_no),
  CONSTRAINT fk_loan_student FOREIGN KEY (student_id) REFERENCES students(student_id),
  CONSTRAINT fk_loan_equipment FOREIGN KEY (equipment_id) REFERENCES equipment(equipment_id),
  CONSTRAINT fk_loan_issued FOREIGN KEY (issued_by) REFERENCES users(id),
  CONSTRAINT fk_loan_received FOREIGN KEY (received_by) REFERENCES users(id)
) ENGINE=InnoDB;
