package com.cqu.drs.model;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * Result of a field damage assessment for one infrastructure category at a
 * reported disaster. Part of the "Damage Assessment &amp; Restoration Planning"
 * feature of DRS-Enhanced.
 */
public class DamageAssessment implements Serializable {

    private static final long serialVersionUID = 1L;

    private int assessmentId;
    private int reportId;
    private String reportSummary;
    private InfrastructureCategory category;
    private DamageLevel damageLevel;
    private double estimatedCost;
    private int affectedPeople;
    private String notes;
    private String assessedBy;
    private LocalDateTime assessedAt;

    public DamageAssessment() {
    }

    public DamageAssessment(int reportId, InfrastructureCategory category, DamageLevel damageLevel,
                            double estimatedCost, int affectedPeople, String notes) {
        if (estimatedCost < 0) {
            throw new IllegalArgumentException("Estimated cost cannot be negative");
        }
        if (affectedPeople < 0) {
            throw new IllegalArgumentException("Affected people cannot be negative");
        }
        this.reportId = reportId;
        this.category = category;
        this.damageLevel = damageLevel;
        this.estimatedCost = estimatedCost;
        this.affectedPeople = affectedPeople;
        this.notes = notes;
    }

    public int getAssessmentId() {
        return assessmentId;
    }

    public void setAssessmentId(int assessmentId) {
        this.assessmentId = assessmentId;
    }

    public int getReportId() {
        return reportId;
    }

    public void setReportId(int reportId) {
        this.reportId = reportId;
    }

    public String getReportSummary() {
        return reportSummary;
    }

    public void setReportSummary(String reportSummary) {
        this.reportSummary = reportSummary;
    }

    public InfrastructureCategory getCategory() {
        return category;
    }

    public void setCategory(InfrastructureCategory category) {
        this.category = category;
    }

    public DamageLevel getDamageLevel() {
        return damageLevel;
    }

    public void setDamageLevel(DamageLevel damageLevel) {
        this.damageLevel = damageLevel;
    }

    public double getEstimatedCost() {
        return estimatedCost;
    }

    public void setEstimatedCost(double estimatedCost) {
        this.estimatedCost = estimatedCost;
    }

    public int getAffectedPeople() {
        return affectedPeople;
    }

    public void setAffectedPeople(int affectedPeople) {
        this.affectedPeople = affectedPeople;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public String getAssessedBy() {
        return assessedBy;
    }

    public void setAssessedBy(String assessedBy) {
        this.assessedBy = assessedBy;
    }

    public LocalDateTime getAssessedAt() {
        return assessedAt;
    }

    public void setAssessedAt(LocalDateTime assessedAt) {
        this.assessedAt = assessedAt;
    }

    @Override
    public String toString() {
        return "Damage #" + assessmentId + " " + category + " " + damageLevel
                + String.format(" $%,.0f", estimatedCost) + " (" + affectedPeople + " affected)";
    }
}
