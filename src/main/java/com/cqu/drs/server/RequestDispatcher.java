package com.cqu.drs.server;

import com.cqu.drs.model.AlertLevel;
import com.cqu.drs.model.DamageLevel;
import com.cqu.drs.model.DisasterReport;
import com.cqu.drs.model.DisasterType;
import com.cqu.drs.model.EvacuationAlert;
import com.cqu.drs.model.InfrastructureCategory;
import com.cqu.drs.model.Resource;
import com.cqu.drs.model.RestorationJob;
import com.cqu.drs.model.Role;
import com.cqu.drs.model.SeverityLevel;
import com.cqu.drs.model.Shelter;
import com.cqu.drs.model.Status;
import com.cqu.drs.model.User;
import com.cqu.drs.model.Volunteer;
import com.cqu.drs.protocol.Command;
import com.cqu.drs.protocol.Request;
import com.cqu.drs.protocol.Response;
import com.cqu.drs.security.AccessControl;
import com.cqu.drs.server.service.ServerContext;

import java.io.Serializable;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Server-side controller: authenticates the session, enforces role-based
 * access control, routes each {@link Command} to the business service and
 * converts the outcome (or exception) into a {@link Response}. Stateless, so
 * one instance is shared safely by every client-handler thread.
 */
public class RequestDispatcher {

    private final ServerContext ctx;
    private final SessionManager sessions;

    public RequestDispatcher(ServerContext ctx, SessionManager sessions) {
        this.ctx = ctx;
        this.sessions = sessions;
    }

    public Response handle(Request req) {
        if (req == null || req.getCommand() == null) {
            return Response.error("Malformed request");
        }
        Command cmd = req.getCommand();
        User user = null;
        if (!AccessControl.isPublic(cmd)) {
            SessionManager.Session s = sessions.get(req.getSessionToken());
            if (s == null) {
                return Response.error("Your session has expired - please log in again");
            }
            user = s.getUser();
            if (!AccessControl.isAllowed(user.getRole(), cmd)) {
                ctx.audit().record(user.getUsername(), "ACCESS_DENIED", cmd.name());
                return Response.error("Access denied: " + user.getRole() + " cannot perform " + cmd);
            }
        }
        try {
            return execute(cmd, req, user);
        } catch (SecurityException | IllegalArgumentException | IllegalStateException e) {
            return Response.error(e.getMessage());
        } catch (SQLException e) {
            System.err.println("[DB] " + cmd + " failed: " + e.getMessage());
            return Response.error("Database error: " + e.getMessage());
        } catch (RuntimeException e) {
            e.printStackTrace();
            return Response.error("Server error: " + e);
        }
    }

