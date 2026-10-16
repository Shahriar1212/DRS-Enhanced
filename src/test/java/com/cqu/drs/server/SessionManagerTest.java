package com.cqu.drs.server;

import com.cqu.drs.model.Role;
import com.cqu.drs.model.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** Session tokens issued at login. */
class SessionManagerTest {

    @Test
    @DisplayName("TC-S17 tokens are unique and resolve to the user until logout")
    void tokens() {
        SessionManager sm = new SessionManager(30);
        User u = new User("coord", "Emma", Role.COORDINATOR);
        String t1 = sm.create(u);
        String t2 = sm.create(u);
        assertNotEquals(t1, t2);
        assertSame(u, sm.get(t1).getUser());
        sm.remove(t1);
        assertNull(sm.get(t1));
        assertNull(sm.get("forged-token"));
        assertEquals(1, sm.activeCount());
    }

    @Test
    @DisplayName("TC-S18 sessions expire after the inactivity timeout")
    void expiry() {
        SessionManager sm = new SessionManager(0);
        String t = sm.create(new User("a", "A", Role.ADMIN));
        try {
            Thread.sleep(20);
        } catch (InterruptedException ignored) {
            Thread.currentThread().interrupt();
        }
        assertNull(sm.get(t));
    }
}
