package com.cqu.drs.security;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.security.KeyPair;

import static org.junit.jupiter.api.Assertions.*;

/** RSA digital signatures and SHA-256 hashing (non-repudiation). */
class SignatureServiceTest {

    private final KeyPair alice = SignatureService.generateKeyPair();
    private final KeyPair bob = SignatureService.generateKeyPair();

    @Test
    @DisplayName("TC-S09 signature verifies with the signer's public key")
    void signAndVerify() {
        String sig = SignatureService.sign("FIRE|Rockhampton CBD", alice.getPrivate());
        assertTrue(SignatureService.verify("FIRE|Rockhampton CBD", sig, alice.getPublic()));
    }

    @Test
    @DisplayName("TC-S10 changed content or another user's key fails verification")
    void tamperOrWrongSigner() {
        String sig = SignatureService.sign("FIRE|Rockhampton CBD", alice.getPrivate());
        assertFalse(SignatureService.verify("FIRE|Yeppoon", sig, alice.getPublic()));
        assertFalse(SignatureService.verify("FIRE|Rockhampton CBD", sig, bob.getPublic()));
        assertFalse(SignatureService.verify("x", "not-base64!", alice.getPublic()));
    }

    @Test
    @DisplayName("TC-S11 keys survive Base64 encoding (as stored in MySQL)")
    void keyEncoding() {
        var pub = SignatureService.decodePublicKey(SignatureService.encodePublicKey(alice.getPublic()));
        var priv = SignatureService.decodePrivateKey(SignatureService.encodePrivateKey(alice.getPrivate()));
        assertTrue(SignatureService.verify("data", SignatureService.sign("data", priv), pub));
    }

    @Test
    @DisplayName("TC-S12 SHA-256 is deterministic and 64 hex chars")
    void sha256() {
        assertEquals(SignatureService.sha256("abc"), SignatureService.sha256("abc"));
        assertEquals("ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad", SignatureService.sha256("abc"));
    }
}
