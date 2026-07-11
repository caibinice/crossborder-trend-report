package com.example.crossborder.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.Base64;
import java.util.Optional;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

public class JwtTokenService {
    private static final Base64.Encoder ENCODER = Base64.getUrlEncoder().withoutPadding();
    private static final Base64.Decoder DECODER = Base64.getUrlDecoder();
    private static final ObjectMapper JSON = new ObjectMapper();
    private final byte[] secret;
    private final long expireSeconds;

    public JwtTokenService(String secret, long expireMinutes) {
        this.secret = secret.getBytes(StandardCharsets.UTF_8);
        this.expireSeconds = Math.max(1, expireMinutes) * 60;
    }

    public String issue(String username, String role) {
        long now = Instant.now().getEpochSecond();
        String header = encode("{\"alg\":\"HS256\",\"typ\":\"JWT\"}");
        String payload = encode("{\"sub\":\"" + escape(username) + "\",\"role\":\"" + escape(role) + "\",\"iat\":" + now + ",\"exp\":" + (now + expireSeconds) + "}");
        String signingInput = header + "." + payload;
        return signingInput + "." + sign(signingInput);
    }

    public Optional<Claims> verify(String token) {
        try {
            String[] parts = token == null ? new String[0] : token.split("\\.");
            if (parts.length != 3 || !MessageDigest.isEqual(sign(parts[0] + "." + parts[1]).getBytes(StandardCharsets.UTF_8), parts[2].getBytes(StandardCharsets.UTF_8))) return Optional.empty();
            JsonNode payload = JSON.readTree(new String(DECODER.decode(parts[1]), StandardCharsets.UTF_8));
            if (payload.path("exp").asLong(0) <= Instant.now().getEpochSecond()) return Optional.empty();
            String username = payload.path("sub").asText("");
            return username.isBlank() ? Optional.empty() : Optional.of(new Claims(username, payload.path("role").asText("operator")));
        } catch (Exception ignored) {
            return Optional.empty();
        }
    }

    private String sign(String value) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret, "HmacSHA256"));
            return ENCODER.encodeToString(mac.doFinal(value.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new IllegalStateException("Cannot sign JWT", e);
        }
    }

    private String encode(String value) { return ENCODER.encodeToString(value.getBytes(StandardCharsets.UTF_8)); }
    private String escape(String value) { return value.replace("\\", "\\\\").replace("\"", "\\\""); }

    public record Claims(String username, String role) {}
}
