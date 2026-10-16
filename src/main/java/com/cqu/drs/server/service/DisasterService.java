package com.cqu.drs.server.service;

import com.cqu.drs.model.DashboardSummary;
import com.cqu.drs.model.Department;
import com.cqu.drs.model.DisasterReport;
import com.cqu.drs.model.DisasterType;
import com.cqu.drs.model.ResponseTask;
import com.cqu.drs.model.Role;
import com.cqu.drs.model.SeverityLevel;
import com.cqu.drs.model.Status;
import com.cqu.drs.model.User;
import com.cqu.drs.security.AccessControl;
import com.cqu.drs.security.CryptoService;
import com.cqu.drs.security.SignatureService;
import com.cqu.drs.server.db.ReportDAO;
import com.cqu.drs.util.CoordinationService;
import com.cqu.drs.util.PriorityCalculator;
import com.cqu.drs.util.Validation;

import java.security.PublicKey;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Reporting, assessment/prioritisation and resolution of disasters
 * (the core DRS-Initial use cases, now persistent, signed and access-controlled).
 */
public class DisasterService {

    private final ServerContext ctx;

    DisasterService(ServerContext ctx) {
        this.ctx = ctx;
    }

    /** Saves a new report, encrypting the phone number and signing the content as the reporting user. */
    public DisasterReport report(User user, DisasterType type, String location, String description,
                                 String reporterName, String reporterPhone) throws SQLException {
        if (type == null) {
            throw new IllegalArgumentException("Disaster type is required");
        }
        DisasterReport r = new DisasterReport(type,
                Validation.requireText(location, "Location", 150),
                Validation.requireText(description, "Description", 2000),
                Validation.requireText(reporterName, "Reporter name", 100),
                Validation.optionalPhone(reporterPhone));
        r.setReportedBy(user.getUsername());
        r.setSignature(ctx.auth().signAs(user.getUsername(), r.canonicalContent()));
        try (Connection c = ctx.connection()) {
            ctx.reportDAO.insert(c, r, ctx.getCrypto().encrypt(r.getReporterPhone()));
        }
        ctx.audit().record(user.getUsername(), "REPORT_DISASTER",
                "Report #" + r.getReportId() + " " + type + " at " + r.getLocation());
        r.setSignatureValid(true);
        return r;
    }

    /** Reports submitted by this user (own phone numbers shown in full). */
    public ArrayList<DisasterReport> listMine(User user) throws SQLException {
        try (Connection c = ctx.connection()) {
            return toReports(ctx.reportDAO.findByReporter(c, user.getUsername()), true);
        }
    }

    /** All reports, most urgent first; phone numbers decrypted only for coordinators/admins. */
    public ArrayList<DisasterReport> listAll(User viewer) throws SQLException {
        try (Connection c = ctx.connection()) {
            return toReports(ctx.reportDAO.findAll(c), AccessControl.canViewPersonalData(viewer.getRole()));
        }
    }

    /**
     * Assesses a report: sets severity, computes the priority score and (first time only)
     * creates response tasks for every relevant department - all in one transaction.
     */
    public DisasterReport assess(User user, int reportId, SeverityLevel severity) throws SQLException {
        if (severity == null) {
            throw new IllegalArgumentException("Select a severity level");
        }
        Connection c = ctx.connection();
        int tasksCreated = 0;
        DisasterReport report;
        try {
            c.setAutoCommit(false);
            ReportDAO.Row row = ctx.reportDAO.findById(c, reportId, true);
            if (row == null) {
                throw new IllegalArgumentException("Report #" + reportId + " not found");
            }
            report = row.report;
            if (report.getStatus() == Status.RESOLVED) {
                throw new IllegalStateException("Report #" + reportId + " is already resolved");
            }
            boolean firstAssessment = report.getSeverity() == null;
            report.setSeverity(severity);
            report.setPriorityScore(PriorityCalculator.calculate(report.getType(), severity));
            report.setStatus(Status.ASSESSED);
            if (firstAssessment) {
                List<Department> departments = ctx.departmentDAO.findAllWithResources(c);
                List<ResponseTask> tasks = CoordinationService.coordinateResponse(report, departments);
                for (ResponseTask t : tasks) {
                    ctx.taskDAO.insert(c, t);
                }
                tasksCreated = tasks.size();
            } else {
                report.setStatus(Status.IN_PROGRESS);
            }
            ctx.reportDAO.updateAssessment(c, report);
            c.commit();
        } catch (SQLException | RuntimeException e) {
            ServerContext.rollback(c);
            throw e;
        } finally {
            c.close();
        }
        ctx.audit().record(user.getUsername(), "ASSESS_REPORT", "Report #" + reportId + " severity " + severity
                + ", priority " + report.getPriorityScore() + ", " + tasksCreated + " task(s) created");
        report.setReporterPhone(null);
        return report;
    }

