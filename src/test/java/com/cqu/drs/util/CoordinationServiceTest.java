package com.cqu.drs.util;

import com.cqu.drs.model.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class CoordinationServiceTest {

    private Department fire;
    private Department hospital;
    private Department police;
    private Department electricity;
    private List<Department> departments;

    @BeforeEach
    void setUp() {
        fire = new Department("Fire & Emergency", DepartmentType.FIRE_AND_EMERGENCY, "111");
        hospital = new Department("Base Hospital", DepartmentType.HOSPITAL, "222");
        police = new Department("Police Service", DepartmentType.LAW_ENFORCEMENT, "333");
        electricity = new Department("Ergon Energy", DepartmentType.ELECTRICITY, "444");
        departments = Arrays.asList(fire, hospital, police, electricity);
    }

    @Test
    void coordinateResponseCreatesTasksOnlyForRelevantDepartments() {
        DisasterReport report = new DisasterReport(DisasterType.FIRE, "CBD", "Warehouse fire",
                "Alex Chen", "0400123456");
        report.setSeverity(SeverityLevel.HIGH);
        report.setPriorityScore(PriorityCalculator.calculate(report.getType(), report.getSeverity()));

        List<ResponseTask> tasks = CoordinationService.coordinateResponse(report, departments);

        // FIRE maps to FIRE_AND_EMERGENCY, HOSPITAL, LAW_ENFORCEMENT -> not ELECTRICITY
        assertEquals(3, tasks.size());
        assertTrue(tasks.stream().anyMatch(t -> t.getDepartment() == fire));
        assertTrue(tasks.stream().anyMatch(t -> t.getDepartment() == hospital));
        assertTrue(tasks.stream().anyMatch(t -> t.getDepartment() == police));
        assertTrue(tasks.stream().noneMatch(t -> t.getDepartment() == electricity));
        assertEquals(Status.IN_PROGRESS, report.getStatus());
    }

    @Test
    void coordinateResponseOnUnassessedReportThrows() {
        DisasterReport report = new DisasterReport(DisasterType.FLOOD, "Yeppoon", "Minor flooding",
                "Alex Chen", "0400123456");
        assertThrows(IllegalStateException.class, () -> CoordinationService.coordinateResponse(report, departments));
    }
}
