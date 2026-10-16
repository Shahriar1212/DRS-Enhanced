package com.cqu.drs.model;

/**
 * Severity of damage found during a damage assessment. The weight drives
 * restoration-job priority and the base days drive the time estimate.
 */
public enum DamageLevel {
    MINOR(1, 2),
    MODERATE(2, 7),
    MAJOR(3, 21),
    DESTROYED(4, 60);

    private final int weight;
    private final int baseDays;

    DamageLevel(int weight, int baseDays) {
        this.weight = weight;
        this.baseDays = baseDays;
    }

    public int getWeight() {
        return weight;
    }

    public int getBaseDays() {
        return baseDays;
    }
}
