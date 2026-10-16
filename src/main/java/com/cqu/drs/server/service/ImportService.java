package com.cqu.drs.server.service;

import com.cqu.drs.model.Department;
import com.cqu.drs.model.DepartmentType;
import com.cqu.drs.model.DisasterType;
import com.cqu.drs.model.Resource;
import com.cqu.drs.model.SeverityLevel;
import com.cqu.drs.model.User;
import com.cqu.drs.model.Volunteer;
import com.cqu.drs.util.Validation;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;

/**
 * Loads the data collected with DRS-Initial (Assessment 2) into MySQL. The
 * client parses the CSV files and sends the rows to the server, which stores
 * them through the same services as live requests (so reports are encrypted,
 * signed, assessed and audited). Re-importing skips rows that already exist.
 */
public class ImportService {

    private final ServerContext ctx;

    ImportService(ServerContext ctx) {
        this.ctx = ctx;
    }

    /**
     * @param data map of "departments", "resources", "reports", "volunteers" to rows of CSV fields
     * @return a summary such as "Imported 6 departments, 4 resources, 5 reports (8 tasks), 4 volunteers; skipped 0"
     */
    public String importLegacy(User admin, Map<String, List<String[]>> data) throws SQLException {
        int depts = 0;
        int resources = 0;
        int reports = 0;
        int volunteers = 0;
        int skipped = 0;
        try (Connection c = ctx.connection()) {
            for (String[] f : data.getOrDefault("departments", List.of())) {
                // name,type,contact
                if (ctx.departmentDAO.findByName(c, f[0].trim()) != null) {
                    skipped++;
                    continue;
                }
                ctx.departmentDAO.insert(c, new Department(f[0].trim(),
                        DepartmentType.valueOf(f[1].trim()), f.length > 2 ? f[2].trim() : null));
                depts++;
            }
            for (String[] f : data.getOrDefault("resources", List.of())) {
                // department,name,quantity,reorderLevel
                Department d = ctx.departmentDAO.findByName(c, f[0].trim());
                if (d == null || ctx.departmentDAO.resourceExists(c, d.getDepartmentId(), f[1].trim())) {
                    skipped++;
                    continue;
                }
                ctx.departmentDAO.insertResource(c, d.getDepartmentId(),
                        new Resource(f[1].trim(), Integer.parseInt(f[2].trim()), Integer.parseInt(f[3].trim())));
                resources++;
            }
            for (String[] f : data.getOrDefault("volunteers", List.of())) {
                // name,phone,skill
                if (ctx.volunteerDAO.existsByName(c, f[0].trim())) {
                    skipped++;
                    continue;
                }
                Volunteer v = new Volunteer(f[0].trim(), Validation.optionalPhone(f.length > 1 ? f[1] : null),
                        f.length > 2 ? Validation.optionalText(f[2], "Skill", 100) : null);
                ctx.volunteerDAO.insert(c, v, ctx.getCrypto().encrypt(v.getPhone()));
                volunteers++;
            }
        }
        for (String[] f : data.getOrDefault("reports", List.of())) {
            // type,location,description,reporterName,reporterPhone,severity(optional)
            DisasterType type = DisasterType.valueOf(f[0].trim());
            try (Connection c = ctx.connection()) {
                if (ctx.reportDAO.exists(c, type, f[1].trim(), f[2].trim())) {
                    skipped++;
                    continue;
                }
            }
            var report = ctx.disasters().report(admin, type, f[1], f[2], f[3], f.length > 4 ? f[4] : null);
            if (f.length > 5 && !Validation.isBlank(f[5])) {
                ctx.disasters().assess(admin, report.getReportId(), SeverityLevel.valueOf(f[5].trim()));
            }
            reports++;
        }
        String summary = "Imported " + depts + " department(s), " + resources + " resource(s), " + reports
                + " report(s), " + volunteers + " volunteer(s); skipped " + skipped + " existing row(s)";
        ctx.audit().record(admin.getUsername(), "IMPORT_LEGACY_DATA", summary);
        return summary;
    }
}
