package com.cqu.drs.server;

import com.cqu.drs.client.LegacyDataReader;
import com.cqu.drs.client.ServerConnection;
import com.cqu.drs.model.AlertLevel;
import com.cqu.drs.model.AuditEntry;
import com.cqu.drs.model.DamageLevel;
import com.cqu.drs.model.DashboardSummary;
import com.cqu.drs.model.DisasterReport;
import com.cqu.drs.model.DisasterType;
import com.cqu.drs.model.EvacuationAlert;
import com.cqu.drs.model.InfrastructureCategory;
import com.cqu.drs.model.JobStatus;
import com.cqu.drs.model.RecoverySummary;
import com.cqu.drs.model.Resource;
import com.cqu.drs.model.ResponseTask;
import com.cqu.drs.model.SeverityLevel;
import com.cqu.drs.model.Shelter;
import com.cqu.drs.model.Status;
import com.cqu.drs.model.User;
import com.cqu.drs.model.Volunteer;
import com.cqu.drs.protocol.Command;
import com.cqu.drs.protocol.Request;
import com.cqu.drs.protocol.Response;
import com.cqu.drs.security.CryptoService;
import com.cqu.drs.server.db.DatabaseManager;
import com.cqu.drs.server.service.ServerContext;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import java.io.IOException;
import java.io.Serializable;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.junit.jupiter.api.Assertions.*;

