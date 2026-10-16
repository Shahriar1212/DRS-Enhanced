package com.cqu.drs.util;

/**
 * Input validation shared by the client (immediate feedback) and the server
 * (authoritative check - the server never trusts client input).
 */
public final class Validation {

    private Validation() {
    }

    public static boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }

    /**
     * Returns the trimmed text.
     * @throws IllegalArgumentException if blank or longer than {@code maxLength}
     */
    public static String requireText(String value, String fieldName, int maxLength) {
        if (isBlank(value)) {
            throw new IllegalArgumentException(fieldName + " is required");
        }
        String t = value.trim();
        if (t.length() > maxLength) {
            throw new IllegalArgumentException(fieldName + " must be at most " + maxLength + " characters");
        }
        return t;
    }

    /** Optional text: null/blank becomes null, otherwise trimmed and length-checked. */
    public static String optionalText(String value, String fieldName, int maxLength) {
        if (isBlank(value)) {
            return null;
        }
        return requireText(value, fieldName, maxLength);
    }

    /** Australian-style phone check: 8-15 digits, optional leading +, spaces allowed. */
    public static boolean isValidPhone(String phone) {
        if (isBlank(phone)) {
            return false;
        }
        String p = phone.replaceAll("[\\s()-]", "");
        return p.matches("\\+?\\d{8,15}");
    }

    /**
     * Validates an optional phone number.
     * @return trimmed phone or null when blank
     * @throws IllegalArgumentException when present but invalid
     */
    public static String optionalPhone(String phone) {
        if (isBlank(phone)) {
            return null;
        }
        if (!isValidPhone(phone)) {
            throw new IllegalArgumentException("Phone number must contain 8-15 digits");
        }
        return phone.trim();
    }

    /**
     * Requires a positive whole number.
     * @throws IllegalArgumentException if not positive
     */
    public static int requirePositive(int value, String fieldName) {
        if (value <= 0) {
            throw new IllegalArgumentException(fieldName + " must be greater than zero");
        }
        return value;
    }
}
