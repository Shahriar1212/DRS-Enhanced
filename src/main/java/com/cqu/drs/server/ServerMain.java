package com.cqu.drs.server;

import com.cqu.drs.security.CryptoService;
import com.cqu.drs.server.db.DatabaseManager;
import com.cqu.drs.server.service.ServerContext;

import java.sql.SQLException;

/**
 * Entry point of the DRS-Enhanced server (run this first).
 * <ol>
 *   <li>reads drs.properties,</li>
 *   <li>creates the MySQL database and tables programmatically,</li>
 *   <li>seeds default accounts/shelters on first run,</li>
 *   <li>starts the multi-threaded socket server.</li>
 * </ol>
 */
public class ServerMain {

    public static void main(String[] args) {
        ServerConfig config = new ServerConfig();
        System.out.println("==============================================");
        System.out.println(" DRS-Enhanced Server - COIT20258 Assessment 3");
        System.out.println("==============================================");
        DatabaseManager db = new DatabaseManager(config);
        try {
            System.out.println("Connecting to MySQL " + config.getDbHost() + ":" + config.getDbPort()
                    + " as '" + config.getDbUser() + "' ...");
            db.initialise();
            System.out.println("Database '" + db.getDatabaseName() + "' and 12 tables ready (created if missing).");
        } catch (SQLException e) {
            System.err.println("Cannot connect to / initialise MySQL: " + e.getMessage());
            System.err.println("Check that MySQL is running and that db.user/db.password in drs.properties are correct");
            System.err.println("(run database/00_create_app_user.sql as root once to create the drs_user account).");
            System.exit(1);
            return;
        }
        CryptoService crypto = CryptoService.loadOrCreate(config.getKeyFile());
        ServerContext ctx = new ServerContext(db, crypto);
        try {
            if (new DataSeeder(ctx).seedIfEmpty()) {
                System.out.println("First run: default accounts, departments and shelters created.");
                for (String[] u : DataSeeder.DEFAULT_USERS) {
                    System.out.println("   " + u[3] + ": " + u[0] + " / " + u[2]);
                }
            }
        } catch (SQLException e) {
            System.err.println("Seeding failed: " + e.getMessage());
        }
        SessionManager sessions = new SessionManager(config.getSessionTimeoutMinutes());
        DRSServer server = new DRSServer(config.getServerPort(), config.getServerThreads(),
                new RequestDispatcher(ctx, sessions));
        try {
            server.start();
            System.out.println("Thread pool size: " + config.getServerThreads()
                    + ". Start the JavaFX client now (Run Project). Press Ctrl+C / Stop to shut down.");
        } catch (java.io.IOException e) {
            System.err.println("Cannot listen on port " + config.getServerPort() + ": " + e.getMessage()
                    + " (is another server already running?)");
            System.exit(2);
        }
        Runtime.getRuntime().addShutdownHook(new Thread(server::stop));
    }
}
