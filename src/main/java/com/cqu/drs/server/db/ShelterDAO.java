package com.cqu.drs.server.db;

import com.cqu.drs.model.AlertLevel;
import com.cqu.drs.model.AlertStatus;
import com.cqu.drs.model.EvacuationAlert;
import com.cqu.drs.model.Shelter;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/** Data access for {@code shelters} and {@code evacuation_alerts} (Evacuation feature). */
public class ShelterDAO {

    public int insertShelter(Connection c, Shelter s) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(
                "INSERT INTO shelters (name, location, capacity, occupancy, contact, closed, updated_at) VALUES (?,?,?,?,?,?,?)",
                Statement.RETURN_GENERATED_KEYS)) {
            LocalDateTime now = LocalDateTime.now();
            ps.setString(1, s.getName());
            ps.setString(2, s.getLocation());
            ps.setInt(3, s.getCapacity());
            ps.setInt(4, s.getOccupancy());
            ps.setString(5, s.getContact());
            ps.setBoolean(6, s.isClosed());
            ps.setObject(7, now);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                keys.next();
                s.setShelterId(keys.getInt(1));
                s.setLastUpdated(now);
                return s.getShelterId();
            }
        }
    }

    public List<Shelter> findAllShelters(Connection c) throws SQLException {
        List<Shelter> list = new ArrayList<>();
        try (PreparedStatement ps = c.prepareStatement("SELECT * FROM shelters ORDER BY name");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(mapShelter(rs));
            }
        }
        return list;
    }

    public boolean shelterNameExists(Connection c, String name) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement("SELECT 1 FROM shelters WHERE name = ?")) {
            ps.setString(1, name);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    /** Locks the shelter row so concurrent check-ins from several clients cannot overfill it. */
    public Shelter findShelterForUpdate(Connection c, int shelterId) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement("SELECT * FROM shelters WHERE shelter_id = ? FOR UPDATE")) {
            ps.setInt(1, shelterId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? mapShelter(rs) : null;
            }
        }
    }

    public void updateOccupancy(Connection c, Shelter s) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(
                "UPDATE shelters SET occupancy = ?, closed = ?, updated_at = ? WHERE shelter_id = ?")) {
            ps.setInt(1, s.getOccupancy());
            ps.setBoolean(2, s.isClosed());
            ps.setObject(3, LocalDateTime.now());
            ps.setInt(4, s.getShelterId());
            ps.executeUpdate();
        }
    }

    public int insertAlert(Connection c, EvacuationAlert a) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(
                "INSERT INTO evacuation_alerts (report_id, zone, level, message, shelter_id, status, issued_by, issued_at)"
                        + " VALUES (?,?,?,?,?,?,?,?)", Statement.RETURN_GENERATED_KEYS)) {
            LocalDateTime now = LocalDateTime.now();
            ps.setInt(1, a.getReportId());
            ps.setString(2, a.getZone());
            ps.setString(3, a.getLevel().name());
            ps.setString(4, a.getMessage());
            if (a.getShelterId() == null) {
                ps.setNull(5, Types.INTEGER);
            } else {
                ps.setInt(5, a.getShelterId());
            }
            ps.setString(6, a.getStatus().name());
            ps.setString(7, a.getIssuedBy());
            ps.setObject(8, now);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                keys.next();
                a.setAlertId(keys.getInt(1));
                a.setIssuedAt(now);
                return a.getAlertId();
            }
        }
    }

    /** Active alerts first, most severe first, newest first. */
    public List<EvacuationAlert> findAllAlerts(Connection c) throws SQLException {
        List<EvacuationAlert> list = new ArrayList<>();
        try (PreparedStatement ps = c.prepareStatement(
                "SELECT a.*, s.name AS shelter_name, r.type AS r_type, r.location AS r_location"
                        + " FROM evacuation_alerts a JOIN disaster_reports r ON a.report_id = r.report_id"
                        + " LEFT JOIN shelters s ON a.shelter_id = s.shelter_id"
                        + " ORDER BY (a.status = 'LIFTED') ASC,"
                        + " FIELD(a.level, 'EMERGENCY_WARNING', 'WATCH_AND_ACT', 'ADVICE'), a.issued_at DESC");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(mapAlert(rs));
            }
        }
        return list;
    }

    public EvacuationAlert findAlertForUpdate(Connection c, int alertId) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(
                "SELECT a.*, s.name AS shelter_name, r.type AS r_type, r.location AS r_location"
                        + " FROM evacuation_alerts a JOIN disaster_reports r ON a.report_id = r.report_id"
                        + " LEFT JOIN shelters s ON a.shelter_id = s.shelter_id WHERE a.alert_id = ? FOR UPDATE")) {
            ps.setInt(1, alertId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? mapAlert(rs) : null;
            }
        }
    }

    public void updateAlertStatus(Connection c, EvacuationAlert a) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(
                "UPDATE evacuation_alerts SET status = ?, lifted_at = ? WHERE alert_id = ?")) {
            ps.setString(1, a.getStatus().name());
            ps.setObject(2, a.getLiftedAt());
            ps.setInt(3, a.getAlertId());
            ps.executeUpdate();
        }
    }

    private Shelter mapShelter(ResultSet rs) throws SQLException {
        Shelter s = new Shelter();
        s.setShelterId(rs.getInt("shelter_id"));
        s.setName(rs.getString("name"));
        s.setLocation(rs.getString("location"));
        s.setCapacity(rs.getInt("capacity"));
        s.setOccupancy(rs.getInt("occupancy"));
        s.setContact(rs.getString("contact"));
        s.setClosed(rs.getBoolean("closed"));
        s.setLastUpdated(rs.getObject("updated_at", LocalDateTime.class));
        return s;
    }

    private EvacuationAlert mapAlert(ResultSet rs) throws SQLException {
        EvacuationAlert a = new EvacuationAlert(rs.getInt("report_id"), rs.getString("zone"),
                AlertLevel.valueOf(rs.getString("level")), rs.getString("message"));
        a.setAlertId(rs.getInt("alert_id"));
        int shelterId = rs.getInt("shelter_id");
        a.setShelterId(rs.wasNull() ? null : shelterId);
        a.setShelterName(rs.getString("shelter_name"));
        a.setStatus(AlertStatus.valueOf(rs.getString("status")));
        a.setIssuedBy(rs.getString("issued_by"));
        a.setIssuedAt(rs.getObject("issued_at", LocalDateTime.class));
        a.setLiftedAt(rs.getObject("lifted_at", LocalDateTime.class));
        a.setReportSummary("#" + a.getReportId() + " " + rs.getString("r_type") + " - " + rs.getString("r_location"));
        return a;
    }
}
