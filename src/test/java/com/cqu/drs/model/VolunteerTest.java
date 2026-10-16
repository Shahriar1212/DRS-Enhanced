package com.cqu.drs.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class VolunteerTest {

    @Test
    void newVolunteerIsAvailableByDefault() {
        Volunteer v = new Volunteer("Sam Lee", "0400111222", "First Aid");
        assertTrue(v.isAvailable());
    }

    @Test
    void assigningToTaskMarksVolunteerUnavailable() {
        DisasterReport report = new DisasterReport(DisasterType.FIRE, "CBD", "Warehouse fire",
                "Alex Chen", "0400123456");
        report.setSeverity(SeverityLevel.HIGH);
        Department fire = new Department("Fire & Emergency", DepartmentType.FIRE_AND_EMERGENCY, "111");
        ResponseTask task = new ResponseTask("Support fire crew", 30, report, fire);
        Volunteer v = new Volunteer("Priya Nair", "0400333444", "Firefighting");

        task.assignVolunteer(v);

        assertFalse(v.isAvailable());
        assertEquals(v, task.getAssignedVolunteer());
        assertEquals(Status.IN_PROGRESS, task.getStatus());
    }
}
