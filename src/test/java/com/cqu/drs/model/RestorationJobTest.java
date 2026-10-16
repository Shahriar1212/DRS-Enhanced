package com.cqu.drs.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** Unit tests for restoration job progress (new feature 2) and alert lifecycle (new feature 1). */
class RestorationJobTest {

    @Test
    @DisplayName("TC-D01 progress drives job status PLANNED -> IN_PROGRESS -> COMPLETED")
    void progressDrivesStatus() {
        RestorationJob j = new RestorationJob();
        assertEquals(JobStatus.PLANNED, j.getStatus());
        j.updateProgress(40);
        assertEquals(JobStatus.IN_PROGRESS, j.getStatus());
        j.updateProgress(100);
        assertEquals(JobStatus.COMPLETED, j.getStatus());
    }

    @Test
    @DisplayName("TC-D02 progress outside 0-100 or going backwards is rejected")
    void invalidProgressRejected() {
        RestorationJob j = new RestorationJob();
        assertThrows(IllegalArgumentException.class, () -> j.updateProgress(101));
        assertThrows(IllegalArgumentException.class, () -> j.updateProgress(-1));
        j.updateProgress(50);
        assertThrows(IllegalArgumentException.class, () -> j.updateProgress(30));
        assertEquals(50, j.getProgress());
    }

    @Test
    @DisplayName("TC-D03 damage assessment rejects negative cost or people")
    void assessmentValidation() {
        assertThrows(IllegalArgumentException.class, () -> new DamageAssessment(1,
                InfrastructureCategory.POWER, DamageLevel.MINOR, -1, 0, null));
        assertThrows(IllegalArgumentException.class, () -> new DamageAssessment(1,
                InfrastructureCategory.POWER, DamageLevel.MINOR, 0, -5, null));
    }

    @Test
    @DisplayName("TC-E07 an alert can be lifted once only and is time-stamped")
    void alertLiftedOnce() {
        EvacuationAlert a = new EvacuationAlert(1, "Yeppoon", AlertLevel.EMERGENCY_WARNING, "Evacuate");
        assertEquals(AlertStatus.ACTIVE, a.getStatus());
        a.lift();
        assertEquals(AlertStatus.LIFTED, a.getStatus());
        assertNotNull(a.getLiftedAt());
        assertThrows(IllegalStateException.class, a::lift);
    }
}
