package no.carlham.workouttracker.security;

import org.springframework.security.oauth2.jwt.Jwt;

import no.carlham.workouttracker.user.Role;

/**
 * The logged-in user, as read from a validated access token.
 */
public record CurrentUser(Long id, Role role) {

    public static CurrentUser from(Jwt jwt) {
        return new CurrentUser(
                Long.valueOf(jwt.getSubject()),
                Role.valueOf(jwt.getClaimAsString(JwtClaims.ROLE)));
    }

    public boolean isAdmin() {
        return role == Role.ADMIN;
    }
}
