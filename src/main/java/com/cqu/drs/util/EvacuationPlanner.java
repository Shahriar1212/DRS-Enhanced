package com.cqu.drs.util;

import com.cqu.drs.model.AlertLevel;
import com.cqu.drs.model.DisasterReport;
import com.cqu.drs.model.SeverityLevel;
import com.cqu.drs.model.Shelter;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.PriorityQueue;

/**
 * Business rules for the "Evacuation Warning &amp; Shelter Management" feature:
 * recommends a warning level from the assessed severity, drafts the public
 * message, and picks the best shelter for a group of evacuees.
 */
public final class EvacuationPlanner {

    private EvacuationPlanner() {
        // utility class - no instances
    }

    /** CRITICAL -&gt; Emergency Warning, HIGH -&gt; Watch and Act, otherwise Advice. */
    public static AlertLevel recommendAlertLevel(SeverityLevel severity) {
        if (severity == null) {
            return AlertLevel.ADVICE;
        }
        switch (severity) {
            case CRITICAL:
                return AlertLevel.EMERGENCY_WARNING;
            case HIGH:
                return AlertLevel.WATCH_AND_ACT;
            default:
                return AlertLevel.ADVICE;
        }
    }

    /** Drafts the public warning text for a report, level and (optional) shelter. */
    public static String draftMessage(DisasterReport report, AlertLevel level, String zone, Shelter shelter) {
        StringBuilder sb = new StringBuilder();
        sb.append(level.getLabel().toUpperCase(Locale.ROOT)).append(": ")
                .append(report.getType()).append(" affecting ").append(zone).append(". ")
                .append(level.getDefaultAction());
        if (shelter != null) {
            sb.append(" Nearest available shelter: ").append(shelter.getName())
                    .append(", ").append(shelter.getLocation()).append('.');
        }
        return sb.toString();
    }

    /**
     * Recommends the best shelter for {@code people} evacuees from {@code zone}.
     * Shelters that are closed or too small are excluded; shelters whose location
     * matches the zone are preferred, then the one with the most free places.
     * A {@link PriorityQueue} keeps the ranking O(n log n).
     */
    public static Optional<Shelter> recommendShelter(List<Shelter> shelters, String zone, int people) {
        String z = zone == null ? "" : zone.toLowerCase(Locale.ROOT);
        Comparator<Shelter> ranking = Comparator
                .comparing((Shelter s) -> !matchesZone(s, z))      // zone matches first
                .thenComparing(Shelter::getFreeSpaces, Comparator.reverseOrder());
        PriorityQueue<Shelter> queue = new PriorityQueue<>(ranking);
        for (Shelter s : shelters) {
            if (s.canAccept(Math.max(1, people))) {
                queue.add(s);
            }
        }
        return Optional.ofNullable(queue.poll());
    }

    private static boolean matchesZone(Shelter s, String zoneLower) {
        if (zoneLower.isEmpty() || s.getLocation() == null) {
            return false;
        }
        String loc = s.getLocation().toLowerCase(Locale.ROOT);
        return zoneLower.contains(loc) || loc.contains(zoneLower);
    }
}
