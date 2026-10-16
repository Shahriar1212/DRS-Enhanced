package com.cqu.drs.server.db;

import com.cqu.drs.model.Department;
import com.cqu.drs.model.DepartmentType;
import com.cqu.drs.model.DisasterReport;
import com.cqu.drs.model.DisasterType;
import com.cqu.drs.model.ResponseTask;
import com.cqu.drs.model.Status;
import com.cqu.drs.model.Volunteer;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/** Data access for {@code response_tasks} (joined with reports, departments and volunteers). */
public class TaskDAO {

    private static final String SELECT = "SELECT t.*, r.type AS r_type, r.location AS r_location,"
            + " d.name AS d_name, d.type AS d_type, d.contact_info AS d_contact,"
            + " v.name AS v_name, v.skill AS v_skill"
            + " FROM response_tasks t"
            + " JOIN disaster_reports r ON t.report_id = r.report_id"
            + " JOIN departments d ON t.department_id = d.department_id"
            + " LEFT JOIN volunteers v ON t.volunteer_id = v.volunteer_id";

    public int insert(Connection c, ResponseTask t) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(
                "INSERT INTO response_tasks (report_id, department_id, volunteer_id, description, priority, status,"
                        + " created_at, updated_at) VALUES (?,?,?,?,?,?,?,?)", Statement.RETURN_GENERATED_KEYS)) {
            LocalDateTime now = LocalDateTime.now();
            ps.setInt(1, t.getDisasterReport().getReportId());
            ps.setInt(2, t.getDepartment().getDepartmentId());
            if (t.getAssignedVolunteer() == null) {
                ps.setNull(3, Types.INTEGER);
            } else {
                ps.setInt(3, t.getAssignedVolunteer().getVolunteerId());
            }
            ps.setString(4, t.getDescription());
            ps.setInt(5, t.getPriority());
            ps.setString(6, t.getStatus().name());
            ps.setObject(7, now);
            ps.setObject(8, now);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                keys.next();
                t.setTaskId(keys.getInt(1));
                t.setCreatedAt(now);
                t.setLastUpdated(now);
                return t.getTaskId();
            }
        }
    }

    /** Open tasks first, then by priority (highest first). */
    public List<ResponseTask> findAll(Connection c) throws SQLException {
        List<ResponseTask> list = new ArrayList<>();
        try (PreparedStatement ps = c.prepareStatement(SELECT
                + " ORDER BY (t.status = 'RESOLVED') ASC, t.priority DESC, t.task_id");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(map(rs));
            }
        }
        return list;
    }

    public ResponseTask findById(Connection c, int taskId) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(SELECT + " WHERE t.task_id = ?")) {
            ps.setInt(1, taskId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? map(rs) : null;
            }
        }
    }

    public void update(Connection c, ResponseTask t) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(
                "UPDATE response_tasks SET volunteer_id = ?, status = ?, updated_at = ? WHERE task_id = ?")) {
            if (t.getAssignedVolunteer() == null) {
                ps.setNull(1, Types.INTEGER);
            } else {
                ps.setInt(1, t.getAssignedVolunteer().getVolunteerId());
            }
            ps.setString(2, t.getStatus().name());
            ps.setObject(3, LocalDateTime.now());
            ps.setInt(4, t.getTaskId());
            ps.executeUpdate();
        }
    }

    /** Number of tasks for a report that are not yet resolved. */
    public int countOpenForReport(Connection c, int reportId) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(
                "SELECT COUNT(*) FROM response_tasks WHERE report_id = ? AND status <> 'RESOLVED'")) {
            ps.setInt(1, reportId);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getInt(1);
            }
        }
    }

    private ResponseTask map(ResultSet rs) throws SQLException {
        DisasterReport r = new DisasterReport();
        r.setReportId(rs.getInt("report_id"));
        r.setType(DisasterType.valueOf(rs.getString("r_type")));
        r.setLocation(rs.getString("r_location"));
        Department d = new Department(rs.getString("d_name"), DepartmentType.valueOf(rs.getString("d_type")),
                rs.getString("d_contact"));
        d.setDepartmentId(rs.getInt("department_id"));
        ResponseTask t = new ResponseTask(rs.getString("description"), rs.getInt("priority"), r, d);
        t.setTaskId(rs.getInt("task_id"));
        t.setStatus(Status.valueOf(rs.getString("status")));
        int volunteerId = rs.getInt("volunteer_id");
        if (!rs.wasNull()) {
            Volunteer v = new Volunteer(rs.getString("v_name"), null, rs.getString("v_skill"));
            v.setVolunteerId(volunteerId);
            v.setAvailable(false);
            t.setAssignedVolunteer(v);
        }
        t.setCreatedAt(rs.getObject("created_at", LocalDateTime.class));
        t.setLastUpdated(rs.getObject("updated_at", LocalDateTime.class));
        return t;
    }
}
