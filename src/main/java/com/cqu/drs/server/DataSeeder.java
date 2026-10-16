package com.cqu.drs.server;

import com.cqu.drs.model.Department;
import com.cqu.drs.model.DepartmentType;
import com.cqu.drs.model.Resource;
import com.cqu.drs.model.Role;
import com.cqu.drs.model.Shelter;
import com.cqu.drs.server.db.DepartmentDAO;
import com.cqu.drs.server.db.ShelterDAO;
import com.cqu.drs.server.service.ServerContext;

import java.sql.Connection;
import java.sql.SQLException;

/**
 * Inserts the minimum reference data on the very first start-up (empty
 * database): one account per role, the two departments DRS-Initial did not
 * have (Waste Management, Schools) and the evacuation shelters. The
 * DRS-Initial (Assessment 2) data itself is loaded from the Admin screen.
 */
public class DataSeeder {

    /** Default accounts: username, full name, password, role. Change these passwords after first login. */
    public static final String[][] DEFAULT_USERS = {
            {"admin", "System Administrator", "Admin@123", "ADMIN"},
            {"coord", "Emma Coordinator", "Coord@123", "COORDINATOR"},
            {"officer", "Liam Field Officer", "Officer@123", "FIELD_OFFICER"},
            {"public", "Olivia Public", "Public@123", "REPORTER"}
    };

    private final ServerContext ctx;

    public DataSeeder(ServerContext ctx) {
        this.ctx = ctx;
    }

    /** @return true if seeding was performed */
    public boolean seedIfEmpty() throws SQLException {
        if (ctx.auth().countUsers() > 0) {
            return false;
        }
        for (String[] u : DEFAULT_USERS) {
            ctx.auth().createUser(u[0], u[1], u[2], Role.valueOf(u[3]));
        }
        try (Connection c = ctx.connection()) {
            DepartmentDAO depts = new DepartmentDAO();
            Department waste = new Department("Waste Management Services", DepartmentType.WASTE_MANAGEMENT, "07 4000 7777");
            Department schools = new Department("Regional Schools Office", DepartmentType.SCHOOLS, "07 4000 8888");
            depts.insert(c, waste);
            depts.insert(c, schools);
            depts.insertResource(c, waste.getDepartmentId(), new Resource("Skip bins", 60, 10));
            depts.insertResource(c, waste.getDepartmentId(), new Resource("Debris trucks", 12, 3));
            depts.insertResource(c, schools.getDepartmentId(), new Resource("Classroom kits", 80, 15));

            ShelterDAO shelters = new ShelterDAO();
            shelters.insertShelter(c, new Shelter("Rockhampton Showgrounds", "Rockhampton", 500, "07 4900 1000"));
            shelters.insertShelter(c, new Shelter("Yeppoon Community Hall", "Yeppoon", 150, "07 4900 2000"));
            shelters.insertShelter(c, new Shelter("Gladstone Entertainment Centre", "Gladstone", 300, "07 4900 3000"));
            shelters.insertShelter(c, new Shelter("CQUniversity Gym", "Rockhampton North", 200, "07 4900 4000"));
        }
        ctx.audit().record("admin", "SEED_DATABASE", "Default users, departments and shelters created");
        return true;
    }
}
