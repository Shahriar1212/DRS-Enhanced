package com.cqu.drs.security;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Base64;

import static org.junit.jupiter.api.Assertions.*;

/** AES-256-GCM field encryption / decryption. */
class CryptoServiceTest {

    private final CryptoService crypto = CryptoService.fromBase64Key(CryptoService.generateBase64Key());

    @Test
    @DisplayName("TC-S04 encrypt then decrypt returns the original phone number")
    void roundTrip() {
        String cipher = crypto.encrypt("0400 123 456");
        assertNotEquals("0400 123 456", cipher);
        assertFalse(cipher.contains("0400"));
        assertEquals("0400 123 456", crypto.decrypt(cipher));
    }

    @Test
    @DisplayName("TC-S05 same plaintext encrypts differently each time (random IV)")
    void randomIv() {
        assertNotEquals(crypto.encrypt("secret"), crypto.encrypt("secret"));
    }

    @Test
    @DisplayName("TC-S06 tampered ciphertext is detected")
    void tamperDetected() {
        byte[] raw = Base64.getDecoder().decode(crypto.encrypt("0400 123 456"));
        raw[raw.length - 1] ^= 1;
        String tampered = Base64.getEncoder().encodeToString(raw);
        assertThrows(SecurityException.class, () -> crypto.decrypt(tampered));
    }

    @Test
    @DisplayName("TC-S07 wrong key cannot decrypt")
    void wrongKey() {
        CryptoService other = CryptoService.fromBase64Key(CryptoService.generateBase64Key());
        assertThrows(SecurityException.class, () -> other.decrypt(crypto.encrypt("x")));
    }

    @Test
    @DisplayName("TC-S08 phone masking hides the middle digits")
    void mask() {
        assertEquals("04******56", CryptoService.mask("0400 123 456"));
        assertEquals("", CryptoService.mask(null));
        assertNull(crypto.encrypt(null));
    }
}
