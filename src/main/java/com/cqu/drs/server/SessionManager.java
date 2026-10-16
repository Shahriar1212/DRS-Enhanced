package com.cqu.drs.server;

import com.cqu.drs.model.User;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Keeps track of logged-in users. Tokens are 256-bit random values; a
 * {@link ConcurrentHashMap} gives O(1) thread-safe look-ups from every
 * client-handler thread. Sessions expire after a period of inactivity.
 */
public class SessionManager {

    /** A logged-in user and their activity times. */
    public static class Session {
        private final User user;
        private final LocalDateTime loginTime;
        private volatile LocalDateTime lastSeen;

        Session(User user) {
            this.user = user;
            this.loginTime = LocalDateTime.now();
            this.lastSeen = loginTime;
        }

        public User getUser() {
            return user;
        }

        public LocalDateTime getLoginTime() {
            return loginTime;
        }

        public LocalDateTime getLastSeen() {
            return lastSeen;
        }
    }

    private static final SecureRandom RANDOM = new SecureRandom();

    private final Map<String, Session> sessions = new ConcurrentHashMap<>();
    private final Duration timeout;

    public SessionManager(int timeoutMinutes) {
        this.timeout = Duration.ofMinutes(timeoutMinutes);
    }

    /** Creates a session and returns its token. */
    public String create(User user) {
        byte[] raw = new byte[32];
        RANDOM.nextBytes(raw);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(raw);
        sessions.put(token, new Session(user));
        return token;
    }

    /** Returns the live session for a token (refreshing its activity time), or null if missing/expired. */
    public Session get(String token) {
        if (token == null) {
            return null;
        }
        Session s = sessions.get(token);
        if (s == null) {
            return null;
        }
        if (Duration.between(s.lastSeen, LocalDateTime.now()).compareTo(timeout) > 0) {
            sessions.remove(token);
            return null;
        }
        s.lastSeen = LocalDateTime.now();
        return s;
    }

    public void remove(String token) {
        if (token != null) {
            sessions.remove(token);
        }
    }

    public int activeCount() {
        return sessions.size();
    }
}
