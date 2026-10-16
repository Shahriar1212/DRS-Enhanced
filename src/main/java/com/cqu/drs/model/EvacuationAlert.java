package com.cqu.drs.model;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * An evacuation warning issued for a zone affected by a {@link DisasterReport},
 * optionally nominating a {@link Shelter} that evacuees should go to.
 */
public class EvacuationAlert implements Serializable {

    private static final long serialVersionUID = 1L;

    private int alertId;
    private int reportId;
    private String reportSummary;
    private String zone;
    private AlertLevel level;
    private String message;
    private Integer shelterId;
    private String shelterName;
    private AlertStatus status;
    private String issuedBy;
    private LocalDateTime issuedAt;
    private LocalDateTime liftedAt;

    public EvacuationAlert() {
        this.status = AlertStatus.ACTIVE;
    }

    public EvacuationAlert(int reportId, String zone, AlertLevel level, String message) {
        this();
        this.reportId = reportId;
        this.zone = zone;
        this.level = level;
        this.message = message;
    }

    public int getAlertId() {
        return alertId;
    }

    public void setAlertId(int alertId) {
        this.alertId = alertId;
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

    public String getZone() {
        return zone;
    }

    public void setZone(String zone) {
        this.zone = zone;
    }

    public AlertLevel getLevel() {
        return level;
    }

    public void setLevel(AlertLevel level) {
        this.level = level;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public Integer getShelterId() {
        return shelterId;
    }

    public void setShelterId(Integer shelterId) {
        this.shelterId = shelterId;
    }

    public String getShelterName() {
        return shelterName;
    }

    public void setShelterName(String shelterName) {
        this.shelterName = shelterName;
    }

    public AlertStatus getStatus() {
        return status;
    }

    public void setStatus(AlertStatus status) {
        this.status = status;
    }

    public String getIssuedBy() {
        return issuedBy;
    }

    public void setIssuedBy(String issuedBy) {
        this.issuedBy = issuedBy;
    }

    public LocalDateTime getIssuedAt() {
        return issuedAt;
    }

    public void setIssuedAt(LocalDateTime issuedAt) {
        this.issuedAt = issuedAt;
    }

    public LocalDateTime getLiftedAt() {
        return liftedAt;
    }

    public void setLiftedAt(LocalDateTime liftedAt) {
        this.liftedAt = liftedAt;
    }

    /** Lifts (cancels) an active alert. */
    public void lift() {
        if (status == AlertStatus.LIFTED) {
            throw new IllegalStateException("Alert #" + alertId + " has already been lifted");
        }
        status = AlertStatus.LIFTED;
        liftedAt = LocalDateTime.now();
    }

    @Override
    public String toString() {
        return "[" + level + "] " + zone + ": " + message + " (" + status + ")";
    }
}
