package com.example.crossborder.service;

import com.example.crossborder.config.SecurityProperties;
import java.util.Optional;
import org.springframework.stereotype.Service;

@Service
public class AdminAuthService {
    public static final String DEV_TOKEN = "dev-auth-disabled";
    private final SecurityProperties properties;
    private final JwtTokenService jwt;

    public AdminAuthService(SecurityProperties properties) {
        this.properties = properties;
        this.jwt = new JwtTokenService(properties.jwtSecret(), properties.jwtExpireMinutes());
    }

    public String issue(String username, String role) { return properties.enabled() ? jwt.issue(username, role) : DEV_TOKEN; }
    public boolean enabled() { return properties.enabled(); }
    public boolean authorized(String authorization) { return !properties.enabled() || claims(authorization).isPresent(); }
    public Optional<JwtTokenService.Claims> claims(String authorization) {
        if (authorization == null || !authorization.startsWith("Bearer ")) return Optional.empty();
        return jwt.verify(authorization.substring(7));
    }
}
