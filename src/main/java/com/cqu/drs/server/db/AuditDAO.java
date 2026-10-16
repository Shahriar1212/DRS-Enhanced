package com.cqu.drs.server.db;

import com.cqu.drs.model.AuditEntry;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/** Data access for the append-only {@code audit_log} table. */
public class AuditDAO {

    public long insert(Connection c, AuditEntry e) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(
                "INSERT INTO audit_log (ts, username, action, details, prev_hash, entry_hash, signature) VALUES (?,?,?,?,?,?,?)",
                Statement.RETURN_GENERATED_KEYS)) {
            ps.setObject(1, e.getTimestamp());
            ps.setString(2, e.getUsername());
            ps.setString(3, e.getAction());
            ps.setString(4, e.getDetails());
            ps.setString(5, e.getPreviousHash());
            ps.setString(6, e.getEntryHash());
            ps.setString(7, e.getSignature());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                keys.next();
                e.setAuditId(keys.getLong(1));
                return e.getAuditId();
            }
        }
    }

    /** Hash of the newest entry, or 64 zeros for an empty log (genesis). */
    public String lastHash(Connection c) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(
                "SELECT entry_hash FROM audit_log ORDER BY audit_id DESC LIMIT 1");
             ResultSet rs = ps.executeQuery()) {
            return rs.next() ? rs.getString(1) : "0".repeat(64);
        }
    }

    /** Entries in chronological order (oldest first), as needed to walk the hash chain. */
    public List<AuditEntry> findAll(Connection c) throws SQLException {
        List<AuditEntry> list = new ArrayList<>();
        try (PreparedStatement ps = c.prepareStatement("SELECT * FROM audit_log ORDER BY audit_id");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                AuditEntry e = new AuditEntry(rs.getObject("ts", LocalDateTime.class), rs.getString("username"),
                        rs.getString("action"), rs.getString("details"));
                e.setAuditId(rs.getLong("audit_id"));
                e.setPreviousHash(rs.getString("prev_hash"));
                e.setEntryHash(rs.getString("entry_hash"));
                e.setSignature(rs.getString("signature"));
                list.add(e);
            }
        }
        return list;
    }
}
