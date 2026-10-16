package com.cqu.drs.util;

import com.cqu.drs.model.DamageAssessment;
import com.cqu.drs.model.DamageLevel;
import com.cqu.drs.model.Department;
import com.cqu.drs.model.DepartmentType;
import com.cqu.drs.model.InfrastructureCategory;
import com.cqu.drs.model.JobStatus;
import com.cqu.drs.model.RecoverySummary;
import com.cqu.drs.model.RestorationJob;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/** Unit tests for damage-to-restoration rules (new feature 2). */
class RestorationPlannerTest {

    private Department fire;
    private Department electricity;
    private Department waste;
    private List<Department> departments;

    @BeforeEach
    void setUp() {
        fire = new Department("Fire & Emergency", DepartmentType.FIRE_AND_EMERGENCY, "111");
        fire.setDepartmentId(1);
        electricity = new Department("Ergon Energy", DepartmentType.ELECTRICITY, "333");
        electricity.setDepartmentId(3);
        waste = new Department("Waste Management", DepartmentType.WASTE_MANAGEMENT, "777");
        waste.setDepartmentId(7);
        departments = List.of(fire, electricity, waste);
    }

    @Test
    @DisplayName("TC-D04 priority = damage weight x 20 + min(people/10, 20)")
    void priorityFormula() {
        assertEquals(20, RestorationPlanner.priority(DamageLevel.MINOR, 0));
        assertEquals(60 + 12, RestorationPlanner.priority(DamageLevel.MAJOR, 120));
        assertEquals(80 + 20, RestorationPlanner.priority(DamageLevel.DESTROYED, 5000)); // capped
    }

    @Test
    @DisplayName("TC-D05 estimated days grow with damage level and cost")
    void estimatedDays() {
        assertEquals(2, RestorationPlanner.estimatedDays(DamageLevel.MINOR, 0));
        assertEquals(21 + 4, RestorationPlanner.estimatedDays(DamageLevel.MAJOR, 1_000_000));
        assertEquals(60 + 60, RestorationPlanner.estimatedDays(DamageLevel.DESTROYED, 1e12)); // capped
    }

    @Test
    @DisplayName("TC-D06 power damage is routed to the electricity department")
    void routesToResponsibleDepartment() {
        assertSame(electricity, RestorationPlanner.responsibleDepartment(InfrastructureCategory.POWER, departments).orElseThrow());
        assertSame(waste, RestorationPlanner.responsibleDepartment(InfrastructureCategory.DEBRIS_AND_WASTE, departments).orElseThrow());
    }

    @Test
    @DisplayName("TC-D07 category with no matching department falls back to Fire & Emergency")
    void fallsBackToFire() {
        assertSame(fire, RestorationPlanner.responsibleDepartment(InfrastructureCategory.SCHOOLS, departments).orElseThrow());
    }

    @Test
    @DisplayName("TC-D08 destroyed buildings create a demolition job")
    void createsDemolitionJob() {
        DamageAssessment a = new DamageAssessment(9, InfrastructureCategory.BUILDINGS, DamageLevel.DESTROYED,
                2_000_000, 300, "Row of houses flattened");
        RestorationJob job = RestorationPlanner.createJob(a, fire, "Gladstone");
        assertTrue(job.getDescription().startsWith("Demolish"));
        assertEquals(9, job.getReportId());
        assertEquals(fire.getDepartmentId(), job.getDepartmentId());
        assertEquals(JobStatus.PLANNED, job.getStatus());
        assertEquals(80 + 20, job.getPriority());
        assertEquals(60 + 8, job.getEstimatedDays());
    }

    @Test
    @DisplayName("TC-D09 recovery summary totals cost, people and progress")
    void summary() {
        DamageAssessment a1 = new DamageAssessment(1, InfrastructureCategory.POWER, DamageLevel.MAJOR, 500_000, 100, null);
        DamageAssessment a2 = new DamageAssessment(1, InfrastructureCategory.ROADS_AND_BRIDGES, DamageLevel.MINOR, 50_000, 20, null);
        RestorationJob j1 = new RestorationJob();
        j1.updateProgress(100);
        RestorationJob j2 = new RestorationJob();
        j2.updateProgress(50);
        RecoverySummary s = RestorationPlanner.summarise(List.of(a1, a2), List.of(j1, j2));
        assertEquals(550_000, s.getTotalEstimatedCost(), 0.001);
        assertEquals(120, s.getTotalAffectedPeople());
        assertEquals(2, s.getTotalJobs());
        assertEquals(1, s.getCompletedJobs());
        assertEquals(75, s.getAverageProgress(), 0.001);
    }
}
