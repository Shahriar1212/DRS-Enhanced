package com.cqu.drs.model;

/**
 * Severity assigned to a disaster report during assessment.
 * Each level carries a numeric weight used by {@link com.cqu.drs.util.PriorityCalculator}.
 */
public enum SeverityLevel {
    LOW(1),
    MEDIUM(2),
    HIGH(3),
    CRITICAL(4);

    private final int weight;

    SeverityLevel(int weight) {
        this.weight = weight;
    }

    public int getWeight() {
        return weight;
    }
}
