package com.cqu.drs.security;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** Password hashing (PBKDF2) and password policy. */
class PasswordHasherTest {

    @Test
    @DisplayName("TC-S01 correct password verifies, wrong password does not")
    void verify() {
        String hash = PasswordHasher.hash("Coord@123");
        assertTrue(PasswordHasher.verify("Coord@123", hash));
        assertFalse(PasswordHasher.verify("coord@123", hash));
        assertFalse(hash.contains("Coord@123"));
    }

    @Test
    @DisplayName("TC-S02 same password hashes differently (random salt)")
    void salted() {
        assertNotEquals(PasswordHasher.hash("Admin@123"), PasswordHasher.hash("Admin@123"));
    }

    @Test
    @DisplayName("TC-S03 weak passwords are rejected by the policy")
    void policy() {
        assertNotNull(PasswordHasher.checkPolicy("short"));
        assertNotNull(PasswordHasher.checkPolicy("password123"));
        assertNull(PasswordHasher.checkPolicy("Strong#2026"));
    }
}
