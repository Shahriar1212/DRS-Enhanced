package com.cqu.drs.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * An external department/organisation the DRS coordinates with
 * (fire &amp; emergency, hospital, electricity, transportation, waste management,
 * water supply, schools, law enforcement). Holds its own relief {@link Resource}s.
 */
public class Department implements Serializable {

    private static final long serialVersionUID = 1L;

    private int departmentId;
    private String name;
    private DepartmentType type;
    private String contactInfo;
    private final List<Resource> resources;

    public Department() {
        this.resources = new ArrayList<>();
    }

    public Department(String name, DepartmentType type, String contactInfo) {
        this();
        this.name = name;
        this.type = type;
        this.contactInfo = contactInfo;
    }

    public int getDepartmentId() {
        return departmentId;
    }

    public void setDepartmentId(int departmentId) {
        this.departmentId = departmentId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public DepartmentType getType() {
        return type;
    }

    public void setType(DepartmentType type) {
        this.type = type;
    }

    public String getContactInfo() {
        return contactInfo;
    }

    public void setContactInfo(String contactInfo) {
        this.contactInfo = contactInfo;
    }

    public List<Resource> getResources() {
        return resources;
    }

    public void addResource(Resource resource) {
        resources.add(resource);
        resource.setDepartmentId(departmentId);
        resource.setDepartmentName(name);
    }

    @Override
    public String toString() {
        return name + " (" + type + ")";
    }
}
