package com.cqu.drs.server.service;

import com.cqu.drs.security.CryptoService;
import com.cqu.drs.server.db.AuditDAO;
import com.cqu.drs.server.db.DamageDAO;
import com.cqu.drs.server.db.DatabaseManager;
import com.cqu.drs.server.db.DepartmentDAO;
import com.cqu.drs.server.db.ReportDAO;
import com.cqu.drs.server.db.ShelterDAO;
import com.cqu.drs.server.db.TaskDAO;
import com.cqu.drs.server.db.UserDAO;
import com.cqu.drs.server.db.VolunteerDAO;

import java.sql.Connection;
import java.sql.SQLException;

/**
 * Wires together the database, the encryption service, the DAOs and the
 * business services so they can be shared by all client-handler threads.
 * All members are stateless or thread-safe.
 */
public class ServerContext {

    private final DatabaseManager db;
    private final CryptoService crypto;

    final UserDAO userDAO = new UserDAO();
    final ReportDAO reportDAO = new ReportDAO();
    final DepartmentDAO departmentDAO = new DepartmentDAO();
    final TaskDAO taskDAO = new TaskDAO();
    final VolunteerDAO volunteerDAO = new VolunteerDAO();
    final ShelterDAO shelterDAO = new ShelterDAO();
    final DamageDAO damageDAO = new DamageDAO();
    final AuditDAO auditDAO = new AuditDAO();

    private final AuthService authService;
    private final AuditService auditService;
    private final DisasterService disasterService;
    private final CoordinationManager coordinationManager;
    private final EvacuationService evacuationService;
    private final DamageService damageService;
    private final ImportService importService;

    public ServerContext(DatabaseManager db, CryptoService crypto) {
        this.db = db;
        this.crypto = crypto;
        this.authService = new AuthService(this);
        this.auditService = new AuditService(this);
        this.disasterService = new DisasterService(this);
        this.coordinationManager = new CoordinationManager(this);
        this.evacuationService = new EvacuationService(this);
        this.damageService = new DamageService(this);
        this.importService = new ImportService(this);
    }

    public Connection connection() throws SQLException {
        return db.getConnection();
    }

    public DatabaseManager getDb() {
        return db;
    }

    public CryptoService getCrypto() {
        return crypto;
    }

    public AuthService auth() {
        return authService;
    }

    public AuditService audit() {
        return auditService;
    }

    public DisasterService disasters() {
        return disasterService;
    }

    public CoordinationManager coordination() {
        return coordinationManager;
    }

    public EvacuationService evacuation() {
        return evacuationService;
    }

    public DamageService damage() {
        return damageService;
    }

    public ImportService importer() {
        return importService;
    }

    /** Rolls back quietly (used in catch blocks). */
    static void rollback(Connection c) {
        try {
            if (c != null && !c.getAutoCommit()) {
                c.rollback();
            }
        } catch (SQLException ignored) {
            // nothing more we can do
        }
    }
}
