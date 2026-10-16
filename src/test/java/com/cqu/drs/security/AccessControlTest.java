package com.cqu.drs.security;

import com.cqu.drs.model.Role;
import com.cqu.drs.protocol.Command;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** Role-based access rights. */
class AccessControlTest {

    @Test
    @DisplayName("TC-S13 public reporter can report but cannot assess or see the audit log")
    void reporterRights() {
        assertTrue(AccessControl.isAllowed(Role.REPORTER, Command.REPORT_DISASTER));
        assertTrue(AccessControl.isAllowed(Role.REPORTER, Command.LIST_ALERTS));
        assertFalse(AccessControl.isAllowed(Role.REPORTER, Command.ASSESS_REPORT));
        assertFalse(AccessControl.isAllowed(Role.REPORTER, Command.LIST_REPORTS));
        assertFalse(AccessControl.isAllowed(Role.REPORTER, Command.LIST_AUDIT_LOG));
    }

    @Test
    @DisplayName("TC-S14 field officer records damage but cannot issue warnings")
    void officerRights() {
        assertTrue(AccessControl.isAllowed(Role.FIELD_OFFICER, Command.RECORD_DAMAGE));
        assertTrue(AccessControl.isAllowed(Role.FIELD_OFFICER, Command.CHECK_IN_EVACUEES));
        assertFalse(AccessControl.isAllowed(Role.FIELD_OFFICER, Command.ISSUE_ALERT));
        assertFalse(AccessControl.isAllowed(Role.FIELD_OFFICER, Command.ASSESS_REPORT));
    }

    @Test
    @DisplayName("TC-S15 roles inherit lower rights; only admin manages users")
    void inheritance() {
        assertTrue(AccessControl.isAllowed(Role.COORDINATOR, Command.RECORD_DAMAGE));
        assertTrue(AccessControl.isAllowed(Role.COORDINATOR, Command.ISSUE_ALERT));
        assertFalse(AccessControl.isAllowed(Role.COORDINATOR, Command.CREATE_USER));
        for (Command c : Command.values()) {
            assertTrue(AccessControl.isAllowed(Role.ADMIN, c), "admin should be allowed " + c);
        }
    }

    @Test
    @DisplayName("TC-S16 without a role only PING/LOGIN/REGISTER are allowed; PII only for coordinator+")
    void anonymousAndPii() {
        assertTrue(AccessControl.isAllowed(null, Command.LOGIN));
        assertFalse(AccessControl.isAllowed(null, Command.REPORT_DISASTER));
        assertFalse(AccessControl.canViewPersonalData(Role.FIELD_OFFICER));
        assertTrue(AccessControl.canViewPersonalData(Role.COORDINATOR));
    }
}
