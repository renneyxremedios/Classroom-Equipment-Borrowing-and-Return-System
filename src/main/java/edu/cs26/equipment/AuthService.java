package edu.cs26.equipment;

import java.sql.*;
import java.util.Arrays;

final class AuthService {
    private final Database database;
    AuthService(Database database) { this.database = database; }

    boolean needsAdminSetup() throws SQLException {
        try (Connection c = database.open();
             PreparedStatement p = c.prepareStatement("SELECT COUNT(*) FROM users");
             ResultSet r = p.executeQuery()) {
            r.next();
            return r.getInt(1) == 0;
        }
    }

    void createFirstAdmin(String username, String displayName, char[] password) throws SQLException {
        if (username == null || !username.matches("[A-Za-z0-9_.-]{3,50}"))
            throw new IllegalArgumentException("Username must be 3-50 letters, digits, _, . or -.");
        if (displayName == null || displayName.isBlank())
            throw new IllegalArgumentException("Display name is required.");
        if (password.length < 8)
            throw new IllegalArgumentException("Use a password of at least 8 characters.");
        // First account initialization is serialized to prevent accidentally creating another admin.
        try (Connection c = database.open()) {
            c.setAutoCommit(false);
            try (Statement s = c.createStatement()) {
                s.executeQuery("SELECT id FROM users FOR UPDATE").close();
                try (PreparedStatement count = c.prepareStatement("SELECT COUNT(*) FROM users");
                     ResultSet rs = count.executeQuery()) {
                    rs.next();
                    if (rs.getInt(1) != 0) throw new IllegalStateException("Administrator already exists.");
                }
                String salt = Passwords.newSalt();
                try (PreparedStatement p = c.prepareStatement(
                    "INSERT INTO users (username,display_name,password_hash,salt,role) VALUES (?,?,?,?,?)")) {
                    p.setString(1, username.trim());
                    p.setString(2, displayName.trim());
                    p.setString(3, Passwords.hash(password, salt));
                    p.setString(4, salt);
                    p.setString(5, "ADMIN");
                    p.executeUpdate();
                }
                c.commit();
            } catch (Exception e) {
                c.rollback();
                if (e instanceof SQLException sql) throw sql;
                throw e;
            }
        }
    }

    Account login(String username, char[] password) throws SQLException {
        try (Connection c = database.open();
             PreparedStatement p = c.prepareStatement(
                 "SELECT id,username,display_name,role,password_hash,salt FROM users WHERE username=?")) {
            p.setString(1, username.trim());
            try (ResultSet r = p.executeQuery()) {
                if (!r.next()) return null;
                if (!Passwords.matches(password, r.getString("salt"), r.getString("password_hash"))) return null;
                return new Account(r.getInt("id"), r.getString("username"),
                    r.getString("display_name"), r.getString("role"));
            }
        }
    }
}
