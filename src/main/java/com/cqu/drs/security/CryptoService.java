package com.cqu.drs.security;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * AES-256-GCM encryption/decryption of sensitive fields (phone numbers and
 * private signing keys) before they are written to MySQL. GCM also
 * authenticates the ciphertext, so tampered values fail to decrypt.
 * Output format: Base64(12-byte IV || ciphertext+tag).
 */
public class CryptoService {

    private static final String TRANSFORMATION = "AES/GCM/NoPadding";
    private static final int IV_BYTES = 12;
    private static final int TAG_BITS = 128;
    private static final SecureRandom RANDOM = new SecureRandom();

    private final SecretKey key;

    public CryptoService(SecretKey key) {
        this.key = key;
    }

    /** Creates a service from a Base64-encoded 256-bit key. */
    public static CryptoService fromBase64Key(String base64Key) {
        byte[] raw = Base64.getDecoder().decode(base64Key.trim());
        return new CryptoService(new SecretKeySpec(raw, "AES"));
    }

    /** Generates a new random 256-bit AES key, Base64 encoded. */
    public static String generateBase64Key() {
        try {
            KeyGenerator gen = KeyGenerator.getInstance("AES");
            gen.init(256);
            return Base64.getEncoder().encodeToString(gen.generateKey().getEncoded());
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("AES not available", e);
        }
    }

    /**
     * Loads the master key from {@code keyFile}, creating it on first run.
     * In production the key would live in a hardware security module / keystore.
     */
    public static CryptoService loadOrCreate(Path keyFile) {
        try {
            if (!Files.exists(keyFile)) {
                if (keyFile.getParent() != null) {
                    Files.createDirectories(keyFile.getParent());
                }
                Files.writeString(keyFile, generateBase64Key(), StandardCharsets.US_ASCII);
            }
            return fromBase64Key(Files.readString(keyFile, StandardCharsets.US_ASCII));
        } catch (java.io.IOException e) {
            throw new IllegalStateException("Cannot read/create master key " + keyFile.toAbsolutePath(), e);
        }
    }

    /** Encrypts text; returns null for null input. */
    public String encrypt(String plainText) {
        if (plainText == null) {
            return null;
        }
        try {
            byte[] iv = new byte[IV_BYTES];
            RANDOM.nextBytes(iv);
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.ENCRYPT_MODE, key, new GCMParameterSpec(TAG_BITS, iv));
            byte[] cipherText = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));
            byte[] out = new byte[iv.length + cipherText.length];
            System.arraycopy(iv, 0, out, 0, iv.length);
            System.arraycopy(cipherText, 0, out, iv.length, cipherText.length);
            return Base64.getEncoder().encodeToString(out);
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("Encryption failed", e);
        }
    }

    /**
     * Decrypts text produced by {@link #encrypt(String)}; returns null for null input.
     * @throws SecurityException if the value was tampered with or the key is wrong
     */
    public String decrypt(String encoded) {
        if (encoded == null) {
            return null;
        }
        try {
            byte[] in = Base64.getDecoder().decode(encoded);
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.DECRYPT_MODE, key, new GCMParameterSpec(TAG_BITS, in, 0, IV_BYTES));
            byte[] plain = cipher.doFinal(in, IV_BYTES, in.length - IV_BYTES);
            return new String(plain, StandardCharsets.UTF_8);
        } catch (GeneralSecurityException | IllegalArgumentException e) {
            throw new SecurityException("Decryption failed - data tampered or wrong key", e);
        }
    }

    /** Masks a phone number for users without the right to see personal data, e.g. 04******56. */
    public static String mask(String phone) {
        if (phone == null || phone.isBlank()) {
            return "";
        }
        String p = phone.replaceAll("\\s", "");
        if (p.length() <= 4) {
            return "****";
        }
        return p.substring(0, 2) + "*".repeat(p.length() - 4) + p.substring(p.length() - 2);
    }
}
