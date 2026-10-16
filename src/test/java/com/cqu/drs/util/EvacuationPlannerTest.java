package com.cqu.drs.util;

import com.cqu.drs.model.AlertLevel;
import com.cqu.drs.model.DisasterReport;
import com.cqu.drs.model.DisasterType;
import com.cqu.drs.model.SeverityLevel;
import com.cqu.drs.model.Shelter;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/** Unit tests for the evacuation business rules (new feature 1). */
class EvacuationPlannerTest {

    @ParameterizedTest(name = "TC-E08 severity {0} -> {1}")
    @CsvSource({"LOW,ADVICE", "MEDIUM,ADVICE", "HIGH,WATCH_AND_ACT", "CRITICAL,EMERGENCY_WARNING"})
    void alertLevelFollowsSeverity(SeverityLevel severity, AlertLevel expected) {
        assertEquals(expected, EvacuationPlanner.recommendAlertLevel(severity));
    }

    @Test
    @DisplayName("TC-E09 unassessed report gets Advice")
    void nullSeverityIsAdvice() {
        assertEquals(AlertLevel.ADVICE, EvacuationPlanner.recommendAlertLevel(null));
    }

    @Test
    @DisplayName("TC-E10 shelter in the same zone is preferred over a bigger one elsewhere")
    void prefersShelterInZone() {
        Shelter far = new Shelter("Rockhampton Showgrounds", "Rockhampton", 500, null);
        Shelter near = new Shelter("Yeppoon Hall", "Yeppoon", 100, null);
        Optional<Shelter> best = EvacuationPlanner.recommendShelter(List.of(far, near), "Yeppoon foreshore", 40);
        assertTrue(best.isPresent());
        assertSame(near, best.get());
    }

    @Test
    @DisplayName("TC-E11 shelter without enough room is skipped")
    void skipsFullShelter() {
        Shelter near = new Shelter("Yeppoon Hall", "Yeppoon", 100, null);
        near.checkIn(90);
        Shelter far = new Shelter("Rockhampton Showgrounds", "Rockhampton", 500, null);
        assertSame(far, EvacuationPlanner.recommendShelter(List.of(near, far), "Yeppoon", 20).orElseThrow());
    }

    @Test
    @DisplayName("TC-E12 no shelter has room -> empty result")
    void noShelterAvailable() {
        Shelter s = new Shelter("Tiny", "Yeppoon", 5, null);
        assertTrue(EvacuationPlanner.recommendShelter(List.of(s), "Yeppoon", 10).isEmpty());
    }

    @Test
    @DisplayName("TC-E13 drafted message names level, zone and shelter")
    void draftMessage() {
        DisasterReport r = new DisasterReport(DisasterType.FLOOD, "Rockhampton North", "River rising", "Tom", null);
        Shelter s = new Shelter("CQUniversity Gym", "Rockhampton North", 200, null);
        String msg = EvacuationPlanner.draftMessage(r, AlertLevel.WATCH_AND_ACT, "Rockhampton North", s);
        assertTrue(msg.startsWith("WATCH AND ACT"));
        assertTrue(msg.contains("FLOOD"));
        assertTrue(msg.contains("CQUniversity Gym"));
    }
}
