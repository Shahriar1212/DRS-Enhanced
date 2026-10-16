package com.cqu.drs.util;

import com.cqu.drs.model.*;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Domain service implementing the "Coordinate Response" use case: given an
 * assessed {@link DisasterReport}, works out which departments must be
 * involved and creates the corresponding {@link ResponseTask}s.
 */
public final class CoordinationService {

    /** Which department types are typically involved for each disaster type. */
    private static final Map<DisasterType, Set<DepartmentType>> RESPONSE_MAP = new EnumMap<>(DisasterType.class);

    static {
        RESPONSE_MAP.put(DisasterType.HURRICANE, EnumSet.of(
                DepartmentType.FIRE_AND_EMERGENCY, DepartmentType.HOSPITAL, DepartmentType.ELECTRICITY,
                DepartmentType.TRANSPORTATION, DepartmentType.WATER_SUPPLY, DepartmentType.WASTE_MANAGEMENT));
        RESPONSE_MAP.put(DisasterType.FIRE, EnumSet.of(
                DepartmentType.FIRE_AND_EMERGENCY, DepartmentType.HOSPITAL, DepartmentType.LAW_ENFORCEMENT));
        RESPONSE_MAP.put(DisasterType.EARTHQUAKE, EnumSet.of(
                DepartmentType.FIRE_AND_EMERGENCY, DepartmentType.HOSPITAL, DepartmentType.ELECTRICITY,
                DepartmentType.TRANSPORTATION, DepartmentType.LAW_ENFORCEMENT, DepartmentType.WASTE_MANAGEMENT));
        RESPONSE_MAP.put(DisasterType.FLOOD, EnumSet.of(
                DepartmentType.FIRE_AND_EMERGENCY, DepartmentType.HOSPITAL, DepartmentType.WATER_SUPPLY,
                DepartmentType.TRANSPORTATION));
        RESPONSE_MAP.put(DisasterType.LANDSLIDE, EnumSet.of(
                DepartmentType.FIRE_AND_EMERGENCY, DepartmentType.TRANSPORTATION, DepartmentType.LAW_ENFORCEMENT));
        RESPONSE_MAP.put(DisasterType.OTHER, EnumSet.of(DepartmentType.FIRE_AND_EMERGENCY, DepartmentType.LAW_ENFORCEMENT));
    }

    private CoordinationService() {
    }

    public static Set<DepartmentType> relevantDepartmentTypes(DisasterType type) {
        return RESPONSE_MAP.getOrDefault(type, EnumSet.of(DepartmentType.FIRE_AND_EMERGENCY));
    }

    /**
     * Creates a {@link ResponseTask} for every relevant department (that exists in
     * {@code availableDepartments}) for the given assessed disaster report.
     *
     * @param report               a report that has already been assessed (severity set)
     * @param availableDepartments the departments registered in the system
     * @return the list of newly created response tasks
     */
    public static List<ResponseTask> coordinateResponse(DisasterReport report, List<Department> availableDepartments) {
        if (report.getSeverity() == null) {
            throw new IllegalStateException("Report must be assessed (severity set) before coordination");
        }
        Set<DepartmentType> needed = relevantDepartmentTypes(report.getType());
        List<ResponseTask> created = new ArrayList<>();

        for (Department dept : availableDepartments) {
            if (needed.contains(dept.getType())) {
                String desc = dept.getType() + " response for " + report.getType() + " at " + report.getLocation();
                created.add(new ResponseTask(desc, report.getPriorityScore(), report, dept));
            }
        }
        report.setStatus(Status.IN_PROGRESS);
        return created;
    }
}
