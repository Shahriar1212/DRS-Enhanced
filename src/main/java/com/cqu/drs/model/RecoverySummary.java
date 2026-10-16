package com.cqu.drs.model;

import java.io.Serializable;

/**
 * Recovery figures for one disaster (or all disasters): total estimated damage,
 * people affected and restoration progress.
 */
public class RecoverySummary implements Serializable {

    private static final long serialVersionUID = 1L;

    private final double totalEstimatedCost;
    private final int totalAffectedPeople;
    private final int totalJobs;
    private final int completedJobs;
    private final double averageProgress;

    public RecoverySummary(double totalEstimatedCost, int totalAffectedPeople, int totalJobs,
                           int completedJobs, double averageProgress) {
        this.totalEstimatedCost = totalEstimatedCost;
        this.totalAffectedPeople = totalAffectedPeople;
        this.totalJobs = totalJobs;
        this.completedJobs = completedJobs;
        this.averageProgress = averageProgress;
    }

    public double getTotalEstimatedCost() {
        return totalEstimatedCost;
    }

    public int getTotalAffectedPeople() {
        return totalAffectedPeople;
    }

    public int getTotalJobs() {
        return totalJobs;
    }

    public int getCompletedJobs() {
        return completedJobs;
    }

    public double getAverageProgress() {
        return averageProgress;
    }

    @Override
    public String toString() {
        return String.format("Estimated damage $%,.0f | %d people affected | %d/%d restoration jobs complete | %.0f%% restored",
                totalEstimatedCost, totalAffectedPeople, completedJobs, totalJobs, averageProgress);
    }
}
