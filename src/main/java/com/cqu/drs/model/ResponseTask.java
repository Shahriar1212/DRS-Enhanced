package com.cqu.drs.model;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * A unit of coordinated work created for a {@link DisasterReport} and carried
 * out by a {@link Department}, optionally with one {@link Volunteer} assigned.
 */
public class ResponseTask implements Serializable {

    private static final long serialVersionUID = 1L;

    private int taskId;
    private String description;
    private Status status;
    private int priority;
    private DisasterReport disasterReport;
    private Department department;
    private Volunteer assignedVolunteer;
    private LocalDateTime createdAt;
    private LocalDateTime lastUpdated;

    public ResponseTask() {
        this.status = Status.ASSIGNED;
    }

    public ResponseTask(String description, int priority, DisasterReport disasterReport, Department department) {
        this();
        this.description = description;
        this.priority = priority;
        this.disasterReport = disasterReport;
        this.department = department;
        this.assignedVolunteer = null;
    }

    public int getTaskId() {
        return taskId;
    }

    public void setTaskId(int taskId) {
        this.taskId = taskId;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    public int getPriority() {
        return priority;
    }

    public void setPriority(int priority) {
        this.priority = priority;
    }

    public DisasterReport getDisasterReport() {
        return disasterReport;
    }

    public void setDisasterReport(DisasterReport disasterReport) {
        this.disasterReport = disasterReport;
    }

    public Department getDepartment() {
        return department;
    }

    public void setDepartment(Department department) {
        this.department = department;
    }

    public Volunteer getAssignedVolunteer() {
        return assignedVolunteer;
    }

    public void setAssignedVolunteer(Volunteer assignedVolunteer) {
        this.assignedVolunteer = assignedVolunteer;
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

    /** Name of the department carrying out the task (for table columns). */
    public String getDepartmentName() {
        return department == null ? "" : department.getName();
    }

    /** Name of the assigned volunteer, or an empty string. */
    public String getVolunteerName() {
        return assignedVolunteer == null ? "" : assignedVolunteer.getName();
    }

    /** Assigns an available volunteer to help with this task. */
    public void assignVolunteer(Volunteer volunteer) {
        if (volunteer == null) {
            throw new IllegalArgumentException("Volunteer is required");
        }
        if (!volunteer.isAvailable()) {
            throw new IllegalStateException(volunteer.getName() + " is not available");
        }
        this.assignedVolunteer = volunteer;
        volunteer.setAvailable(false);
        this.status = Status.IN_PROGRESS;
    }

    @Override
    public String toString() {
        return "Task #" + taskId + " - " + description + " [" + status + "]";
    }
}
