package com.cqu.drs.server.db;

import com.cqu.drs.model.Volunteer;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/** Data access for {@code volunteers}; the phone column holds AES ciphertext. */
public class VolunteerDAO {

    /** A volunteer plus the still-encrypted phone number. */
    public static class Row {
        public Volunteer volunteer;
        public String phoneEncrypted;
    }

    public int insert(Connection c, Volunteer v, String phoneEncrypted) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(
                "INSERT INTO volunteers (name, phone_enc, skill, available, registered_at) VALUES (?,?,?,?,?)",
                Statement.RETURN_GENERATED_KEYS)) {
            LocalDateTime now = LocalDateTime.now();
            ps.setString(1, v.getName());
            ps.setString(2, phoneEncrypted);
            ps.setString(3, v.getSkill());
            ps.setBoolean(4, v.isAvailable());
            ps.setObject(5, now);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                keys.next();
                v.setVolunteerId(keys.getInt(1));
                v.setRegisteredAt(now);
                return v.getVolunteerId();
            }
        }
    }

    public List<Row> findAll(Connection c) throws SQLException {
        List<Row> list = new ArrayList<>();
        try (PreparedStatement ps = c.prepareStatement("SELECT * FROM volunteers ORDER BY volunteer_id");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(map(rs));
            }
        }
        return list;
    }

    public Row findByIdForUpdate(Connection c, int id) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement("SELECT * FROM volunteers WHERE volunteer_id = ? FOR UPDATE")) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? map(rs) : null;
            }
        }
    }

    public boolean existsByName(Connection c, String name) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement("SELECT 1 FROM volunteers WHERE name = ?")) {
            ps.setString(1, name);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    public void setAvailable(Connection c, int id, boolean available) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement("UPDATE volunteers SET available = ? WHERE volunteer_id = ?")) {
            ps.setBoolean(1, available);
            ps.setInt(2, id);
            ps.executeUpdate();
        }
    }

    private Row map(ResultSet rs) throws SQLException {
        Volunteer v = new Volunteer(rs.getString("name"), null, rs.getString("skill"));
        v.setVolunteerId(rs.getInt("volunteer_id"));
        v.setAvailable(rs.getBoolean("available"));
        v.setRegisteredAt(rs.getObject("registered_at", LocalDateTime.class));
        Row row = new Row();
        row.volunteer = v;
        row.phoneEncrypted = rs.getString("phone_enc");
        return row;
    }
}
