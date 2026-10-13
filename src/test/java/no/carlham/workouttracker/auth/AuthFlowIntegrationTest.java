package no.carlham.workouttracker.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.UnsupportedEncodingException;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpHeaders;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

import com.jayway.jsonpath.JsonPath;

import jakarta.servlet.http.Cookie;
import no.carlham.workouttracker.TestcontainersConfiguration;
import no.carlham.workouttracker.user.User;
import no.carlham.workouttracker.user.UserRepository;

/**
 * Runs the whole auth flow through the real app and a real database: no mocks.
 * The database is shared between tests here, so each test uses its own username.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class AuthFlowIntegrationTest {

    private static final String PASSWORD = "correct-horse-battery";

    @Autowired
    private MockMvcTester mvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void loginGivesAccessTokenAndHttpOnlyRefreshCookie() {
        register("alice");
        MvcTestResult login = login("alice", PASSWORD);

        assertThat(login).hasStatusOk();
        Cookie cookie = login.getResponse().getCookie(AuthController.REFRESH_COOKIE);
        assertThat(cookie).isNotNull();
        assertThat(cookie.isHttpOnly()).isTrue();
        assertThat(cookie.getSecure()).isTrue();

        assertThat(mvc.get().uri("/api/users/me").header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken(login)))
                .hasStatusOk()
                .bodyJson()
                .extractingPath("$.username").isEqualTo("alice");
    }

    @Test
    void refreshRotatesTokenAndReuseRevokesEverything() {
        register("bob");
        String firstRefresh = refreshToken(login("bob", PASSWORD));

        MvcTestResult refreshed = refresh(firstRefresh);
        assertThat(refreshed).hasStatusOk();
        String secondRefresh = refreshToken(refreshed);
        assertThat(secondRefresh).isNotEqualTo(firstRefresh);

        // Reusing a spent token looks like theft: it's rejected, and the newest token is revoked too
        assertThat(refresh(firstRefresh)).hasStatus(HttpStatus.UNAUTHORIZED);
        assertThat(refresh(secondRefresh)).hasStatus(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void logoutRevokesRefreshToken() {
        register("carol");
        String refreshToken = refreshToken(login("carol", PASSWORD));

        assertThat(mvc.post().uri("/api/auth/logout").cookie(new Cookie(AuthController.REFRESH_COOKIE, refreshToken)))
                .hasStatus(HttpStatus.NO_CONTENT);

        assertThat(refresh(refreshToken)).hasStatus(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void loginFailsWithWrongPassword() {
        register("dave");

        assertThat(login("dave", "wrong-password"))
                .hasStatus(HttpStatus.UNAUTHORIZED)
                .bodyJson()
                .extractingPath("$.detail").isEqualTo("Invalid username or password");
    }

    @Test
    void registerRejectsTakenUsernameIgnoringCase() {
        register("erin");

        assertThat(registerRequest("ERIN")).hasStatus(HttpStatus.CONFLICT);
    }

    @Test
    void invalidAccessTokenIsRejected() {
        assertThat(mvc.get().uri("/api/users/me").header(HttpHeaders.AUTHORIZATION, "Bearer not-a-real-token"))
                .hasStatus(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void loginIgnoresCaseOfUsername() {
        register("MixedCase");

        assertThat(login("mixedcase", PASSWORD)).hasStatusOk();
    }

    @Test
    void databaseRejectsUsernamesDifferingOnlyInCase() {
        // Backstop for two registrations racing past the service's check
        userRepository.saveAndFlush(new User("Twin", "not-a-real-hash"));

        assertThatThrownBy(() -> userRepository.saveAndFlush(new User("twin", "not-a-real-hash")))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void roleClaimInRealTokenControlsAdminAccess() {
        // Other tests fake admin tokens; this checks a real token's "role" claim becomes ROLE_ADMIN
        register("real-admin");
        jdbcTemplate.update("UPDATE users SET role = 'ADMIN' WHERE username = 'real-admin'");
        register("real-user");

        assertThat(createMuscleGroup(accessToken(login("real-user", PASSWORD)), "Forearms"))
                .hasStatus(HttpStatus.FORBIDDEN);
        assertThat(createMuscleGroup(accessToken(login("real-admin", PASSWORD)), "Forearms"))
                .hasStatus(HttpStatus.CREATED);
    }

    private MvcTestResult createMuscleGroup(String accessToken, String name) {
        return mvc.post().uri("/api/muscle-groups")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        { "name": "%s" }
                        """.formatted(name))
                .exchange();
    }

    private void register(String username) {
        assertThat(registerRequest(username)).hasStatus(HttpStatus.CREATED);
    }

    private MvcTestResult registerRequest(String username) {
        return mvc.post().uri("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        { "username": "%s", "password": "%s" }
                        """.formatted(username, PASSWORD))
                .exchange();
    }

    private MvcTestResult login(String username, String password) {
        return mvc.post().uri("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        { "username": "%s", "password": "%s" }
                        """.formatted(username, password))
                .exchange();
    }

    private MvcTestResult refresh(String refreshToken) {
        return mvc.post().uri("/api/auth/refresh")
                .cookie(new Cookie(AuthController.REFRESH_COOKIE, refreshToken))
                .exchange();
    }

    private static String refreshToken(MvcTestResult result) {
        return result.getResponse().getCookie(AuthController.REFRESH_COOKIE).getValue();
    }

    private static String accessToken(MvcTestResult result) {
        try {
            return JsonPath.read(result.getResponse().getContentAsString(), "$.accessToken");
        } catch (UnsupportedEncodingException e) {
            throw new IllegalStateException(e);
        }
    }
}