/**
 * End-to-end tests: real multi-threaded server + real MySQL database
 * ({@code drs_test}, recreated for every run) + real socket clients.
 * Skipped automatically when MySQL is not reachable with the credentials in
 * drs.properties.
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class ServerIntegrationTest {

    private static DatabaseManager db;
    private static DRSServer server;
    private static ServerConnection admin;
    private static ServerConnection coord;
    private static ServerConnection officer;
    private static ServerConnection pub;

    private static int fireReportId;
    private static int shelterId;

    @BeforeAll
    static void startServer() throws Exception {
        ServerConfig cfg = new ServerConfig();
        db = new DatabaseManager(cfg.getDbHost(), cfg.getDbPort(), "drs_test", cfg.getDbUser(), cfg.getDbPassword());
        try {
            db.initialise();
        } catch (SQLException e) {
            Assumptions.assumeTrue(false, "MySQL not available - integration tests skipped: " + e.getMessage());
        }
        db.dropAllTables();
        db.initialise();
        ServerContext ctx = new ServerContext(db, CryptoService.fromBase64Key(CryptoService.generateBase64Key()));
        new DataSeeder(ctx).seedIfEmpty();
        server = new DRSServer(0, 10, new RequestDispatcher(ctx, new SessionManager(30)));
        server.start();
        admin = login("admin", "Admin@123");
        coord = login("coord", "Coord@123");
        officer = login("officer", "Officer@123");
        pub = login("public", "Public@123");
    }

    @AfterAll
    static void stopServer() {
        for (ServerConnection c : new ServerConnection[]{admin, coord, officer, pub}) {
            if (c != null) {
                c.close();
            }
        }
        if (server != null) {
            server.stop();
        }
    }

    private static ServerConnection connect() throws IOException {
        ServerConnection c = new ServerConnection("localhost", server.getPort());
        c.connect();
        return c;
    }

    @SuppressWarnings("unchecked")
    private static ServerConnection login(String user, String pass) throws IOException {
        ServerConnection c = connect();
        Response r = c.send(new Request(Command.LOGIN).with("username", user).with("password", pass));
        assertTrue(r.isSuccess(), r.getMessage());
        c.setSessionToken((String) ((Map<String, Object>) r.getData()).get("token"));
        return c;
    }

    private static Response ok(ServerConnection c, Request r) throws IOException {
        Response resp = c.send(r);
        assertTrue(resp.isSuccess(), r.getCommand() + " failed: " + resp.getMessage());
        return resp;
    }

    @SuppressWarnings("unchecked")
    private static <T> List<T> list(ServerConnection c, Command cmd) throws IOException {
        return (List<T>) ok(c, new Request(cmd)).getData();
    }

    // ------------------------------------------------------------------ tests

    @Test
    @Order(1)
    @DisplayName("IT-01 database and all 12 tables are created programmatically")
    void schemaCreated() throws SQLException {
        try (Connection c = db.getConnection(); Statement st = c.createStatement();
             ResultSet rs = st.executeQuery("SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = 'drs_test'")) {
            rs.next();
            assertEquals(12, rs.getInt(1));
        }
    }

    @Test
    @Order(2)
    @DisplayName("IT-02 wrong password is rejected; 5 failures lock the account")
    void loginFailuresAndLockout() throws Exception {
        ServerConnection c = connect();
        Response r = c.send(new Request(Command.LOGIN).with("username", "coord").with("password", "wrong"));
        assertFalse(r.isSuccess());
        assertTrue(r.getMessage().contains("Invalid"));
        ok(admin, new Request(Command.CREATE_USER).with("username", "victim").with("fullName", "Lock Test")
                .with("password", "Victim#123").with("role", com.cqu.drs.model.Role.FIELD_OFFICER));
        for (int i = 0; i < 5; i++) {
            c.send(new Request(Command.LOGIN).with("username", "victim").with("password", "nope"));
        }
        Response locked = c.send(new Request(Command.LOGIN).with("username", "victim").with("password", "Victim#123"));
        assertFalse(locked.isSuccess());
        assertTrue(locked.getMessage().contains("locked"));
        c.close();
    }

    @Test
    @Order(3)
    @DisplayName("IT-03 requests without a valid session are refused")
    void noSession() throws Exception {
        ServerConnection c = connect();
        Response r = c.send(new Request(Command.LIST_REPORTS));
        assertFalse(r.isSuccess());
        assertTrue(r.getMessage().contains("session"));
        c.close();
    }

    @Test
    @Order(4)
    @DisplayName("IT-04 DRS-Initial data is imported through the server into MySQL (idempotent)")
    void importLegacyData() throws Exception {
        HashMap<String, ArrayList<String[]>> payload = new HashMap<>();
        LegacyDataReader.readBundled().forEach((k, v) -> payload.put(k, new ArrayList<>(v)));
        Response r = ok(admin, new Request(Command.IMPORT_LEGACY_DATA).with("data", (Serializable) payload));
        assertTrue(r.getMessage().contains("5 report(s)"), r.getMessage());
        Response again = ok(admin, new Request(Command.IMPORT_LEGACY_DATA).with("data", (Serializable) payload));
        assertTrue(again.getMessage().contains("0 report(s)"), again.getMessage());
        assertEquals(5, this.<DisasterReport>list(coord, Command.LIST_REPORTS).size());
        assertEquals(6, this.<Volunteer>list(coord, Command.LIST_VOLUNTEERS).size());
        assertTrue(this.<ResponseTask>list(coord, Command.LIST_TASKS).size() >= 15);
    }

    @Test
    @Order(5)
    @DisplayName("IT-05 public user reports a disaster; it is signed and the phone is encrypted at rest")
    void reportDisaster() throws Exception {
        Response r = ok(pub, new Request(Command.REPORT_DISASTER).with("type", DisasterType.FIRE)
                .with("location", "Emu Park").with("description", "Grass fire near school")
                .with("reporterName", "Olivia Public").with("reporterPhone", "0499 888 777"));
        DisasterReport rep = (DisasterReport) r.getData();
        fireReportId = rep.getReportId();
        assertTrue(fireReportId > 0, "MySQL generated an id");
        assertEquals(Status.REPORTED, rep.getStatus());
        try (Connection c = db.getConnection(); Statement st = c.createStatement();
             ResultSet rs = st.executeQuery("SELECT reporter_phone_enc, signature FROM disaster_reports WHERE report_id = " + fireReportId)) {
            rs.next();
            assertFalse(rs.getString(1).contains("0499"), "phone must be encrypted in MySQL");
            assertNotNull(rs.getString(2));
        }
        List<DisasterReport> mine = list(pub, Command.LIST_MY_REPORTS);
        assertEquals("0499 888 777", mine.get(0).getReporterPhone());
        DisasterReport seenByCoord = this.<DisasterReport>list(coord, Command.LIST_REPORTS).stream()
                .filter(x -> x.getReportId() == fireReportId).findFirst().orElseThrow();
        assertTrue(seenByCoord.getSignatureValid());
        DisasterReport seenByOfficer = this.<DisasterReport>list(officer, Command.LIST_REPORTS).stream()
                .filter(x -> x.getReportId() == fireReportId).findFirst().orElseThrow();
        assertEquals("04******77", seenByOfficer.getReporterPhone(), "PII masked for field officers");
    }

    @Test
    @Order(6)
    @DisplayName("IT-06 invalid report input is rejected by the server")
    void invalidReport() throws Exception {
        Response r = pub.send(new Request(Command.REPORT_DISASTER).with("type", DisasterType.FLOOD)
                .with("location", " ").with("description", "x").with("reporterName", "A"));
        assertFalse(r.isSuccess());
        assertTrue(r.getMessage().contains("Location"));
    }

    @Test
    @Order(7)
    @DisplayName("IT-07 a public reporter cannot assess reports (access denied)")
    void accessDenied() throws Exception {
        Response r = pub.send(new Request(Command.ASSESS_REPORT).with("reportId", fireReportId)
                .with("severity", SeverityLevel.HIGH));
        assertFalse(r.isSuccess());
        assertTrue(r.getMessage().startsWith("Access denied"));
    }

    @Test
    @Order(8)
    @DisplayName("IT-08 coordinator assesses report: priority 45 and 3 department tasks")
    void assess() throws Exception {
        DisasterReport r = (DisasterReport) ok(coord, new Request(Command.ASSESS_REPORT)
                .with("reportId", fireReportId).with("severity", SeverityLevel.HIGH)).getData();
        assertEquals(45, r.getPriorityScore());
        long tasks = this.<ResponseTask>list(coord, Command.LIST_TASKS).stream()
                .filter(t -> t.getDisasterReport().getReportId() == fireReportId).count();
        assertEquals(3, tasks);
        Response resolve = coord.send(new Request(Command.RESOLVE_REPORT).with("reportId", fireReportId));
        assertFalse(resolve.isSuccess(), "cannot resolve while tasks are open");
    }

    @Test
    @Order(9)
    @DisplayName("IT-09 volunteer assignment and resource allocation with stock rules")
    void coordination() throws Exception {
        ResponseTask fireTask = this.<ResponseTask>list(coord, Command.LIST_TASKS).stream()
                .filter(t -> t.getDisasterReport().getReportId() == fireReportId
                        && t.getDepartmentName().equals("Fire & Emergency")).findFirst().orElseThrow();
        Volunteer v = this.<Volunteer>list(coord, Command.LIST_VOLUNTEERS).get(0);
        ok(coord, new Request(Command.ASSIGN_VOLUNTEER).with("taskId", fireTask.getTaskId()).with("volunteerId", v.getVolunteerId()));
        Response twice = coord.send(new Request(Command.ASSIGN_VOLUNTEER).with("taskId", fireTask.getTaskId())
                .with("volunteerId", v.getVolunteerId()));
        assertFalse(twice.isSuccess());

        Resource ext = this.<Resource>list(coord, Command.LIST_RESOURCES).stream()
                .filter(r -> r.getName().equals("Fire extinguishers")).findFirst().orElseThrow();
        Response tooMany = coord.send(new Request(Command.ALLOCATE_RESOURCE).with("taskId", fireTask.getTaskId())
                .with("resourceId", ext.getResourceId()).with("quantity", 999));
        assertFalse(tooMany.isSuccess());
        Response low = ok(coord, new Request(Command.ALLOCATE_RESOURCE).with("taskId", fireTask.getTaskId())
                .with("resourceId", ext.getResourceId()).with("quantity", 35));
        assertTrue(low.getMessage().contains("LOW"), low.getMessage());

        Resource water = this.<Resource>list(coord, Command.LIST_RESOURCES).stream()
                .filter(r -> r.getName().startsWith("Bottled water")).findFirst().orElseThrow();
        Response wrongDept = coord.send(new Request(Command.ALLOCATE_RESOURCE).with("taskId", fireTask.getTaskId())
                .with("resourceId", water.getResourceId()).with("quantity", 1));
        assertFalse(wrongDept.isSuccess(), "resource must belong to the task's department");

        ok(coord, new Request(Command.UPDATE_TASK_STATUS).with("taskId", fireTask.getTaskId()).with("status", Status.RESOLVED));
        assertTrue(this.<Volunteer>list(coord, Command.LIST_VOLUNTEERS).stream()
                .filter(x -> x.getVolunteerId() == v.getVolunteerId()).findFirst().orElseThrow().isAvailable(),
                "volunteer released when the task is resolved");
    }

    @Test
    @Order(10)
    @DisplayName("IT-10 NEW FEATURE 1: issue and lift an evacuation warning linked to a shelter")
    void evacuationWarnings() throws Exception {
        Shelter yeppoon = this.<Shelter>list(coord, Command.LIST_SHELTERS).stream()
                .filter(s -> s.getLocation().equals("Yeppoon")).findFirst().orElseThrow();
        shelterId = yeppoon.getShelterId();
        DisasterReport quake = this.<DisasterReport>list(coord, Command.LIST_REPORTS).stream()
                .filter(r -> r.getType() == DisasterType.EARTHQUAKE).findFirst().orElseThrow();
        EvacuationAlert a = (EvacuationAlert) ok(coord, new Request(Command.ISSUE_ALERT)
                .with("reportId", quake.getReportId()).with("zone", "Yeppoon Esplanade")
                .with("shelterId", shelterId)).getData();
        assertEquals(AlertLevel.EMERGENCY_WARNING, a.getLevel(), "CRITICAL severity -> Emergency Warning");
        assertTrue(a.getMessage().contains("Yeppoon Community Hall"));
        assertFalse(officer.send(new Request(Command.ISSUE_ALERT).with("reportId", quake.getReportId())
                .with("zone", "x")).isSuccess(), "officers cannot issue warnings");
        assertEquals(1, this.<EvacuationAlert>list(pub, Command.LIST_ALERTS).size(), "public sees active warning");
        ok(coord, new Request(Command.LIFT_ALERT).with("alertId", a.getAlertId()));
        assertEquals(0, this.<EvacuationAlert>list(pub, Command.LIST_ALERTS).size());
        assertFalse(coord.send(new Request(Command.LIFT_ALERT).with("alertId", a.getAlertId())).isSuccess());
    }

    @Test
    @Order(11)
    @DisplayName("IT-11 NEW FEATURE 1: concurrent check-ins from many clients never overfill a shelter")
    void concurrentCheckIns() throws Exception {
        // Yeppoon hall capacity 150: 20 clients each try to check in 10 people at the same time
        ExecutorService pool = Executors.newFixedThreadPool(20);
        List<Callable<Boolean>> jobs = new ArrayList<>();
        for (int i = 0; i < 20; i++) {
            jobs.add(() -> {
                ServerConnection c = login("officer", "Officer@123");
                try {
                    return c.send(new Request(Command.CHECK_IN_EVACUEES).with("shelterId", shelterId)
                            .with("people", 10)).isSuccess();
                } finally {
                    c.close();
                }
            });
        }
        int succeeded = 0;
        for (Future<Boolean> f : pool.invokeAll(jobs)) {
            if (f.get()) {
                succeeded++;
            }
        }
        pool.shutdown();
        assertEquals(15, succeeded, "exactly 150/10 check-ins fit");
        Shelter s = this.<Shelter>list(officer, Command.LIST_SHELTERS).stream()
                .filter(x -> x.getShelterId() == shelterId).findFirst().orElseThrow();
        assertEquals(150, s.getOccupancy());
        assertEquals("FULL", s.getStatus());
        Shelter rec = (Shelter) ok(officer, new Request(Command.RECOMMEND_SHELTER).with("zone", "Yeppoon")
                .with("people", 10)).getData();
        assertNotEquals(shelterId, rec.getShelterId(), "full shelter is not recommended");
    }

    @Test
    @Order(12)
    @DisplayName("IT-12 NEW FEATURE 2: damage assessment raises a restoration job for the right department")
    void damageAndRestoration() throws Exception {
        DisasterReport cyclone = this.<DisasterReport>list(officer, Command.LIST_REPORTS).stream()
                .filter(r -> r.getType() == DisasterType.HURRICANE).findFirst().orElseThrow();
        Response r = ok(officer, new Request(Command.RECORD_DAMAGE).with("reportId", cyclone.getReportId())
                .with("category", InfrastructureCategory.POWER).with("damageLevel", DamageLevel.MAJOR)
                .with("estimatedCost", 1_200_000.0).with("affectedPeople", 4000).with("notes", "Lines down"));
        com.cqu.drs.model.RestorationJob job = (com.cqu.drs.model.RestorationJob) r.getData();
        assertEquals("Ergon Energy", job.getDepartmentName());
        assertEquals(80, job.getPriority());
        assertEquals(25, job.getEstimatedDays());
        ok(officer, new Request(Command.RECORD_DAMAGE).with("reportId", cyclone.getReportId())
                .with("category", InfrastructureCategory.DEBRIS_AND_WASTE).with("damageLevel", DamageLevel.MODERATE)
                .with("estimatedCost", 300_000.0).with("affectedPeople", 500));
        ok(officer, new Request(Command.UPDATE_JOB_PROGRESS).with("jobId", job.getJobId()).with("progress", 100));
        assertFalse(officer.send(new Request(Command.UPDATE_JOB_PROGRESS).with("jobId", job.getJobId())
                .with("progress", 50)).isSuccess(), "progress cannot go backwards");
        RecoverySummary s = (RecoverySummary) ok(officer, new Request(Command.RECOVERY_SUMMARY)
                .with("reportId", cyclone.getReportId())).getData();
        assertEquals(1_500_000, s.getTotalEstimatedCost(), 0.01);
        assertEquals(4500, s.getTotalAffectedPeople());
        assertEquals(1, s.getCompletedJobs());
        assertEquals(2, s.getTotalJobs());
        List<com.cqu.drs.model.RestorationJob> jobs = list(officer, Command.LIST_RESTORATION_JOBS);
        assertEquals(JobStatus.COMPLETED, jobs.stream().filter(j -> j.getJobId() == job.getJobId())
                .findFirst().orElseThrow().getStatus());
        Response invalid = officer.send(new Request(Command.RECORD_DAMAGE).with("reportId", cyclone.getReportId())
                .with("category", InfrastructureCategory.POWER).with("damageLevel", DamageLevel.MINOR)
                .with("estimatedCost", -5.0).with("affectedPeople", 0));
        assertFalse(invalid.isSuccess());
    }

    @Test
    @Order(13)
    @DisplayName("IT-13 dashboard summary aggregates the database")
    void dashboard() throws Exception {
        DashboardSummary s = (DashboardSummary) ok(coord, new Request(Command.DASHBOARD_SUMMARY)).getData();
        assertEquals(6, s.getOpenReports());
        assertEquals(150, s.getPeopleSheltered());
        assertEquals(1_500_000, s.getTotalDamageCost(), 0.01);
        assertTrue(s.getLowStockResources() >= 1);
    }

    @Test
    @Order(14)
    @DisplayName("IT-14 audit log is signed and hash-chained; tampering is detected")
    void auditNonRepudiation() throws Exception {
        @SuppressWarnings("unchecked")
        List<AuditEntry> verified = (List<AuditEntry>) ok(admin, new Request(Command.VERIFY_AUDIT_LOG)).getData();
        assertTrue(verified.size() > 20);
        assertTrue(verified.stream().allMatch(e -> Boolean.TRUE.equals(e.getVerified())));
        assertFalse(coord.send(new Request(Command.LIST_AUDIT_LOG)).isSuccess(), "only admins read the audit log");
        try (Connection c = db.getConnection(); Statement st = c.createStatement()) {
            st.executeUpdate("UPDATE audit_log SET details = 'forged' WHERE action = 'ASSESS_REPORT' LIMIT 1");
        }
        Response after = ok(admin, new Request(Command.VERIFY_AUDIT_LOG));
        assertTrue(after.getMessage().contains("FAILED"), after.getMessage());
    }

    @Test
    @Order(15)
    @DisplayName("IT-15 users created by admin get a role; duplicates and weak passwords rejected")
    void userManagement() throws Exception {
        ok(admin, new Request(Command.CREATE_USER).with("username", "newofficer").with("fullName", "New Officer")
                .with("password", "Field#2026").with("role", com.cqu.drs.model.Role.FIELD_OFFICER));
        assertFalse(admin.send(new Request(Command.CREATE_USER).with("username", "newofficer").with("fullName", "X")
                .with("password", "Field#2026").with("role", com.cqu.drs.model.Role.FIELD_OFFICER)).isSuccess());
        assertFalse(admin.send(new Request(Command.CREATE_USER).with("username", "weak").with("fullName", "X")
                .with("password", "abc").with("role", com.cqu.drs.model.Role.FIELD_OFFICER)).isSuccess());
        List<User> users = list(admin, Command.LIST_USERS);
        assertTrue(users.stream().anyMatch(u -> u.getUsername().equals("newofficer")));
        ServerConnection c = login("newofficer", "Field#2026");
        assertTrue(c.send(new Request(Command.LIST_REPORTS)).isSuccess());
        c.close();
    }
}