    /** Marks a report resolved once all of its response tasks are resolved. */
    public void resolve(User user, int reportId) throws SQLException {
        try (Connection c = ctx.connection()) {
            ReportDAO.Row row = ctx.reportDAO.findById(c, reportId, false);
            if (row == null) {
                throw new IllegalArgumentException("Report #" + reportId + " not found");
            }
            int open = ctx.taskDAO.countOpenForReport(c, reportId);
            if (open > 0) {
                throw new IllegalStateException("Report #" + reportId + " still has " + open
                        + " open response task(s) - resolve them on the Coordination screen first");
            }
            row.report.setStatus(Status.RESOLVED);
            ctx.reportDAO.updateAssessment(c, row.report);
        }
        ctx.audit().record(user.getUsername(), "RESOLVE_REPORT", "Report #" + reportId + " resolved");
    }

    /** Dashboard figures computed with SQL aggregates. */
    public DashboardSummary summary() throws SQLException {
        DashboardSummary s = new DashboardSummary();
        try (Connection c = ctx.connection()) {
            s.setOpenReports(count(c, "SELECT COUNT(*) FROM disaster_reports WHERE status <> 'RESOLVED'"));
            s.setCriticalReports(count(c, "SELECT COUNT(*) FROM disaster_reports WHERE severity = 'CRITICAL' AND status <> 'RESOLVED'"));
            s.setActiveTasks(count(c, "SELECT COUNT(*) FROM response_tasks WHERE status <> 'RESOLVED'"));
            s.setActiveAlerts(count(c, "SELECT COUNT(*) FROM evacuation_alerts WHERE status = 'ACTIVE'"));
            s.setPeopleSheltered(count(c, "SELECT COALESCE(SUM(occupancy),0) FROM shelters"));
            s.setShelterCapacity(count(c, "SELECT COALESCE(SUM(capacity),0) FROM shelters WHERE closed = FALSE"));
            s.setLowStockResources(count(c, "SELECT COUNT(*) FROM resources WHERE quantity_available <= reorder_level"));
            s.setRestorationJobsOpen(count(c, "SELECT COUNT(*) FROM restoration_jobs WHERE status <> 'COMPLETED'"));
            try (PreparedStatement ps = c.prepareStatement("SELECT COALESCE(SUM(estimated_cost),0) FROM damage_assessments");
                 ResultSet rs = ps.executeQuery()) {
                rs.next();
                s.setTotalDamageCost(rs.getDouble(1));
            }
        }
        return s;
    }

    private int count(Connection c, String sql) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(sql); ResultSet rs = ps.executeQuery()) {
            rs.next();
            return rs.getInt(1);
        }
    }

    private ArrayList<DisasterReport> toReports(List<ReportDAO.Row> rows, boolean showPhone) throws SQLException {
        ArrayList<DisasterReport> list = new ArrayList<>();
        CryptoService crypto = ctx.getCrypto();
        for (ReportDAO.Row row : rows) {
            DisasterReport r = row.report;
            String phone;
            try {
                phone = crypto.decrypt(row.phoneEncrypted);
            } catch (SecurityException e) {
                phone = "[unreadable]";
            }
            r.setReporterPhone(showPhone ? phone : CryptoService.mask(phone));
            if (r.isSigned() && r.getReportedBy() != null) {
                PublicKey key = ctx.auth().publicKeyOf(r.getReportedBy());
                r.setSignatureValid(SignatureService.verify(r.canonicalContent(), r.getSignature(), key));
            }
            list.add(r);
        }
        return list;
    }

    /** True when the role may assess reports (used by the dispatcher's error messages). */
    public static boolean canAssess(Role role) {
        return role != null && role.atLeast(Role.COORDINATOR);
    }
}
