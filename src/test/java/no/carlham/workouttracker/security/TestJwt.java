package no.carlham.workouttracker.security;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;

import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.JwtRequestPostProcessor;

import no.carlham.workouttracker.user.Role;

/**
 * Fake logged-in users for web layer tests, e.g. {@code mvc.get().uri(...).with(TestJwt.user(1))}.
 * The request is treated as if it carried a valid access token for that user.
 */
public final class TestJwt {

    private TestJwt() {
    }

    public static JwtRequestPostProcessor user(long id) {
        return as(id, Role.USER);
    }

    public static JwtRequestPostProcessor admin(long id) {
        return as(id, Role.ADMIN);
    }

    private static JwtRequestPostProcessor as(long id, Role role) {
        return jwt()
                .jwt(token -> token.subject(String.valueOf(id)).claim(JwtClaims.ROLE, role.name()))
                .authorities(new SimpleGrantedAuthority("ROLE_" + role.name()));
    }
}
