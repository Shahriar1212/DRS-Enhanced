package com.cqu.drs.protocol;

/**
 * Every operation a client can ask the multi-threaded server to perform.
 * The server checks each command against the caller's role in
 * {@link com.cqu.drs.security.AccessControl} before executing it.
 */
public enum Command {
    // --- session ---
    PING,
    LOGIN,
    LOGOUT,
    REGISTER_PUBLIC_USER,

    // --- reporting & assessment (DRS-Initial features, now persistent) ---
    REPORT_DISASTER,
    LIST_MY_REPORTS,
    LIST_REPORTS,
    ASSESS_REPORT,
    RESOLVE_REPORT,
    DASHBOARD_SUMMARY,

    // --- coordination ---
    LIST_TASKS,
    UPDATE_TASK_STATUS,
    ASSIGN_VOLUNTEER,
    LIST_DEPARTMENTS,
    ALLOCATE_RESOURCE,
    RESTOCK_RESOURCE,
    LIST_RESOURCES,

    // --- volunteers ---
    REGISTER_VOLUNTEER,
    LIST_VOLUNTEERS,

    // --- new feature 1: evacuation warnings & shelters ---
    LIST_SHELTERS,
    ADD_SHELTER,
    CHECK_IN_EVACUEES,
    CHECK_OUT_EVACUEES,
    RECOMMEND_SHELTER,
    ISSUE_ALERT,
    LIFT_ALERT,
    LIST_ALERTS,

    // --- new feature 2: damage assessment & restoration ---
    RECORD_DAMAGE,
    LIST_DAMAGE,
    LIST_RESTORATION_JOBS,
    UPDATE_JOB_PROGRESS,
    RECOVERY_SUMMARY,

    // --- administration & security ---
    LIST_USERS,
    CREATE_USER,
    SET_USER_ACTIVE,
    LIST_AUDIT_LOG,
    VERIFY_AUDIT_LOG,
    IMPORT_LEGACY_DATA
}
