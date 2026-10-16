package com.cqu.drs.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class DisasterReportTest {

    @Test
    void newReportStartsInReportedStatusWithNoSeverity() {
        DisasterReport report = new DisasterReport(DisasterType.FIRE, "Rockhampton CBD", "Warehouse fire",
                "Alex Chen", "0400123456");

        assertEquals(Status.REPORTED, report.getStatus());
        assertNull(report.getSeverity());
        assertEquals(0, report.getPriorityScore());
        assertEquals("Alex Chen", report.getReporterName());
    }

    @Test
    void settingSeverityAndPriorityUpdatesReport() {
        DisasterReport report = new DisasterReport(DisasterType.EARTHQUAKE, "Yeppoon", "Magnitude 5 tremor",
                "Alex Chen", "0400123456");

        report.setSeverity(SeverityLevel.CRITICAL);
        report.setPriorityScore(55);
        report.setStatus(Status.ASSESSED);

        assertEquals(SeverityLevel.CRITICAL, report.getSeverity());
        assertEquals(55, report.getPriorityScore());
        assertEquals(Status.ASSESSED, report.getStatus());
    }

    @Test
    void toStringContainsKeyFields() {
        DisasterReport report = new DisasterReport(DisasterType.HURRICANE, "Gladstone", "Category 3 hurricane",
                "Alex Chen", "0400123456");
        String text = report.toString();

        assertTrue(text.contains("HURRICANE"));
        assertTrue(text.contains("Gladstone"));
        assertTrue(text.contains("Alex Chen"));
    }
}
