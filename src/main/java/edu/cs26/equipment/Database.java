package edu.cs26.equipment;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

final class Database {
    private final String url = System.getenv().getOrDefault("DB_URL",
        "jdbc:mysql://localhost:3306/classroom_equipment_db?connectionTimeZone=LOCAL");
    private final String user = System.getenv().getOrDefault("DB_USER", "equipment_app");
    private final String password = System.getenv().getOrDefault("DB_PASSWORD", "");

    Connection open() throws SQLException {
        return DriverManager.getConnection(url, user, password);
    }

    void initialize() throws SQLException {
        try (Connection connection = open(); Statement sql = connection.createStatement()) {
            sql.executeUpdate("""
                CREATE TABLE IF NOT EXISTS users (
                  id INT NOT NULL AUTO_INCREMENT PRIMARY KEY,
                  username VARCHAR(50) NOT NULL UNIQUE,
                  display_name VARCHAR(100) NOT NULL,
                  password_hash VARCHAR(255) NOT NULL,
                  salt VARCHAR(80) NOT NULL,
                  role VARCHAR(16) NOT NULL DEFAULT 'STAFF',
                  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
                ) ENGINE=InnoDB
                """);
            sql.executeUpdate("""
                CREATE TABLE IF NOT EXISTS students (
                  student_id VARCHAR(32) NOT NULL PRIMARY KEY,
                  full_name VARCHAR(120) NOT NULL,
                  course VARCHAR(60) NOT NULL DEFAULT '',
                  section VARCHAR(40) NOT NULL DEFAULT ''
                ) ENGINE=InnoDB
                """);
            sql.executeUpdate("""
                CREATE TABLE IF NOT EXISTS equipment (
                  equipment_id VARCHAR(32) NOT NULL PRIMARY KEY,
                  name VARCHAR(120) NOT NULL,
                  category VARCHAR(24) NOT NULL,
                  notes VARCHAR(500) NOT NULL DEFAULT '',
                  status VARCHAR(16) NOT NULL DEFAULT 'AVAILABLE'
                ) ENGINE=InnoDB
                """);
            sql.executeUpdate("""
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
                ) ENGINE=InnoDB
                """);
        }
    }
}
