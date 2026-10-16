package com.cqu.drs.util;

import com.cqu.drs.model.DamageAssessment;
import com.cqu.drs.model.DamageLevel;
import com.cqu.drs.model.Department;
import com.cqu.drs.model.DepartmentType;
import com.cqu.drs.model.InfrastructureCategory;
import com.cqu.drs.model.JobStatus;
import com.cqu.drs.model.RecoverySummary;
import com.cqu.drs.model.RestorationJob;

import java.util.List;
import java.util.Optional;

/**
 * Business rules for the "Damage Assessment &amp; Restoration Planning" feature:
 * turns a damage assessment into a prioritised restoration job for the
 * responsible department, and summarises recovery progress.
 */
public final class RestorationPlanner {

    /** Upper bound on the extra priority contributed by the number of people affected. */
    static final int MAX_PEOPLE_BONUS = 20;

    private RestorationPlanner() {
        // utility class - no instances
    }

    /** Priority = damage weight x 20 + min(affected people / 10, 20). Higher = more urgent. */
    public static int priority(DamageLevel level, int affectedPeople) {
        if (level == null) {
            throw new IllegalArgumentException("Damage level is required");
        }
        return level.getWeight() * 20 + Math.min(Math.max(affectedPeople, 0) / 10, MAX_PEOPLE_BONUS);
    }

    /** Estimated duration: base days for the damage level plus one day per $250k of damage (max +60). */
    public static int estimatedDays(DamageLevel level, double estimatedCost) {
        int extra = (int) Math.min(60, Math.max(0, estimatedCost) / 250_000);
        return level.getBaseDays() + extra;
    }

    /** Picks the department responsible for a category (falls back to Fire &amp; Emergency). */
    public static Optional<Department> responsibleDepartment(InfrastructureCategory category,
                                                             List<Department> departments) {
        DepartmentType wanted = category.getResponsibleDepartment();
        Optional<Department> exact = departments.stream().filter(d -> d.getType() == wanted).findFirst();
        if (exact.isPresent()) {
            return exact;
        }
        return departments.stream().filter(d -> d.getType() == DepartmentType.FIRE_AND_EMERGENCY).findFirst();
    }

    /** Short work description, e.g. "Demolish and clear destroyed buildings at Gladstone". */
    public static String describe(DamageAssessment a, String location) {
        String where = location == null ? "" : " at " + location;
        if (a.getCategory() == InfrastructureCategory.BUILDINGS && a.getDamageLevel() == DamageLevel.DESTROYED) {
            return "Demolish and clear destroyed buildings" + where;
        }
        if (a.getCategory() == InfrastructureCategory.DEBRIS_AND_WASTE) {
            return "Remove debris and waste" + where;
        }
        String verb = a.getDamageLevel().getWeight() >= DamageLevel.MAJOR.getWeight() ? "Reconstruct " : "Repair ";
        return verb + a.getCategory().getLabel().toLowerCase() + where;
    }

    /** Builds the restoration job for an assessment (not yet saved, so no id). */
    public static RestorationJob createJob(DamageAssessment assessment, Department department, String location) {
        if (department == null) {
            throw new IllegalArgumentException("A responsible department is required");
        }
        RestorationJob job = new RestorationJob();
        job.setAssessmentId(assessment.getAssessmentId());
        job.setReportId(assessment.getReportId());
        job.setCategory(assessment.getCategory());
        job.setDepartmentId(department.getDepartmentId());
        job.setDepartmentName(department.getName());
        job.setDescription(describe(assessment, location));
        job.setPriority(priority(assessment.getDamageLevel(), assessment.getAffectedPeople()));
        job.setEstimatedDays(estimatedDays(assessment.getDamageLevel(), assessment.getEstimatedCost()));
        job.setProgress(0);
        job.setStatus(JobStatus.PLANNED);
        return job;
    }

    /** Totals the damage and restoration progress for a set of assessments and jobs. */
    public static RecoverySummary summarise(List<DamageAssessment> assessments, List<RestorationJob> jobs) {
        double cost = 0;
        int affected = 0;
        for (DamageAssessment a : assessments) {
            cost += a.getEstimatedCost();
            affected += a.getAffectedPeople();
        }
        int completed = 0;
        double progressSum = 0;
        for (RestorationJob j : jobs) {
            progressSum += j.getProgress();
            if (j.getStatus() == JobStatus.COMPLETED) {
                completed++;
            }
        }
        double avg = jobs.isEmpty() ? 0 : progressSum / jobs.size();
        return new RecoverySummary(cost, affected, jobs.size(), completed, avg);
    }
}
