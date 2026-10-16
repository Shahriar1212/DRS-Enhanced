package com.cqu.drs.security;

import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.MessageDigest;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Signature;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

/**
 * RSA-2048 / SHA256withRSA digital signatures and SHA-256 hashing used for
 * non-repudiation. Every user has their own key pair; the server signs each
 * action with the acting user's private key, and anyone can verify it with
 * the user's public key.
 */
public final class SignatureService {

    private static final String ALGORITHM = "SHA256withRSA";

    private SignatureService() {
    }

    /** Generates a new RSA-2048 key pair. */
    public static KeyPair generateKeyPair() {
        try {
            KeyPairGenerator gen = KeyPairGenerator.getInstance("RSA");
            gen.initialize(2048);
            return gen.generateKeyPair();
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("RSA not available", e);
        }
    }

    /** Signs UTF-8 text; returns a Base64 signature. */
    public static String sign(String data, PrivateKey privateKey) {
        try {
            Signature s = Signature.getInstance(ALGORITHM);
            s.initSign(privateKey);
            s.update(data.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(s.sign());
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("Signing failed", e);
        }
    }

    /** Verifies a Base64 signature; returns false on any mismatch or malformed input. */
    public static boolean verify(String data, String base64Signature, PublicKey publicKey) {
        if (data == null || base64Signature == null || publicKey == null) {
            return false;
        }
        try {
            Signature s = Signature.getInstance(ALGORITHM);
            s.initVerify(publicKey);
            s.update(data.getBytes(StandardCharsets.UTF_8));
            return s.verify(Base64.getDecoder().decode(base64Signature));
        } catch (GeneralSecurityException | IllegalArgumentException e) {
            return false;
        }
    }

    /** Hex SHA-256 digest of UTF-8 text. */
    public static String sha256(String data) {
        try {
            byte[] d = MessageDigest.getInstance("SHA-256").digest(data.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : d) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }

    public static String encodePublicKey(PublicKey key) {
        return Base64.getEncoder().encodeToString(key.getEncoded());
    }

    public static String encodePrivateKey(PrivateKey key) {
        return Base64.getEncoder().encodeToString(key.getEncoded());
    }

    public static PublicKey decodePublicKey(String base64) {
        try {
            return KeyFactory.getInstance("RSA").generatePublic(new X509EncodedKeySpec(Base64.getDecoder().decode(base64)));
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("Invalid public key", e);
        }
    }

    public static PrivateKey decodePrivateKey(String base64) {
        try {
            return KeyFactory.getInstance("RSA").generatePrivate(new PKCS8EncodedKeySpec(Base64.getDecoder().decode(base64)));
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("Invalid private key", e);
        }
    }
}
