package com.cqu.drs.server.db;

import com.cqu.drs.model.DamageAssessment;
import com.cqu.drs.model.DamageLevel;
import com.cqu.drs.model.InfrastructureCategory;
import com.cqu.drs.model.JobStatus;
import com.cqu.drs.model.RestorationJob;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/** Data access for {@code damage_assessments} and {@code restoration_jobs} (Damage feature). */
public class DamageDAO {

    public int insertAssessment(Connection c, DamageAssessment a) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(
                "INSERT INTO damage_assessments (report_id, category, damage_level, estimated_cost, affected_people,"
                        + " notes, assessed_by, assessed_at) VALUES (?,?,?,?,?,?,?,?)", Statement.RETURN_GENERATED_KEYS)) {
            LocalDateTime now = LocalDateTime.now();
            ps.setInt(1, a.getReportId());
            ps.setString(2, a.getCategory().name());
            ps.setString(3, a.getDamageLevel().name());
            ps.setDouble(4, a.getEstimatedCost());
            ps.setInt(5, a.getAffectedPeople());
            ps.setString(6, a.getNotes());
            ps.setString(7, a.getAssessedBy());
            ps.setObject(8, now);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                keys.next();
                a.setAssessmentId(keys.getInt(1));
                a.setAssessedAt(now);
                return a.getAssessmentId();
            }
        }
    }

    /** Assessments, optionally for one report ({@code reportId <= 0} = all). */
    public List<DamageAssessment> findAssessments(Connection c, int reportId) throws SQLException {
        String sql = "SELECT a.*, r.type AS r_type, r.location AS r_location FROM damage_assessments a"
                + " JOIN disaster_reports r ON a.report_id = r.report_id"
                + (reportId > 0 ? " WHERE a.report_id = ?" : "") + " ORDER BY a.assessed_at DESC";
        List<DamageAssessment> list = new ArrayList<>();
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            if (reportId > 0) {
                ps.setInt(1, reportId);
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    DamageAssessment a = new DamageAssessment();
                    a.setAssessmentId(rs.getInt("assessment_id"));
                    a.setReportId(rs.getInt("report_id"));
                    a.setCategory(InfrastructureCategory.valueOf(rs.getString("category")));
                    a.setDamageLevel(DamageLevel.valueOf(rs.getString("damage_level")));
                    a.setEstimatedCost(rs.getDouble("estimated_cost"));
                    a.setAffectedPeople(rs.getInt("affected_people"));
                    a.setNotes(rs.getString("notes"));
                    a.setAssessedBy(rs.getString("assessed_by"));
                    a.setAssessedAt(rs.getObject("assessed_at", LocalDateTime.class));
                    a.setReportSummary("#" + a.getReportId() + " " + rs.getString("r_type") + " - " + rs.getString("r_location"));
                    list.add(a);
                }
            }
        }
        return list;
    }

    public int insertJob(Connection c, RestorationJob j) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(
                "INSERT INTO restoration_jobs (assessment_id, department_id, description, priority, progress, status,"
                        + " estimated_days, created_at, updated_at) VALUES (?,?,?,?,?,?,?,?,?)",
                Statement.RETURN_GENERATED_KEYS)) {
            LocalDateTime now = LocalDateTime.now();
            ps.setInt(1, j.getAssessmentId());
            ps.setInt(2, j.getDepartmentId());
            ps.setString(3, j.getDescription());
            ps.setInt(4, j.getPriority());
            ps.setInt(5, j.getProgress());
            ps.setString(6, j.getStatus().name());
            ps.setInt(7, j.getEstimatedDays());
            ps.setObject(8, now);
            ps.setObject(9, now);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                keys.next();
                j.setJobId(keys.getInt(1));
                j.setCreatedAt(now);
                j.setLastUpdated(now);
                return j.getJobId();
            }
        }
    }

    /** Jobs (optionally for one report), unfinished and highest priority first. */
    public List<RestorationJob> findJobs(Connection c, int reportId) throws SQLException {
        String sql = "SELECT j.*, a.report_id, a.category, d.name AS d_name FROM restoration_jobs j"
                + " JOIN damage_assessments a ON j.assessment_id = a.assessment_id"
                + " JOIN departments d ON j.department_id = d.department_id"
                + (reportId > 0 ? " WHERE a.report_id = ?" : "")
                + " ORDER BY (j.status = 'COMPLETED') ASC, j.priority DESC, j.job_id";
        List<RestorationJob> list = new ArrayList<>();
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            if (reportId > 0) {
                ps.setInt(1, reportId);
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapJob(rs));
                }
            }
        }
        return list;
    }

    public RestorationJob findJobForUpdate(Connection c, int jobId) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(
                "SELECT j.*, a.report_id, a.category, d.name AS d_name FROM restoration_jobs j"
                        + " JOIN damage_assessments a ON j.assessment_id = a.assessment_id"
                        + " JOIN departments d ON j.department_id = d.department_id WHERE j.job_id = ? FOR UPDATE")) {
            ps.setInt(1, jobId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? mapJob(rs) : null;
            }
        }
    }

    public void updateJobProgress(Connection c, RestorationJob j) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(
                "UPDATE restoration_jobs SET progress = ?, status = ?, updated_at = ? WHERE job_id = ?")) {
            ps.setInt(1, j.getProgress());
            ps.setString(2, j.getStatus().name());
            ps.setObject(3, LocalDateTime.now());
            ps.setInt(4, j.getJobId());
            ps.executeUpdate();
        }
    }

    private RestorationJob mapJob(ResultSet rs) throws SQLException {
        RestorationJob j = new RestorationJob();
        j.setJobId(rs.getInt("job_id"));
        j.setAssessmentId(rs.getInt("assessment_id"));
        j.setReportId(rs.getInt("report_id"));
        j.setCategory(InfrastructureCategory.valueOf(rs.getString("category")));
        j.setDepartmentId(rs.getInt("department_id"));
        j.setDepartmentName(rs.getString("d_name"));
        j.setDescription(rs.getString("description"));
        j.setPriority(rs.getInt("priority"));
        j.setProgress(rs.getInt("progress"));
        j.setStatus(JobStatus.valueOf(rs.getString("status")));
        j.setEstimatedDays(rs.getInt("estimated_days"));
        j.setCreatedAt(rs.getObject("created_at", LocalDateTime.class));
        j.setLastUpdated(rs.getObject("updated_at", LocalDateTime.class));
        return j;
    }
}
