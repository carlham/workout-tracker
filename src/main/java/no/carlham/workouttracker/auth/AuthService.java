package no.carlham.workouttracker.auth;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;

import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import no.carlham.workouttracker.security.AuthProperties;
import no.carlham.workouttracker.security.JwtClaims;
import no.carlham.workouttracker.user.User;
import no.carlham.workouttracker.user.UserRepository;
import no.carlham.workouttracker.user.UserResponse;

@Service
public class AuthService {
    private static final SecureRandom RANDOM = new SecureRandom();

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtEncoder jwtEncoder;
    private final AuthProperties properties;

    public AuthService(UserRepository userRepository,
        RefreshTokenRepository refreshTokenRepository,
        PasswordEncoder passwordEncoder,
        JwtEncoder jwtEncoder,
        AuthProperties properties
    ) {
        this.userRepository = userRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtEncoder = jwtEncoder;
        this.properties = properties;
    }

    /** An access token for the response body and a refresh token for the cookie. */
    public record Tokens(String accessToken, String refreshToken) {}

    @Transactional
    public UserResponse register(RegisterRequest request) {
        if (userRepository.existsByUsernameIgnoreCase(request.username())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Username is already taken");
        }

        User user = new User(request.username(), passwordEncoder.encode(request.password()));
        return UserResponse.from(userRepository.save(user));
    }

    @Transactional
    public Tokens login(LoginRequest request) {
        // Same error for unknown user and wrong password, so the response doesn't reveal which usernames exist
        User user = userRepository.findByUsernameIgnoreCase(request.username())
            .filter(found -> passwordEncoder.matches(request.password(), found.getPasswordHash()))
            .orElseThrow(() -> unauthorized("Invalid username or password"));

        return issueTokens(user);
    }

    // noRollbackFor: when a reused token is detected we revoke all the user's tokens and then throw.
    // Without this, the exception would roll back the revocation.
    @Transactional(noRollbackFor = ResponseStatusException.class)
    public Tokens refresh(String refreshToken) {
        RefreshToken current = refreshTokenRepository.findByTokenHash(hash(refreshToken))
            .orElseThrow(() -> unauthorized("Invalid refresh token"));
        Instant now = Instant.now();

        if (current.isRevoked()) {
            throw reuseDetected(current, now);
        }
        if (current.isExpired(now)) {
            throw unauthorized("Refresh token has expired");
        }

        // Revoke in the database, only if it's still active. If two requests use the same token at the
        // same moment, both pass the check above, but only one of these updates changes a row.
        if (refreshTokenRepository.revokeIfActive(current.getId(), now) == 0) {
            throw reuseDetected(current, now);
        }
        return issueTokens(current.getUser());
    }

    // Each refresh token is single-use. Seeing one again means it was probably stolen,
    // so log the user out everywhere.
    private ResponseStatusException reuseDetected(RefreshToken token, Instant now) {
        refreshTokenRepository.revokeAllForUser(token.getUser().getId(), now);
        return unauthorized("Invalid refresh token");
    }

    @Transactional
    public void logout(String refreshToken) {
        refreshTokenRepository.findByTokenHash(hash(refreshToken))
            .filter(token -> !token.isRevoked())
            .ifPresent(token -> token.revoke(Instant.now()));
    }

    private Tokens issueTokens(User user) {
        Instant now = Instant.now();

        JwtClaimsSet claims = JwtClaimsSet.builder()
            .subject(user.getId().toString())
            .issuedAt(now)
            .expiresAt(now.plus(properties.accessTokenTtl()))
            .claim(JwtClaims.USERNAME, user.getUsername())
            .claim(JwtClaims.ROLE, user.getRole().name())
            .build();
        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
        String accessToken = jwtEncoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();

        byte[] randomBytes = new byte[32];
        RANDOM.nextBytes(randomBytes);
        String refreshToken = Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);
        refreshTokenRepository.save(
            new RefreshToken(user, hash(refreshToken), now.plus(properties.refreshTokenTtl())));

        return new Tokens(accessToken, refreshToken);
    }

    // SHA-256 is enough here (no BCrypt needed): the token is 32 random bytes, so it can't be guessed
    private static String hash(String token) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is not available", e);
        }
    }

    private static ResponseStatusException unauthorized(String message) {
        return new ResponseStatusException(HttpStatus.UNAUTHORIZED, message);
    }
}
