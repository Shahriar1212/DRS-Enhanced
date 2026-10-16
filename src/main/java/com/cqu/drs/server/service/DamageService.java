package com.cqu.drs.server.service;

import com.cqu.drs.model.DamageAssessment;
import com.cqu.drs.model.DamageLevel;
import com.cqu.drs.model.Department;
import com.cqu.drs.model.InfrastructureCategory;
import com.cqu.drs.model.RecoverySummary;
import com.cqu.drs.model.RestorationJob;
import com.cqu.drs.model.User;
import com.cqu.drs.server.db.ReportDAO;
import com.cqu.drs.util.RestorationPlanner;
import com.cqu.drs.util.Validation;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * NEW FEATURE 2 - Damage Assessment &amp; Restoration Planning.
 * Field officers record infrastructure damage for a disaster; the system
 * immediately raises a prioritised restoration job for the responsible
 * department, tracks its progress and summarises recovery per disaster.
 */
public class DamageService {

    private final ServerContext ctx;

    DamageService(ServerContext ctx) {
        this.ctx = ctx;
    }

    /** Saves the assessment and its generated restoration job in one transaction. */
    public RestorationJob recordDamage(User user, int reportId, InfrastructureCategory category, DamageLevel level,
                                       double estimatedCost, int affectedPeople, String notes) throws SQLException {
        if (category == null || level == null) {
            throw new IllegalArgumentException("Choose an infrastructure category and damage level");
        }
        DamageAssessment a = new DamageAssessment(reportId, category, level, estimatedCost, affectedPeople,
                Validation.optionalText(notes, "Notes", 2000));
        a.setAssessedBy(user.getUsername());
        Connection c = ctx.connection();
        RestorationJob job;
        try {
            c.setAutoCommit(false);
            ReportDAO.Row row = ctx.reportDAO.findById(c, reportId, false);
            if (row == null) {
                throw new IllegalArgumentException("Report #" + reportId + " not found");
            }
            List<Department> departments = ctx.departmentDAO.findAllWithResources(c);
            Department dept = RestorationPlanner.responsibleDepartment(category, departments)
                    .orElseThrow(() -> new IllegalStateException("No department available to restore " + category));
            ctx.damageDAO.insertAssessment(c, a);
            job = RestorationPlanner.createJob(a, dept, row.report.getLocation());
            ctx.damageDAO.insertJob(c, job);
            c.commit();
        } catch (SQLException | RuntimeException e) {
            ServerContext.rollback(c);
            throw e;
        } finally {
            c.close();
        }
        ctx.audit().record(user.getUsername(), "RECORD_DAMAGE", "Assessment #" + a.getAssessmentId() + " "
                + category + " " + level + String.format(" $%,.0f", estimatedCost)
                + " -> job #" + job.getJobId() + " (" + job.getDepartmentName() + ")");
        return job;
    }

    public ArrayList<DamageAssessment> listAssessments(int reportId) throws SQLException {
        try (Connection c = ctx.connection()) {
            return new ArrayList<>(ctx.damageDAO.findAssessments(c, reportId));
        }
    }

    public ArrayList<RestorationJob> listJobs(int reportId) throws SQLException {
        try (Connection c = ctx.connection()) {
            return new ArrayList<>(ctx.damageDAO.findJobs(c, reportId));
        }
    }

    public RestorationJob updateProgress(User user, int jobId, int progress) throws SQLException {
        Connection c = ctx.connection();
        RestorationJob job;
        try {
            c.setAutoCommit(false);
            job = ctx.damageDAO.findJobForUpdate(c, jobId);
            if (job == null) {
                throw new IllegalArgumentException("Restoration job #" + jobId + " not found");
            }
            job.updateProgress(progress);
            ctx.damageDAO.updateJobProgress(c, job);
            c.commit();
        } catch (SQLException | RuntimeException e) {
            ServerContext.rollback(c);
            throw e;
        } finally {
            c.close();
        }
        ctx.audit().record(user.getUsername(), "UPDATE_JOB_PROGRESS", "Job #" + jobId + " -> " + progress + "% ("
                + job.getStatus() + ")");
        return job;
    }

    /** Recovery totals for one report ({@code reportId <= 0} = all reports). */
    public RecoverySummary summary(int reportId) throws SQLException {
        return RestorationPlanner.summarise(listAssessments(reportId), listJobs(reportId));
    }
}
