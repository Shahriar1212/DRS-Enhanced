package com.cqu.drs.server.db;

import com.cqu.drs.model.Role;
import com.cqu.drs.model.User;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/** Data access for the {@code users} table (PreparedStatements throughout to prevent SQL injection). */
public class UserDAO {

    /** Server-side view of a user row, including secrets that never leave the server. */
    public static class UserRecord {
        public User user;
        public String passwordHash;
        public String publicKey;
        public String privateKeyEnc;
        public int failedAttempts;
    }

    public int insert(Connection c, User u, String passwordHash, String publicKey, String privateKeyEnc)
            throws SQLException {
        String sql = "INSERT INTO users (username, full_name, password_hash, role, public_key, private_key_enc,"
                + " active, failed_attempts, created_at) VALUES (?,?,?,?,?,?,?,0,?)";
        try (PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            LocalDateTime now = LocalDateTime.now();
            ps.setString(1, u.getUsername());
            ps.setString(2, u.getFullName());
            ps.setString(3, passwordHash);
            ps.setString(4, u.getRole().name());
            ps.setString(5, publicKey);
            ps.setString(6, privateKeyEnc);
            ps.setBoolean(7, u.isActive());
            ps.setObject(8, now);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                keys.next();
                u.setUserId(keys.getInt(1));
                u.setCreatedAt(now);
                return u.getUserId();
            }
        }
    }

    public UserRecord findByUsername(Connection c, String username) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement("SELECT * FROM users WHERE username = ?")) {
            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? mapRecord(rs) : null;
            }
        }
    }

    public List<User> findAll(Connection c) throws SQLException {
        List<User> list = new ArrayList<>();
        try (PreparedStatement ps = c.prepareStatement("SELECT * FROM users ORDER BY user_id");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(mapRecord(rs).user);
            }
        }
        return list;
    }

    public int count(Connection c) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement("SELECT COUNT(*) FROM users");
             ResultSet rs = ps.executeQuery()) {
            rs.next();
            return rs.getInt(1);
        }
    }

    public void setFailedAttempts(Connection c, String username, int attempts) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement("UPDATE users SET failed_attempts = ? WHERE username = ?")) {
            ps.setInt(1, attempts);
            ps.setString(2, username);
            ps.executeUpdate();
        }
    }

    public boolean setActive(Connection c, int userId, boolean active) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(
                "UPDATE users SET active = ?, failed_attempts = 0 WHERE user_id = ?")) {
            ps.setBoolean(1, active);
            ps.setInt(2, userId);
            return ps.executeUpdate() == 1;
        }
    }

    private UserRecord mapRecord(ResultSet rs) throws SQLException {
        User u = new User();
        u.setUserId(rs.getInt("user_id"));
        u.setUsername(rs.getString("username"));
        u.setFullName(rs.getString("full_name"));
        u.setRole(Role.valueOf(rs.getString("role")));
        u.setActive(rs.getBoolean("active"));
        u.setCreatedAt(rs.getObject("created_at", LocalDateTime.class));
        UserRecord r = new UserRecord();
        r.user = u;
        r.passwordHash = rs.getString("password_hash");
        r.publicKey = rs.getString("public_key");
        r.privateKeyEnc = rs.getString("private_key_enc");
        r.failedAttempts = rs.getInt("failed_attempts");
        return r;
    }
}
