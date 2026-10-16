package com.cqu.drs.server.service;

import com.cqu.drs.model.Role;
import com.cqu.drs.model.User;
import com.cqu.drs.security.PasswordHasher;
import com.cqu.drs.security.SignatureService;
import com.cqu.drs.server.db.UserDAO;

import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Authentication and user management: PBKDF2 password checks, account lock-out
 * after repeated failures, and a per-user RSA key pair (private key stored
 * AES-encrypted) used to sign that user's actions.
 */
public class AuthService {

    /** Failed logins allowed before an account is locked. */
    public static final int MAX_FAILED_ATTEMPTS = 5;

    private final ServerContext ctx;
    private final Map<String, PrivateKey> privateKeyCache = new ConcurrentHashMap<>();
    private final Map<String, PublicKey> publicKeyCache = new ConcurrentHashMap<>();

    AuthService(ServerContext ctx) {
        this.ctx = ctx;
    }

    /**
     * Checks credentials.
     * @return the user on success
     * @throws SecurityException with a user-friendly reason on failure
     */
    public User login(String username, String password) throws SQLException {
        if (username == null || username.isBlank() || password == null || password.isEmpty()) {
            throw new SecurityException("Enter a username and password");
        }
        try (Connection c = ctx.connection()) {
            UserDAO.UserRecord rec = ctx.userDAO.findByUsername(c, username.trim());
            if (rec == null) {
                throw new SecurityException("Invalid username or password");
            }
            if (!rec.user.isActive()) {
                throw new SecurityException("Account is disabled - contact the administrator");
            }
            if (rec.failedAttempts >= MAX_FAILED_ATTEMPTS) {
                throw new SecurityException("Account locked after " + MAX_FAILED_ATTEMPTS
                        + " failed attempts - contact the administrator");
            }
            if (!PasswordHasher.verify(password, rec.passwordHash)) {
                ctx.userDAO.setFailedAttempts(c, rec.user.getUsername(), rec.failedAttempts + 1);
                int left = MAX_FAILED_ATTEMPTS - rec.failedAttempts - 1;
                throw new SecurityException("Invalid username or password (" + Math.max(left, 0) + " attempts left)");
            }
            if (rec.failedAttempts > 0) {
                ctx.userDAO.setFailedAttempts(c, rec.user.getUsername(), 0);
            }
            return rec.user;
        }
    }

    /** Creates a user with a hashed password and a fresh signing key pair. */
    public User createUser(String username, String fullName, String password, Role role) throws SQLException {
        if (username == null || !username.matches("[A-Za-z0-9_.]{3,30}")) {
            throw new IllegalArgumentException("Username must be 3-30 letters, digits, '.' or '_'");
        }
        if (fullName == null || fullName.isBlank()) {
            throw new IllegalArgumentException("Full name is required");
        }
        if (role == null) {
            throw new IllegalArgumentException("Role is required");
        }
        String policy = PasswordHasher.checkPolicy(password);
        if (policy != null) {
            throw new IllegalArgumentException(policy);
        }
        try (Connection c = ctx.connection()) {
            if (ctx.userDAO.findByUsername(c, username) != null) {
                throw new IllegalArgumentException("Username '" + username + "' is already taken");
            }
            KeyPair keys = SignatureService.generateKeyPair();
            String publicKey = SignatureService.encodePublicKey(keys.getPublic());
            String privateEnc = ctx.getCrypto().encrypt(SignatureService.encodePrivateKey(keys.getPrivate()));
            User u = new User(username, fullName.trim(), role);
            ctx.userDAO.insert(c, u, PasswordHasher.hash(password), publicKey, privateEnc);
            return u;
        }
    }

    public List<User> listUsers() throws SQLException {
        try (Connection c = ctx.connection()) {
            return ctx.userDAO.findAll(c);
        }
    }

    /** Enables/disables an account (also clears a lock-out). */
    public void setActive(int userId, boolean active) throws SQLException {
        try (Connection c = ctx.connection()) {
            if (!ctx.userDAO.setActive(c, userId, active)) {
                throw new IllegalArgumentException("User #" + userId + " not found");
            }
        }
    }

    public int countUsers() throws SQLException {
        try (Connection c = ctx.connection()) {
            return ctx.userDAO.count(c);
        }
    }

    /** Decrypts (and caches) a user's private signing key. */
    public PrivateKey privateKeyOf(String username) throws SQLException {
        PrivateKey cached = privateKeyCache.get(username);
        if (cached != null) {
            return cached;
        }
        try (Connection c = ctx.connection()) {
            UserDAO.UserRecord rec = ctx.userDAO.findByUsername(c, username);
            if (rec == null) {
                throw new IllegalArgumentException("Unknown user " + username);
            }
            PrivateKey key = SignatureService.decodePrivateKey(ctx.getCrypto().decrypt(rec.privateKeyEnc));
            privateKeyCache.put(username, key);
            return key;
        }
    }

    /** Returns (and caches) a user's public verification key, or null if the user does not exist. */
    public PublicKey publicKeyOf(String username) throws SQLException {
        PublicKey cached = publicKeyCache.get(username);
        if (cached != null) {
            return cached;
        }
        try (Connection c = ctx.connection()) {
            UserDAO.UserRecord rec = ctx.userDAO.findByUsername(c, username);
            if (rec == null) {
                return null;
            }
            PublicKey key = SignatureService.decodePublicKey(rec.publicKey);
            publicKeyCache.put(username, key);
            return key;
        }
    }

    /** Signs text with a user's private key (non-repudiation). */
    public String signAs(String username, String data) throws SQLException {
        return SignatureService.sign(data, privateKeyOf(username));
    }
}
