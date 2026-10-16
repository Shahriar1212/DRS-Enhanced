package com.cqu.drs.client;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Reads the CSV files exported from DRS-Initial (Assessment 2): departments,
 * resources, volunteers and disaster reports. Supports double-quoted fields
 * containing commas. The first line of every file is a header and is skipped.
 */
public final class LegacyDataReader {

    /** Data set name -&gt; file name. */
    public static final Map<String, String> FILES = Map.of(
            "departments", "departments.csv",
            "resources", "resources.csv",
            "volunteers", "volunteers.csv",
            "reports", "disaster_reports.csv");

    private static final String BUNDLED = "/com/cqu/drs/data/drs-initial/";

    private LegacyDataReader() {
    }

    /** Reads the copy of the DRS-Initial data bundled inside the application. */
    public static HashMap<String, List<String[]>> readBundled() throws IOException {
        HashMap<String, List<String[]>> data = new HashMap<>();
        for (Map.Entry<String, String> e : FILES.entrySet()) {
            try (InputStream in = LegacyDataReader.class.getResourceAsStream(BUNDLED + e.getValue())) {
                if (in == null) {
                    throw new IOException("Bundled file missing: " + e.getValue());
                }
                data.put(e.getKey(), parse(new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))));
            }
        }
        return data;
    }

    /** Reads the CSV files from a folder chosen by the user (missing files are skipped). */
    public static HashMap<String, List<String[]>> readFolder(Path folder) throws IOException {
        HashMap<String, List<String[]>> data = new HashMap<>();
        for (Map.Entry<String, String> e : FILES.entrySet()) {
            Path f = folder.resolve(e.getValue());
            if (Files.exists(f)) {
                try (BufferedReader r = Files.newBufferedReader(f, StandardCharsets.UTF_8)) {
                    data.put(e.getKey(), parse(r));
                }
            }
        }
        if (data.isEmpty()) {
            throw new IOException("No DRS-Initial CSV files found in " + folder);
        }
        return data;
    }

    static List<String[]> parse(BufferedReader reader) throws IOException {
        List<String[]> rows = new ArrayList<>();
        String line;
        boolean header = true;
        while ((line = reader.readLine()) != null) {
            if (header) {
                header = false;
                continue;
            }
            if (!line.isBlank()) {
                rows.add(splitCsv(line));
            }
        }
        return rows;
    }

    /** Splits one CSV line, honouring double quotes ("a, b" stays one field; "" is an escaped quote). */
    public static String[] splitCsv(String line) {
        List<String> fields = new ArrayList<>();
        StringBuilder sb = new StringBuilder();
        boolean quoted = false;
        for (int i = 0; i < line.length(); i++) {
            char ch = line.charAt(i);
            if (quoted) {
                if (ch == '"' && i + 1 < line.length() && line.charAt(i + 1) == '"') {
                    sb.append('"');
                    i++;
                } else if (ch == '"') {
                    quoted = false;
                } else {
                    sb.append(ch);
                }
            } else if (ch == '"') {
                quoted = true;
            } else if (ch == ',') {
                fields.add(sb.toString());
                sb.setLength(0);
            } else {
                sb.append(ch);
            }
        }
        fields.add(sb.toString());
        return fields.toArray(new String[0]);
    }
}
