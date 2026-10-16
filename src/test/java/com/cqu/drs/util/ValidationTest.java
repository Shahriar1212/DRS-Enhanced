package com.cqu.drs.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** Input validation shared by client and server. */
class ValidationTest {

    @Test
    @DisplayName("TC-V01 valid and invalid phone numbers")
    void phones() {
        assertTrue(Validation.isValidPhone("0400 123 456"));
        assertTrue(Validation.isValidPhone("+61 7 4000 1111"));
        assertFalse(Validation.isValidPhone("12345"));
        assertFalse(Validation.isValidPhone("call me"));
        assertNull(Validation.optionalPhone("  "));
        assertThrows(IllegalArgumentException.class, () -> Validation.optionalPhone("abc"));
    }

    @Test
    @DisplayName("TC-V02 required text is trimmed and length-checked")
    void requiredText() {
        assertEquals("Yeppoon", Validation.requireText("  Yeppoon ", "Location", 20));
        assertThrows(IllegalArgumentException.class, () -> Validation.requireText(" ", "Location", 20));
        assertThrows(IllegalArgumentException.class, () -> Validation.requireText("x".repeat(21), "Location", 20));
    }
}
