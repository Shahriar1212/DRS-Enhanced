package com.cqu.drs.server.service;

import com.cqu.drs.model.AuditEntry;
import com.cqu.drs.security.SignatureService;

import java.security.PublicKey;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

/**
 * Tamper-evident audit trail. Each entry is time-stamped, linked to the
 * previous entry by a SHA-256 hash chain, and signed with the acting user's
 * RSA private key, so a user cannot later deny an action (non-repudiation)
 * and any edit/deletion in the table is detected by {@link #verify()}.
 */
public class AuditService {

    /** Serialises appends so the hash chain is never forked by concurrent client threads. */
    private static final Object CHAIN_LOCK = new Object();

    private final ServerContext ctx;

    AuditService(ServerContext ctx) {
        this.ctx = ctx;
    }

    /**
     * Appends a signed entry. Called after the business transaction commits.
     * Audit failures are logged but never undo the completed business action.
     */
    public AuditEntry record(String username, String action, String details) {
        synchronized (CHAIN_LOCK) {
            try (Connection c = ctx.connection()) {
                AuditEntry e = new AuditEntry(LocalDateTime.now().truncatedTo(ChronoUnit.MILLIS),
                        username, action, details);
                e.setPreviousHash(ctx.auditDAO.lastHash(c));
                e.setEntryHash(SignatureService.sha256(e.canonicalContent()));
                e.setSignature(ctx.auth().signAs(username, e.getEntryHash()));
                ctx.auditDAO.insert(c, e);
                return e;
            } catch (SQLException | RuntimeException ex) {
                System.err.println("[AUDIT] failed to record " + action + " by " + username + ": " + ex.getMessage());
                return null;
            }
        }
    }

    /** All entries, newest first, for the Admin screen. */
    public List<AuditEntry> list() throws SQLException {
        try (Connection c = ctx.connection()) {
            List<AuditEntry> all = ctx.auditDAO.findAll(c);
            List<AuditEntry> newestFirst = new ArrayList<>(all);
            java.util.Collections.reverse(newestFirst);
            return newestFirst;
        }
    }

    /**
     * Re-computes the hash chain and checks every signature.
     * @return entries (newest first) with {@code verified} set on each
     */
    public List<AuditEntry> verify() throws SQLException {
        List<AuditEntry> entries;
        try (Connection c = ctx.connection()) {
            entries = ctx.auditDAO.findAll(c);
        }
        String expectedPrev = "0".repeat(64);
        for (AuditEntry e : entries) {
            boolean chainOk = expectedPrev.equals(e.getPreviousHash());
            boolean hashOk = SignatureService.sha256(e.canonicalContent()).equals(e.getEntryHash());
            PublicKey key = ctx.auth().publicKeyOf(e.getUsername());
            boolean sigOk = SignatureService.verify(e.getEntryHash(), e.getSignature(), key);
            e.setVerified(chainOk && hashOk && sigOk);
            expectedPrev = e.getEntryHash();
        }
        List<AuditEntry> newestFirst = new ArrayList<>(entries);
        java.util.Collections.reverse(newestFirst);
        return newestFirst;
    }
}
