package com.cqu.drs.client;

import com.cqu.drs.model.Role;
import com.cqu.drs.model.User;
import com.cqu.drs.protocol.Command;
import com.cqu.drs.security.AccessControl;

/**
 * Holds the client's connection and the logged-in user (application-wide
 * client state shared by all controllers).
 */
public final class ClientSession {

    private static ServerConnection connection;
    private static User currentUser;

    private ClientSession() {
    }

    public static ServerConnection getConnection() {
        return connection;
    }

    public static void setConnection(ServerConnection c) {
        connection = c;
    }

    public static User getUser() {
        return currentUser;
    }

    public static void setUser(User user) {
        currentUser = user;
    }

    public static Role getRole() {
        return currentUser == null ? null : currentUser.getRole();
    }

    /** True when the logged-in user's role may run the command (mirrors the server check). */
    public static boolean can(Command command) {
        return AccessControl.isAllowed(getRole(), command);
    }

    public static void clear() {
        currentUser = null;
        if (connection != null) {
            connection.setSessionToken(null);
        }
    }
}
