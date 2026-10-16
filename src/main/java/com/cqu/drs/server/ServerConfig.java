package com.cqu.drs.server;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;

/**
 * Server settings read from {@code drs.properties} in the working directory
 * (the project folder when run from NetBeans), falling back to the copy
 * bundled on the classpath, then to built-in defaults.
 */
public class ServerConfig {

    public static final String FILE_NAME = "drs.properties";

    private final Properties props = new Properties();

    public ServerConfig() {
        loadDefaults();
        try (InputStream in = ServerConfig.class.getResourceAsStream("/" + FILE_NAME)) {
            if (in != null) {
                props.load(in);
            }
        } catch (IOException ignored) {
            // fall back to defaults
        }
        Path external = Paths.get(FILE_NAME);
        if (Files.exists(external)) {
            try (InputStream in = Files.newInputStream(external)) {
                props.load(in);
            } catch (IOException e) {
                System.err.println("Could not read " + external.toAbsolutePath() + ": " + e.getMessage());
            }
        }
    }

    private void loadDefaults() {
        props.setProperty("server.host", "localhost");
        props.setProperty("server.port", "5050");
        props.setProperty("server.threads", "20");
        props.setProperty("db.host", "localhost");
        props.setProperty("db.port", "3306");
        props.setProperty("db.name", "drs_enhanced");
        props.setProperty("db.user", "drs_user");
        props.setProperty("db.password", "Drs@2026");
        props.setProperty("security.keyFile", "security/drs-master.key");
        props.setProperty("session.timeoutMinutes", "30");
    }

    public String get(String key) {
        String sys = System.getProperty(key);
        return sys != null ? sys : props.getProperty(key);
    }

    public int getInt(String key) {
        return Integer.parseInt(get(key).trim());
    }

    public String getServerHost() {
        return get("server.host");
    }

    public int getServerPort() {
        return getInt("server.port");
    }

    public int getServerThreads() {
        return getInt("server.threads");
    }

    public String getDbHost() {
        return get("db.host");
    }

    public int getDbPort() {
        return getInt("db.port");
    }

    public String getDbName() {
        return get("db.name");
    }

    public String getDbUser() {
        return get("db.user");
    }

    public String getDbPassword() {
        return get("db.password");
    }

    public Path getKeyFile() {
        return Paths.get(get("security.keyFile"));
    }

    public int getSessionTimeoutMinutes() {
        return getInt("session.timeoutMinutes");
    }
}
