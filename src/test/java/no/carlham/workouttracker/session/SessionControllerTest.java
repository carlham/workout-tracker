package no.carlham.workouttracker.session;

import static no.carlham.workouttracker.security.TestJwt.user;
import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

import no.carlham.workouttracker.security.SecurityConfig;

/**
 * Request validation for sessions. The happy paths are covered end to end in SessionFlowIntegrationTest.
 */
@WebMvcTest(SessionController.class)
@Import(SecurityConfig.class)
class SessionControllerTest {

    @Autowired
    private MockMvcTester mvc;

    @MockitoBean
    private SessionService service;

    @Test
    void setWithBothRepsAndDurationIsRejected() {
        assertThat(postSet("""
                { "exerciseId": 1, "reps": 8, "durationSeconds": 60 }
                """))
                .hasStatus(HttpStatus.BAD_REQUEST)
                .bodyJson()
                .extractingPath("$.errors").asMap().containsKey("repsOrDurationValid");
    }

    @Test
    void setWithNeitherRepsNorDurationIsRejected() {
        assertThat(postSet("""
                { "exerciseId": 1, "weightKg": 60 }
                """))
                .hasStatus(HttpStatus.BAD_REQUEST);
    }

    @Test
    void weightWithMoreThanTwoDecimalsIsRejected() {
        assertThat(postSet("""
                { "exerciseId": 1, "reps": 8, "weightKg": 82.555 }
                """))
                .hasStatus(HttpStatus.BAD_REQUEST)
                .bodyJson()
                .extractingPath("$.errors").asMap().containsKey("weightKg");
    }

    @Test
    void invalidSetInsideNewSessionIsRejected() {
        assertThat(mvc.post().uri("/api/sessions")
                .with(user(1))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        { "sets": [ { "exerciseId": 1, "reps": -3 } ] }
                        """))
                .hasStatus(HttpStatus.BAD_REQUEST)
                .bodyJson()
                .extractingPath("$.errors").asMap().containsKey("sets[0].reps");
    }

    @Test
    void sessionsRequireLogin() {
        assertThat(mvc.get().uri("/api/sessions")).hasStatus(HttpStatus.UNAUTHORIZED);
    }

    private MvcTestResult postSet(String json) {
        return mvc.post().uri("/api/sessions/1/sets")
                .with(user(1))
                .contentType(MediaType.APPLICATION_JSON)
                .content(json)
                .exchange();
    }
}
