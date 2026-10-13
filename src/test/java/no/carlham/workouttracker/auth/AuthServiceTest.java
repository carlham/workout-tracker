package no.carlham.workouttracker.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

import no.carlham.workouttracker.security.AuthProperties;
import no.carlham.workouttracker.user.User;
import no.carlham.workouttracker.user.UserRepository;

/**
 * The refresh rules that are hard to reach through HTTP. The full flow is in AuthFlowIntegrationTest.
 */
class AuthServiceTest {

    private final RefreshTokenRepository refreshTokenRepository = mock(RefreshTokenRepository.class);

    private AuthService service;

    @BeforeEach
    void setUp() {
        AuthProperties properties = new AuthProperties(
                "a-test-secret-that-is-at-least-32-bytes", Duration.ofMinutes(15), Duration.ofDays(14));
        service = new AuthService(mock(UserRepository.class), refreshTokenRepository,
                mock(PasswordEncoder.class), mock(JwtEncoder.class), properties);
    }

    @Test
    void tokenUsedByAnotherRequestAtTheSameMomentCountsAsReuse() {
        // The token looked active when it was read, but another request revoked it first, so the
        // database's "revoke if still active" update changed nothing
        RefreshToken token = activeToken(5L, 42L);
        when(refreshTokenRepository.findByTokenHash(anyString())).thenReturn(Optional.of(token));
        when(refreshTokenRepository.revokeIfActive(eq(5L), any(Instant.class))).thenReturn(0);

        assertThatThrownBy(() -> service.refresh("some-token"))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        e -> assertThat(e.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED));
        verify(refreshTokenRepository).revokeAllForUser(eq(42L), any(Instant.class));
    }

    @Test
    void alreadyRevokedTokenRevokesEverything() {
        RefreshToken token = activeToken(5L, 42L);
        token.revoke(Instant.now());
        when(refreshTokenRepository.findByTokenHash(anyString())).thenReturn(Optional.of(token));

        assertThatThrownBy(() -> service.refresh("some-token")).isInstanceOf(ResponseStatusException.class);
        verify(refreshTokenRepository).revokeAllForUser(eq(42L), any(Instant.class));
    }

    private static RefreshToken activeToken(Long tokenId, Long userId) {
        User user = new User("someone", "not-a-real-hash");
        ReflectionTestUtils.setField(user, "id", userId);
        RefreshToken token = new RefreshToken(user, "hash", Instant.now().plus(Duration.ofDays(1)));
        ReflectionTestUtils.setField(token, "id", tokenId);
        return token;
    }
}
