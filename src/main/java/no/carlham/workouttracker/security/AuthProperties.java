package no.carlham.workouttracker.security;

import java.nio.charset.StandardCharsets;
import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Settings under {@code app.auth.*} in application.properties.
 */
@ConfigurationProperties("app.auth")
public record AuthProperties(String jwtSecret, Duration accessTokenTtl, Duration refreshTokenTtl) {

    public AuthProperties {
        // HS256 requires a key of at least 256 bits; fail at startup rather than at first login
        if (jwtSecret == null || jwtSecret.getBytes(StandardCharsets.UTF_8).length < 32) {
            throw new IllegalArgumentException("app.auth.jwt-secret must be at least 32 bytes long");
        }
    }
}
