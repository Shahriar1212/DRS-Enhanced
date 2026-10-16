package com.cqu.drs.protocol;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * The server's reply to a {@link Request}: success flag, human-readable
 * message, optional payload and a server timestamp.
 */
public class Response implements Serializable {

    private static final long serialVersionUID = 1L;

    private final boolean success;
    private final String message;
    private final Serializable data;
    private final LocalDateTime serverTime;

    private Response(boolean success, String message, Serializable data) {
        this.success = success;
        this.message = message;
        this.data = data;
        this.serverTime = LocalDateTime.now();
    }

    public static Response ok(String message, Serializable data) {
        return new Response(true, message, data);
    }

    public static Response ok(String message) {
        return new Response(true, message, null);
    }

    public static Response error(String message) {
        return new Response(false, message, null);
    }

    public boolean isSuccess() {
        return success;
    }

    public String getMessage() {
        return message;
    }

    public Serializable getData() {
        return data;
    }

    public LocalDateTime getServerTime() {
        return serverTime;
    }

    @Override
    public String toString() {
        return "Response{success=" + success + ", message='" + message + "'}";
    }
}