    @SuppressWarnings("unchecked")
    private Response execute(Command cmd, Request req, User user) throws SQLException {
        switch (cmd) {
            case PING:
                return Response.ok("pong");
            case LOGIN: {
                User u = ctx.auth().login(req.getString("username"), req.getString("password"));
                String token = sessions.create(u);
                ctx.audit().record(u.getUsername(), "LOGIN", "Signed in as " + u.getRole());
                HashMap<String, Serializable> data = new HashMap<>();
                data.put("token", token);
                data.put("user", u);
                return Response.ok("Welcome " + u.getFullName(), data);
            }
            case REGISTER_PUBLIC_USER: {
                User u = ctx.auth().createUser(req.getString("username"), req.getString("fullName"),
                        req.getString("password"), Role.REPORTER);
                ctx.audit().record(u.getUsername(), "REGISTER", "Self-registered public reporter account");
                return Response.ok("Account '" + u.getUsername() + "' created - you can now log in", u);
            }
            case LOGOUT:
                sessions.remove(req.getSessionToken());
                ctx.audit().record(user.getUsername(), "LOGOUT", "Signed out");
                return Response.ok("Logged out");

            case REPORT_DISASTER: {
                DisasterReport r = ctx.disasters().report(user, (DisasterType) req.get("type"),
                        req.getString("location"), req.getString("description"),
                        req.getString("reporterName"), req.getString("reporterPhone"));
                return Response.ok("Report #" + r.getReportId() + " submitted and digitally signed", r);
            }
            case LIST_MY_REPORTS:
                return Response.ok("OK", ctx.disasters().listMine(user));
            case LIST_REPORTS:
                return Response.ok("OK", ctx.disasters().listAll(user));
            case ASSESS_REPORT: {
                DisasterReport r = ctx.disasters().assess(user, req.getInt("reportId"), (SeverityLevel) req.get("severity"));
                return Response.ok("Report #" + r.getReportId() + " assessed - priority " + r.getPriorityScore()
                        + ". Response tasks created for the relevant departments.", r);
            }
            case RESOLVE_REPORT:
                ctx.disasters().resolve(user, req.getInt("reportId"));
                return Response.ok("Report #" + req.getInt("reportId") + " resolved");
            case DASHBOARD_SUMMARY:
                return Response.ok("OK", ctx.disasters().summary());

            case LIST_TASKS:
                return Response.ok("OK", ctx.coordination().listTasks());
            case UPDATE_TASK_STATUS:
                ctx.coordination().updateTaskStatus(user, req.getInt("taskId"), (Status) req.get("status"));
                return Response.ok("Task #" + req.getInt("taskId") + " is now " + req.get("status"));
            case ASSIGN_VOLUNTEER: {
                var t = ctx.coordination().assignVolunteer(user, req.getInt("taskId"), req.getInt("volunteerId"));
                return Response.ok(t.getVolunteerName() + " assigned to task #" + t.getTaskId(), t);
            }
            case LIST_DEPARTMENTS:
                return Response.ok("OK", ctx.coordination().listDepartments());
            case LIST_RESOURCES:
                return Response.ok("OK", ctx.coordination().listResources());
            case ALLOCATE_RESOURCE: {
                int qty = req.getInt("quantity");
                Resource r = ctx.coordination().allocateResource(user, req.getInt("taskId"), req.getInt("resourceId"), qty);
                String warn = r.isLowStock() ? " WARNING: " + r.getName() + " is now LOW on stock ("
                        + r.getQuantityAvailable() + " left)." : "";
                return Response.ok(qty + " x " + r.getName() + " allocated to task #" + req.getInt("taskId") + "." + warn, r);
            }
            case RESTOCK_RESOURCE: {
                Resource r = ctx.coordination().restock(user, req.getInt("resourceId"), req.getInt("quantity"));
                return Response.ok(r.getName() + " restocked - now " + r.getQuantityAvailable(), r);
            }
            case REGISTER_VOLUNTEER: {
                Volunteer v = ctx.coordination().registerVolunteer(user, req.getString("name"),
                        req.getString("phone"), req.getString("skill"));
                return Response.ok("Volunteer '" + v.getName() + "' registered (ID " + v.getVolunteerId() + ")", v);
            }
            case LIST_VOLUNTEERS:
                return Response.ok("OK", ctx.coordination().listVolunteers(user));

            case LIST_SHELTERS:
                return Response.ok("OK", ctx.evacuation().listShelters());
            case ADD_SHELTER: {
                Shelter s = ctx.evacuation().addShelter(user, req.getString("name"), req.getString("location"),
                        req.getInt("capacity"), req.getString("contact"));
                return Response.ok("Shelter '" + s.getName() + "' added", s);
            }
            case CHECK_IN_EVACUEES:
            case CHECK_OUT_EVACUEES: {
                boolean in = cmd == Command.CHECK_IN_EVACUEES;
                Shelter s = ctx.evacuation().changeOccupancy(user, req.getInt("shelterId"), req.getInt("people"), in);
                return Response.ok(req.getInt("people") + " evacuee(s) checked " + (in ? "in to " : "out of ")
                        + s.getName() + " - " + s.getOccupancy() + "/" + s.getCapacity() + " (" + s.getStatus() + ")", s);
            }
            case RECOMMEND_SHELTER: {
                Shelter s = ctx.evacuation().recommend(req.getString("zone"), req.getInt("people"));
                return Response.ok("Recommended: " + s.getName() + " (" + s.getFreeSpaces() + " free places)", s);
            }
            case ISSUE_ALERT: {
                Serializable sid = req.get("shelterId");
                EvacuationAlert a = ctx.evacuation().issueAlert(user, req.getInt("reportId"), req.getString("zone"),
                        (AlertLevel) req.get("level"), sid == null ? null : ((Number) sid).intValue(),
                        req.getString("message"));
                return Response.ok(a.getLevel() + " issued for " + a.getZone() + " (alert #" + a.getAlertId() + ")", a);
            }
            case LIFT_ALERT: {
                EvacuationAlert a = ctx.evacuation().liftAlert(user, req.getInt("alertId"));
                return Response.ok("Alert #" + a.getAlertId() + " lifted", a);
            }
            case LIST_ALERTS:
                return Response.ok("OK", ctx.evacuation().listAlerts(user.getRole() == Role.REPORTER));

            case RECORD_DAMAGE: {
                RestorationJob j = ctx.damage().recordDamage(user, req.getInt("reportId"),
                        (InfrastructureCategory) req.get("category"), (DamageLevel) req.get("damageLevel"),
                        req.getDouble("estimatedCost"), req.getInt("affectedPeople"), req.getString("notes"));
                return Response.ok("Damage recorded. Restoration job #" + j.getJobId() + " (priority " + j.getPriority()
                        + ", ~" + j.getEstimatedDays() + " days) assigned to " + j.getDepartmentName(), j);
            }
            case LIST_DAMAGE:
                return Response.ok("OK", ctx.damage().listAssessments(optionalInt(req, "reportId")));
            case LIST_RESTORATION_JOBS:
                return Response.ok("OK", ctx.damage().listJobs(optionalInt(req, "reportId")));
            case UPDATE_JOB_PROGRESS: {
                RestorationJob j = ctx.damage().updateProgress(user, req.getInt("jobId"), req.getInt("progress"));
                return Response.ok("Job #" + j.getJobId() + " is " + j.getProgress() + "% (" + j.getStatus() + ")", j);
            }
            case RECOVERY_SUMMARY:
                return Response.ok("OK", ctx.damage().summary(optionalInt(req, "reportId")));

            case LIST_USERS:
                return Response.ok("OK", new java.util.ArrayList<>(ctx.auth().listUsers()));
            case CREATE_USER: {
                User u = ctx.auth().createUser(req.getString("username"), req.getString("fullName"),
                        req.getString("password"), (Role) req.get("role"));
                ctx.audit().record(user.getUsername(), "CREATE_USER", u.getUsername() + " as " + u.getRole());
                return Response.ok("User '" + u.getUsername() + "' created with a new RSA signing key", u);
            }
            case SET_USER_ACTIVE: {
                boolean active = Boolean.TRUE.equals(req.get("active"));
                int id = req.getInt("userId");
                if (id == user.getUserId() && !active) {
                    throw new IllegalArgumentException("You cannot disable your own account");
                }
                ctx.auth().setActive(id, active);
                ctx.audit().record(user.getUsername(), "SET_USER_ACTIVE", "User #" + id + " active=" + active);
                return Response.ok("User #" + id + (active ? " enabled/unlocked" : " disabled"));
            }
            case LIST_AUDIT_LOG:
                return Response.ok("OK", new java.util.ArrayList<>(ctx.audit().list()));
            case VERIFY_AUDIT_LOG: {
                var entries = new java.util.ArrayList<>(ctx.audit().verify());
                long bad = entries.stream().filter(e -> !Boolean.TRUE.equals(e.getVerified())).count();
                return Response.ok(bad == 0 ? "All " + entries.size() + " audit entries verified: hash chain intact, signatures valid"
                        : bad + " of " + entries.size() + " audit entries FAILED verification - possible tampering", entries);
            }
            case IMPORT_LEGACY_DATA: {
                Map<String, List<String[]>> data = (Map<String, List<String[]>>) req.get("data");
                if (data == null) {
                    throw new IllegalArgumentException("No data supplied");
                }
                return Response.ok(ctx.importer().importLegacy(user, data));
            }
            default:
                return Response.error("Unsupported command " + cmd);
        }
    }

    private int optionalInt(Request req, String key) {
        return req.get(key) == null ? 0 : req.getInt(key);
    }
}
