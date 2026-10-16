package com.cqu.drs.model;

/**
 * Warning levels used for evacuation alerts. Modelled on the three-tier
 * Australian Warning System (Advice, Watch and Act, Emergency Warning).
 */
public enum AlertLevel {
    ADVICE(1, "Advice", "Stay informed and monitor conditions."),
    WATCH_AND_ACT(2, "Watch and Act", "Conditions are changing - prepare to leave now."),
    EMERGENCY_WARNING(3, "Emergency Warning", "Evacuate immediately to the nominated shelter.");

    private final int rank;
    private final String label;
    private final String defaultAction;

    AlertLevel(int rank, String label, String defaultAction) {
        this.rank = rank;
        this.label = label;
        this.defaultAction = defaultAction;
    }

    public int getRank() {
        return rank;
    }

    public String getLabel() {
        return label;
    }

    public String getDefaultAction() {
        return defaultAction;
    }

    @Override
    public String toString() {
        return label;
    }
}
