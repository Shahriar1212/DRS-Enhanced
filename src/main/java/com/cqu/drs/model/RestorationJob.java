package com.cqu.drs.model;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * A restoration / reconstruction work order generated from a
 * {@link DamageAssessment} and assigned to the responsible department.
 */
public class RestorationJob implements Serializable {

    private static final long serialVersionUID = 1L;

    private int jobId;
    private int assessmentId;
    private int reportId;
    private InfrastructureCategory category;
    private int departmentId;
    private String departmentName;
    private String description;
    private int priority;
    private int progress;
    private JobStatus status;
    private int estimatedDays;
    private LocalDateTime createdAt;
    private LocalDateTime lastUpdated;

    public RestorationJob() {
        this.status = JobStatus.PLANNED;
    }

    public int getJobId() {
        return jobId;
    }

    public void setJobId(int jobId) {
        this.jobId = jobId;
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

    public InfrastructureCategory getCategory() {
        return category;
    }

    public void setCategory(InfrastructureCategory category) {
        this.category = category;
    }

    public int getDepartmentId() {
        return departmentId;
    }

    public void setDepartmentId(int departmentId) {
        this.departmentId = departmentId;
    }

    public String getDepartmentName() {
        return departmentName;
    }

    public void setDepartmentName(String departmentName) {
        this.departmentName = departmentName;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public int getPriority() {
        return priority;
    }

    public void setPriority(int priority) {
        this.priority = priority;
    }

    public int getProgress() {
        return progress;
    }

    public void setProgress(int progress) {
        this.progress = progress;
    }

    public JobStatus getStatus() {
        return status;
    }

    public void setStatus(JobStatus status) {
        this.status = status;
    }

    public int getEstimatedDays() {
        return estimatedDays;
    }

    public void setEstimatedDays(int estimatedDays) {
        this.estimatedDays = estimatedDays;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getLastUpdated() {
        return lastUpdated;
    }

    public void setLastUpdated(LocalDateTime lastUpdated) {
        this.lastUpdated = lastUpdated;
    }

    /**
     * Records new progress (0-100) and derives the status from it.
     * @throws IllegalArgumentException if progress is out of range or goes backwards
     */
    public void updateProgress(int newProgress) {
        if (newProgress < 0 || newProgress > 100) {
            throw new IllegalArgumentException("Progress must be between 0 and 100");
        }
        if (newProgress < progress) {
            throw new IllegalArgumentException("Progress cannot go backwards (currently " + progress + "%)");
        }
        this.progress = newProgress;
        this.status = JobStatus.fromProgress(newProgress);
    }

    @Override
    public String toString() {
        return "Job #" + jobId + " " + description + " - " + progress + "% (" + status + ")";
    }
}
