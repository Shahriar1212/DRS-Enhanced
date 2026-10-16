package com.cqu.drs.protocol;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

/**
 * A message sent from the JavaFX client to the server over a TCP socket
 * (Java object serialisation). Carries the command, the session token
 * returned by LOGIN, and named parameters.
 */
public class Request implements Serializable {

    private static final long serialVersionUID = 1L;

    private final Command command;
    private String sessionToken;
    private final HashMap<String, Serializable> params = new HashMap<>();

    public Request(Command command) {
        this.command = command;
    }

    public Command getCommand() {
        return command;
    }

    public String getSessionToken() {
        return sessionToken;
    }

    public void setSessionToken(String sessionToken) {
        this.sessionToken = sessionToken;
    }

    /** Adds a parameter and returns this request (fluent style). */
    public Request with(String key, Serializable value) {
        params.put(key, value);
        return this;
    }

    public Map<String, Serializable> getParams() {
        return params;
    }

    public Serializable get(String key) {
        return params.get(key);
    }

    public String getString(String key) {
        Object v = params.get(key);
        return v == null ? null : v.toString();
    }

    /**
     * Returns an int parameter.
     * @throws IllegalArgumentException if it is missing or not a number
     */
    public int getInt(String key) {
        Object v = params.get(key);
        if (v instanceof Number) {
            return ((Number) v).intValue();
        }
        if (v == null) {
            throw new IllegalArgumentException("Missing parameter: " + key);
        }
        try {
            return Integer.parseInt(v.toString().trim());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(key + " must be a whole number");
        }
    }

    /** Returns a double parameter (accepts numbers or numeric strings). */
    public double getDouble(String key) {
        Object v = params.get(key);
        if (v instanceof Number) {
            return ((Number) v).doubleValue();
        }
        if (v == null) {
            throw new IllegalArgumentException("Missing parameter: " + key);
        }
        try {
            return Double.parseDouble(v.toString().trim().replace(",", "").replace("$", ""));
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(key + " must be a number");
        }
    }

    @Override
    public String toString() {
        return "Request{" + command + ", params=" + params.keySet() + "}";
    }
}
