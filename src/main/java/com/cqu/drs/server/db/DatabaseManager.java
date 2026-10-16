package com.cqu.drs.server.db;

import com.cqu.drs.server.ServerConfig;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Creates the MySQL database and tables programmatically on start-up and hands
 * out JDBC connections. Each request gets its own connection, so the worker
 * threads of the multi-threaded server never share JDBC state.
 */
public class DatabaseManager {

    private static final String OPTIONS = "?useSSL=false&allowPublicKeyRetrieval=true"
            + "&serverTimezone=UTC&characterEncoding=UTF-8";

    private final String serverUrl;
    private final String databaseUrl;
    private final String databaseName;
    private final String user;
    private final String password;

    public DatabaseManager(ServerConfig config) {
        this(config.getDbHost(), config.getDbPort(), config.getDbName(), config.getDbUser(), config.getDbPassword());
    }

    public DatabaseManager(String host, int port, String databaseName, String user, String password) {
        if (!databaseName.matches("[A-Za-z0-9_]+")) {
            throw new IllegalArgumentException("Invalid database name: " + databaseName);
        }
        this.serverUrl = "jdbc:mysql://" + host + ":" + port + "/" + OPTIONS;
        this.databaseUrl = "jdbc:mysql://" + host + ":" + port + "/" + databaseName + OPTIONS;
        this.databaseName = databaseName;
        this.user = user;
        this.password = password;
    }

    /** Creates the database (if missing) and every table (if missing). */
    public void initialise() throws SQLException {
        try (Connection c = DriverManager.getConnection(serverUrl, user, password);
             Statement st = c.createStatement()) {
            st.executeUpdate("CREATE DATABASE IF NOT EXISTS " + databaseName
                    + " CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci");
        }
        try (Connection c = getConnection(); Statement st = c.createStatement()) {
            for (String ddl : Schema.ALL_TABLES) {
                st.executeUpdate(ddl);
            }
        }
    }

    /** Opens a new connection to the DRS database (caller must close it). */
    public Connection getConnection() throws SQLException {
        return DriverManager.getConnection(databaseUrl, user, password);
    }

    /** Drops every table - used only by the integration tests to start from a clean database. */
    public void dropAllTables() throws SQLException {
        try (Connection c = getConnection(); Statement st = c.createStatement()) {
            st.execute("SET FOREIGN_KEY_CHECKS = 0");
            for (String t : Schema.TABLE_NAMES) {
                st.execute("DROP TABLE IF EXISTS " + t);
            }
            st.execute("SET FOREIGN_KEY_CHECKS = 1");
        }
    }

    public String getDatabaseName() {
        return databaseName;
    }

    public String getDatabaseUrl() {
        return databaseUrl;
    }
}
