package com.cqu.drs.util;

import com.cqu.drs.model.DisasterType;
import com.cqu.drs.model.SeverityLevel;

/**
 * Computes a numeric priority score for an assessed disaster report so that
 * responses can be ranked ("quick and accurate assessment ... prioritized responses").
 *
 * Score = (severity weight x 10) + (disaster-type weight x 5). Higher score = more urgent.
 */
public final class PriorityCalculator {

    private PriorityCalculator() {
        // utility class - no instances
    }

    /** Relative urgency weighting of each disaster type (1 = lowest, 4 = highest). */
    public static int typeWeight(DisasterType type) {
        switch (type) {
            case EARTHQUAKE:
            case HURRICANE:
                return 4;
            case FIRE:
            case FLOOD:
                return 3;
            case LANDSLIDE:
                return 2;
            case OTHER:
            default:
                return 1;
        }
    }

    /** Calculates the priority score for an assessed disaster report; higher means more urgent. */
    public static int calculate(DisasterType type, SeverityLevel severity) {
        if (severity == null) {
            throw new IllegalArgumentException("Severity must be assessed before calculating priority");
        }
        return (severity.getWeight() * 10) + (typeWeight(type) * 5);
    }
}
