package com.cqu.drs.client;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;

/** Reads the server host/port for the client from drs.properties (defaults localhost:5050). */
public final class ClientConfig {

    private ClientConfig() {
    }

    public static String serverAddress() {
        Properties p = new Properties();
        p.setProperty("server.host", "localhost");
        p.setProperty("server.port", "5050");
        try (InputStream in = ClientConfig.class.getResourceAsStream("/drs.properties")) {
            if (in != null) {
                p.load(in);
            }
        } catch (Exception ignored) {
            // defaults
        }
        Path f = Paths.get("drs.properties");
        if (Files.exists(f)) {
            try (InputStream in = Files.newInputStream(f)) {
                p.load(in);
            } catch (Exception ignored) {
                // defaults
            }
        }
        return p.getProperty("server.host").trim() + ":" + p.getProperty("server.port").trim();
    }
}
