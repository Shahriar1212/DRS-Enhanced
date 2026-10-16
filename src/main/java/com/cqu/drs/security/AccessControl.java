package com.cqu.drs.security;

import com.cqu.drs.model.Role;
import com.cqu.drs.protocol.Command;

import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;

/**
 * Role-based access control: the minimum {@link Role} required for every
 * {@link Command}. Enforced by the server for every request; the client also
 * uses it to hide screens the user cannot use.
 */
public final class AccessControl {

    /** Commands that may be sent without logging in. */
    private static final Map<Command, Role> MINIMUM_ROLE = new EnumMap<>(Command.class);

    static {
        // Public/any logged-in user
        put(Role.REPORTER, Command.LOGOUT, Command.REPORT_DISASTER, Command.LIST_MY_REPORTS,
                Command.LIST_ALERTS, Command.LIST_SHELTERS);
        // Field officers
        put(Role.FIELD_OFFICER, Command.LIST_REPORTS, Command.LIST_TASKS, Command.DASHBOARD_SUMMARY,
                Command.LIST_DEPARTMENTS, Command.LIST_RESOURCES, Command.LIST_VOLUNTEERS,
                Command.CHECK_IN_EVACUEES, Command.CHECK_OUT_EVACUEES, Command.RECOMMEND_SHELTER,
                Command.RECORD_DAMAGE, Command.LIST_DAMAGE, Command.LIST_RESTORATION_JOBS,
                Command.UPDATE_JOB_PROGRESS, Command.RECOVERY_SUMMARY, Command.UPDATE_TASK_STATUS);
        // Coordinators
        put(Role.COORDINATOR, Command.ASSESS_REPORT, Command.RESOLVE_REPORT, Command.ASSIGN_VOLUNTEER,
                Command.ALLOCATE_RESOURCE, Command.RESTOCK_RESOURCE, Command.REGISTER_VOLUNTEER,
                Command.ADD_SHELTER, Command.ISSUE_ALERT, Command.LIFT_ALERT);
        // Administrators
        put(Role.ADMIN, Command.LIST_USERS, Command.CREATE_USER, Command.SET_USER_ACTIVE,
                Command.LIST_AUDIT_LOG, Command.VERIFY_AUDIT_LOG, Command.IMPORT_LEGACY_DATA);
    }

    private AccessControl() {
    }

    private static void put(Role role, Command... commands) {
        for (Command c : commands) {
            MINIMUM_ROLE.put(c, role);
        }
    }

    /** True for commands allowed without a session (PING, LOGIN, REGISTER_PUBLIC_USER). */
    public static boolean isPublic(Command command) {
        return command == Command.PING || command == Command.LOGIN || command == Command.REGISTER_PUBLIC_USER;
    }

    /** True when {@code role} may execute {@code command}. */
    public static boolean isAllowed(Role role, Command command) {
        if (isPublic(command)) {
            return true;
        }
        if (role == null) {
            return false;
        }
        Role min = MINIMUM_ROLE.get(command);
        return min != null && role.atLeast(min);
    }

    /** True when the role may see decrypted personal data (phone numbers). */
    public static boolean canViewPersonalData(Role role) {
        return role != null && role.atLeast(Role.COORDINATOR);
    }

    /** Read-only view of the permission table (used in the report and Admin screen). */
    public static Map<Command, Role> permissionTable() {
        return Collections.unmodifiableMap(MINIMUM_ROLE);
    }
}
