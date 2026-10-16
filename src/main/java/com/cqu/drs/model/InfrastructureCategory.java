package com.cqu.drs.model;

/**
 * Categories of infrastructure that can be damaged, each mapped to the
 * department responsible for restoring it (case study: assessment, restoration
 * and demolition of buildings, debris removal, land and maritime infrastructure
 * repairs, public health services).
 */
public enum InfrastructureCategory {
    BUILDINGS("Buildings (assessment/demolition)", DepartmentType.FIRE_AND_EMERGENCY),
    ROADS_AND_BRIDGES("Roads & bridges", DepartmentType.TRANSPORTATION),
    MARITIME("Maritime infrastructure", DepartmentType.TRANSPORTATION),
    POWER("Power network", DepartmentType.ELECTRICITY),
    WATER_SUPPLY("Water & sewerage", DepartmentType.WATER_SUPPLY),
    DEBRIS_AND_WASTE("Debris & waste removal", DepartmentType.WASTE_MANAGEMENT),
    SCHOOLS("Schools", DepartmentType.SCHOOLS),
    PUBLIC_HEALTH("Public health services", DepartmentType.HOSPITAL);

    private final String label;
    private final DepartmentType responsibleDepartment;

    InfrastructureCategory(String label, DepartmentType responsibleDepartment) {
        this.label = label;
        this.responsibleDepartment = responsibleDepartment;
    }

    public String getLabel() {
        return label;
    }

    public DepartmentType getResponsibleDepartment() {
        return responsibleDepartment;
    }

    @Override
    public String toString() {
        return label;
    }
}
