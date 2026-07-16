package com.example.crossborder.service;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class PasswordService {
    private static final BCryptPasswordEncoder ENCODER = new BCryptPasswordEncoder();

    public String hash(String rawPassword) {
        if (rawPassword == null || rawPassword.isBlank()) {
            throw new ApiValidationException("密码不能为空");
        }
        return ENCODER.encode(rawPassword);
    }

    public boolean matches(String rawPassword, String storedPassword) {
        if (rawPassword == null || storedPassword == null) {
            return false;
        }
        if (isBcrypt(storedPassword)) {
            return ENCODER.matches(rawPassword, storedPassword);
        }
        // Existing development databases contain legacy plaintext values. A successful
        // login upgrades them immediately; new and updated users are always hashed.
        return constantTimeEquals(rawPassword, storedPassword);
    }

    public boolean needsUpgrade(String storedPassword) {
        return !isBcrypt(storedPassword);
    }

    private boolean isBcrypt(String value) {
        return value != null && value.matches("^\\$2[aby]\\$.{56}$");
    }

    private boolean constantTimeEquals(String first, String second) {
        byte[] a = first.getBytes(java.nio.charset.StandardCharsets.UTF_8);
        byte[] b = second.getBytes(java.nio.charset.StandardCharsets.UTF_8);
        return java.security.MessageDigest.isEqual(a, b);
    }
}
