package com.example.crossborder.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class JwtTokenServiceTest {
    @Test
    void issueAndVerify_returnsClaimsBeforeExpiry() {
        JwtTokenService service = new JwtTokenService("test-secret", 60);

        String token = service.issue("admin", "admin");

        JwtTokenService.Claims claims = service.verify(token).orElseThrow();
        assertEquals("admin", claims.username());
        assertEquals("admin", claims.role());
    }

    @Test
    void verify_rejectsTamperedToken() {
        JwtTokenService service = new JwtTokenService("test-secret", 60);
        String token = service.issue("admin", "admin");

        assertTrue(service.verify(token + "x").isEmpty());
    }
}
