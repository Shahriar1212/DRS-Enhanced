package com.cqu.drs.model;

/**
 * Access-control roles of DRS-Enhanced. Each role inherits the rights of the
 * roles before it (REPORTER &lt; FIELD_OFFICER &lt; COORDINATOR &lt; ADMIN);
 * the exact command-to-role mapping lives in
 * {@link com.cqu.drs.security.AccessControl}.
 */
public enum Role {
    /** Member of the public: reports disasters, reads warnings and shelters. */
    REPORTER(1, "Public Reporter"),
    /** Field officer: records damage, checks evacuees into shelters, updates job progress. */
    FIELD_OFFICER(2, "Field Officer"),
    /** Emergency coordinator: assesses, coordinates, issues evacuation warnings. */
    COORDINATOR(3, "Emergency Coordinator"),
    /** System administrator: users, audit log and data import. */
    ADMIN(4, "Administrator");

    private final int level;
    private final String label;

    Role(int level, String label) {
        this.level = level;
        this.label = label;
    }

    public int getLevel() {
        return level;
    }

    public String getLabel() {
        return label;
    }

    /** True when this role has at least the rights of {@code other}. */
    public boolean atLeast(Role other) {
        return this.level >= other.level;
    }

    @Override
    public String toString() {
        return label;
    }
}
