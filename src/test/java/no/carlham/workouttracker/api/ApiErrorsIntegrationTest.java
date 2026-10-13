package no.carlham.workouttracker.api;

import static no.carlham.workouttracker.security.TestJwt.user;
import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

import no.carlham.workouttracker.TestcontainersConfiguration;
import no.carlham.workouttracker.user.User;
import no.carlham.workouttracker.user.UserRepository;

/**
 * Bad requests get a 4xx with a ProblemDetail body, never a 500 or an empty body.
 * These go through the whole app, since several of the errors come from Spring or the database.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class ApiErrorsIntegrationTest {

    @Autowired
    private MockMvcTester mvc;

    @Autowired
    private UserRepository userRepository;

    private long userId;

    @BeforeEach
    void createUser() {
        userId = userRepository.save(new User("errors-" + System.nanoTime(), "not-a-real-hash")).getId();
    }

    @Test
    void unknownSortPropertyIsBadRequest() {
        // Swagger UI fills in "string" as the sort value, which used to cause a 500
        for (String path : new String[] {"/api/exercises", "/api/templates", "/api/templates/public", "/api/sessions"}) {
            assertThat(mvc.get().uri(path + "?sort=string").with(user(userId)))
                    .as(path)
                    .hasStatus(HttpStatus.BAD_REQUEST)
                    .bodyJson()
                    .extractingPath("$.detail").isEqualTo("Unknown property 'string'");
        }
    }

    @Test
    void missingResourceHasProblemDetailBody() {
        assertThat(mvc.get().uri("/api/exercises/999999").with(user(userId)))
                .hasStatus(HttpStatus.NOT_FOUND)
                .bodyJson()
                .extractingPath("$.detail").isEqualTo("Unknown exercise id 999999");
    }

    @Test
    void malformedJsonHasProblemDetailBody() {
        assertThat(mvc.post().uri("/api/sessions")
                .with(user(userId))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{ not json"))
                .hasStatus(HttpStatus.BAD_REQUEST)
                .bodyJson()
                // "title" is part of ProblemDetail; Spring's default error format has "error" instead
                .extractingPath("$.title").isEqualTo("Bad Request");
    }

    @Test
    void wrongTypeInPathHasProblemDetailBody() {
        assertThat(mvc.get().uri("/api/exercises/abc").with(user(userId)))
                .hasStatus(HttpStatus.BAD_REQUEST)
                .bodyJson()
                // "title" is part of ProblemDetail; Spring's default error format has "error" instead
                .extractingPath("$.title").isEqualTo("Bad Request");
    }

    @Test
    void nullMuscleGroupLinkIsBadRequest() {
        assertThat(mvc.post().uri("/api/exercises")
                .with(user(userId))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        { "name": "Null link", "muscleGroups": [ null ] }
                        """))
                .hasStatus(HttpStatus.BAD_REQUEST)
                .bodyJson()
                .extractingPath("$.errors").asMap().containsKey("muscleGroups[0]");
    }

    @Test
    void passwordOver72BytesIsBadRequest() {
        // 40 characters, but æ takes two bytes in UTF-8, so 80 bytes: more than BCrypt accepts
        assertThat(mvc.post().uri("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        { "username": "norwegian-%d", "password": "%s" }
                        """.formatted(System.nanoTime(), "æ".repeat(40))))
                .hasStatus(HttpStatus.BAD_REQUEST)
                .bodyJson()
                .extractingPath("$.errors").asMap().containsKey("passwordWithinLimit");
    }

    @Test
    void authEndpointsIgnoreStaleAccessToken() {
        // A frontend may still send its expired access token; the public auth endpoints should ignore it
        assertThat(mvc.post().uri("/api/auth/register")
                .header(HttpHeaders.AUTHORIZATION, "Bearer expired-or-garbage")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        { "username": "stale-token-%d", "password": "correct-horse-battery" }
                        """.formatted(System.nanoTime())))
                .hasStatus(HttpStatus.CREATED);
    }
}
