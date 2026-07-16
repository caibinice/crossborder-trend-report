package com.example.crossborder.service;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class PasswordServiceTest {
    private final PasswordService passwords = new PasswordService();

    @Test
    void hashesPasswordsAndRejectsTheWrongValue() {
        String hash = passwords.hash("correct-horse-battery-staple");

        assertTrue(hash.startsWith("$2"));
        assertTrue(passwords.matches("correct-horse-battery-staple", hash));
        assertFalse(passwords.matches("wrong-password", hash));
    }

    @Test
    void supportsOneTimeUpgradeOfLegacyPlaintextPassword() {
        assertTrue(passwords.matches("admin", "admin"));
        assertTrue(passwords.needsUpgrade("admin"));
        assertFalse(passwords.needsUpgrade(passwords.hash("admin")));
    }
}
