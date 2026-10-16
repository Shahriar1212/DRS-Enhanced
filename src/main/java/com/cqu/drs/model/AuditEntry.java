package com.cqu.drs.model;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * One time-stamped, hash-chained and digitally signed entry in the audit log.
 * Provides non-repudiation: every change is attributable to the signing user,
 * and any later tampering breaks the hash chain or the signature.
 */
public class AuditEntry implements Serializable {

    private static final long serialVersionUID = 1L;

    private long auditId;
    private LocalDateTime timestamp;
    private String username;
    private String action;
    private String details;
    private String previousHash;
    private String entryHash;
    private String signature;
    private Boolean verified;

    public AuditEntry() {
    }

    public AuditEntry(LocalDateTime timestamp, String username, String action, String details) {
        this.timestamp = timestamp;
        this.username = username;
        this.action = action;
        this.details = details;
    }

    public long getAuditId() {
        return auditId;
    }

    public void setAuditId(long auditId) {
        this.auditId = auditId;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public String getDetails() {
        return details;
    }

    public void setDetails(String details) {
        this.details = details;
    }

    public String getPreviousHash() {
        return previousHash;
    }

    public void setPreviousHash(String previousHash) {
        this.previousHash = previousHash;
    }

    public String getEntryHash() {
        return entryHash;
    }

    public void setEntryHash(String entryHash) {
        this.entryHash = entryHash;
    }

    public String getSignature() {
        return signature;
    }

    public void setSignature(String signature) {
        this.signature = signature;
    }

    /** Null = not yet verified, true/false = result of the last verification. */
    public Boolean getVerified() {
        return verified;
    }

    public void setVerified(Boolean verified) {
        this.verified = verified;
    }

    /** Text the entry hash is computed over (previous hash links the chain). */
    public String canonicalContent() {
        return previousHash + "|" + timestamp + "|" + username + "|" + action + "|" + details;
    }

    @Override
    public String toString() {
        return timestamp + " " + username + " " + action + ": " + details;
    }
}
