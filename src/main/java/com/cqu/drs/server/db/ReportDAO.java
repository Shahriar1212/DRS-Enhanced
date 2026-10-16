package com.cqu.drs.server.db;

import com.cqu.drs.model.DisasterReport;
import com.cqu.drs.model.DisasterType;
import com.cqu.drs.model.SeverityLevel;
import com.cqu.drs.model.Status;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Data access for {@code disaster_reports}. The reporter phone column holds
 * AES ciphertext; encryption/decryption is done by the service layer.
 */
public class ReportDAO {

    public int insert(Connection c, DisasterReport r, String phoneEncrypted) throws SQLException {
        String sql = "INSERT INTO disaster_reports (type, location, description, reporter_name, reporter_phone_enc,"
                + " severity, priority_score, status, reported_by, signature, date_reported, updated_at)"
                + " VALUES (?,?,?,?,?,?,?,?,?,?,?,?)";
        try (PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            LocalDateTime now = LocalDateTime.now();
            if (r.getDateReported() == null) {
                r.setDateReported(now);
            }
            ps.setString(1, r.getType().name());
            ps.setString(2, r.getLocation());
            ps.setString(3, r.getDescription());
            ps.setString(4, r.getReporterName());
            ps.setString(5, phoneEncrypted);
            ps.setString(6, r.getSeverity() == null ? null : r.getSeverity().name());
            ps.setInt(7, r.getPriorityScore());
            ps.setString(8, r.getStatus().name());
            ps.setString(9, r.getReportedBy());
            ps.setString(10, r.getSignature());
            ps.setObject(11, r.getDateReported());
            ps.setObject(12, now);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                keys.next();
                r.setReportId(keys.getInt(1));
                r.setLastUpdated(now);
                return r.getReportId();
            }
        }
    }

    /** Raw row including the encrypted phone; index 0 = report, index 1 = encrypted phone. */
    public static class Row {
        public DisasterReport report;
        public String phoneEncrypted;
    }

    public Row findById(Connection c, int id, boolean forUpdate) throws SQLException {
        String sql = "SELECT * FROM disaster_reports WHERE report_id = ?" + (forUpdate ? " FOR UPDATE" : "");
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? map(rs) : null;
            }
        }
    }

    /** All reports, most urgent first (unassessed reports first so they get triaged). */
    public List<Row> findAll(Connection c) throws SQLException {
        return query(c, "SELECT * FROM disaster_reports ORDER BY (severity IS NULL) DESC,"
                + " (status = 'RESOLVED') ASC, priority_score DESC, date_reported DESC", null);
    }

    public List<Row> findByReporter(Connection c, String username) throws SQLException {
        return query(c, "SELECT * FROM disaster_reports WHERE reported_by = ? ORDER BY date_reported DESC", username);
    }

    /** Looks for an identical report (used to make the legacy import idempotent). */
    public boolean exists(Connection c, DisasterType type, String location, String description) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(
                "SELECT 1 FROM disaster_reports WHERE type = ? AND location = ? AND description = ?")) {
            ps.setString(1, type.name());
            ps.setString(2, location);
            ps.setString(3, description);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    public void updateAssessment(Connection c, DisasterReport r) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(
                "UPDATE disaster_reports SET severity = ?, priority_score = ?, status = ?, updated_at = ? WHERE report_id = ?")) {
            ps.setString(1, r.getSeverity() == null ? null : r.getSeverity().name());
            ps.setInt(2, r.getPriorityScore());
            ps.setString(3, r.getStatus().name());
            ps.setObject(4, LocalDateTime.now());
            ps.setInt(5, r.getReportId());
            ps.executeUpdate();
        }
    }

    private List<Row> query(Connection c, String sql, String param) throws SQLException {
        List<Row> list = new ArrayList<>();
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            if (param != null) {
                ps.setString(1, param);
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(map(rs));
                }
            }
        }
        return list;
    }

    private Row map(ResultSet rs) throws SQLException {
        DisasterReport r = new DisasterReport();
        r.setReportId(rs.getInt("report_id"));
        r.setType(DisasterType.valueOf(rs.getString("type")));
        r.setLocation(rs.getString("location"));
        r.setDescription(rs.getString("description"));
        r.setReporterName(rs.getString("reporter_name"));
        String sev = rs.getString("severity");
        r.setSeverity(sev == null ? null : SeverityLevel.valueOf(sev));
        r.setPriorityScore(rs.getInt("priority_score"));
        r.setStatus(Status.valueOf(rs.getString("status")));
        r.setReportedBy(rs.getString("reported_by"));
        r.setSignature(rs.getString("signature"));
        r.setDateReported(rs.getObject("date_reported", LocalDateTime.class));
        r.setLastUpdated(rs.getObject("updated_at", LocalDateTime.class));
        Row row = new Row();
        row.report = r;
        row.phoneEncrypted = rs.getString("reporter_phone_enc");
        return row;
    }
}
