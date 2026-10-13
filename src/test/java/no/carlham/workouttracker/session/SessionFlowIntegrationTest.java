package no.carlham.workouttracker.session;

import static no.carlham.workouttracker.security.TestJwt.user;
import static org.assertj.core.api.Assertions.assertThat;

import java.io.UnsupportedEncodingException;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

import com.jayway.jsonpath.JsonPath;

import no.carlham.workouttracker.TestcontainersConfiguration;
import no.carlham.workouttracker.exercise.Exercise;
import no.carlham.workouttracker.exercise.ExerciseRepository;
import no.carlham.workouttracker.user.User;
import no.carlham.workouttracker.user.UserRepository;

/**
 * Plans and logs a workout through the real app and database: no mocks.
 * Users are created directly in the database, and requests carry a test token for them.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class SessionFlowIntegrationTest {

    @Autowired
    private MockMvcTester mvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ExerciseRepository exerciseRepository;

    @Test
    void logWorkoutFromTemplate() {
        long me = userRepository.save(new User("lifter", "not-a-real-hash")).getId();
        long squat = presetId("Back Squat");
        long plank = presetId("Plank");

        long templateId = idOf(mvc.post().uri("/api/templates")
                .with(user(me))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                          "name": "Leg day",
                          "exercises": [
                            { "exerciseId": %d, "targetSets": 3, "targetRepsMin": 5, "targetRepsMax": 8 },
                            { "exerciseId": %d, "targetSets": 2, "targetSeconds": 60 }
                          ]
                        }
                        """.formatted(squat, plank))
                .exchange());

        // Start the session from the template, then log sets one at a time like in the gym
        long sessionId = idOf(mvc.post().uri("/api/sessions")
                .with(user(me))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        { "templateId": %d }
                        """.formatted(templateId))
                .exchange());

        long firstSetId = logSet(me, sessionId, """
                { "exerciseId": %d, "reps": 5, "weightKg": 80 }
                """.formatted(squat));
        logSet(me, sessionId, """
                { "exerciseId": %d, "reps": 5, "weightKg": 82.5 }
                """.formatted(squat));
        logSet(me, sessionId, """
                { "exerciseId": %d, "durationSeconds": 60 }
                """.formatted(plank));

        // The id returned when logging a set can be used straight away to fix it
        assertThat(mvc.put().uri("/api/sessions/" + sessionId + "/sets/" + firstSetId)
                .with(user(me))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        { "reps": 5, "weightKg": 82.5 }
                        """))
                .hasStatusOk();

        assertThat(mvc.get().uri("/api/sessions/" + sessionId).with(user(me)))
                .hasStatusOk()
                .bodyJson()
                .isLenientlyEqualTo("""
                        {
                          "name": "Leg day",
                          "templateId": %d,
                          "sets": [
                            { "exerciseName": "Back Squat", "setNumber": 1, "reps": 5, "weightKg": 82.5 },
                            { "exerciseName": "Back Squat", "setNumber": 2, "reps": 5, "weightKg": 82.5 },
                            { "exerciseName": "Plank", "setNumber": 1, "durationSeconds": 60 }
                          ]
                        }
                        """.formatted(templateId));

        // Deleting the template keeps the history; the session just loses its link
        assertThat(mvc.delete().uri("/api/templates/" + templateId).with(user(me)))
                .hasStatus(HttpStatus.NO_CONTENT);
        assertThat(mvc.get().uri("/api/sessions/" + sessionId).with(user(me)))
                .hasStatusOk()
                .bodyJson()
                .extractingPath("$.templateId").isNull();
    }

    @Test
    void sessionsArePrivate() {
        long owner = userRepository.save(new User("private-lifter", "not-a-real-hash")).getId();
        long someoneElse = userRepository.save(new User("nosy", "not-a-real-hash")).getId();

        long sessionId = idOf(mvc.post().uri("/api/sessions")
                .with(user(owner))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        { "name": "Secret workout" }
                        """)
                .exchange());

        assertThat(mvc.get().uri("/api/sessions/" + sessionId).with(user(someoneElse)))
                .hasStatus(HttpStatus.NOT_FOUND);
        assertThat(mvc.get().uri("/api/sessions").with(user(someoneElse)))
                .hasStatusOk()
                .bodyJson()
                .extractingPath("$.content").asArray().isEmpty();
    }

    private long logSet(long userId, long sessionId, String json) {
        return idOf(mvc.post().uri("/api/sessions/" + sessionId + "/sets")
                .with(user(userId))
                .contentType(MediaType.APPLICATION_JSON)
                .content(json)
                .exchange());
    }

    private long presetId(String name) {
        return exerciseRepository.findByNameIgnoreCase(name).stream()
                .filter(Exercise::isPreset)
                .findFirst()
                .orElseThrow()
                .getId();
    }

    private static long idOf(MvcTestResult result) {
        assertThat(result).hasStatus(HttpStatus.CREATED);
        try {
            Number id = JsonPath.read(result.getResponse().getContentAsString(), "$.id");
            assertThat(id).as("id in response").isNotNull();
            return id.longValue();
        } catch (UnsupportedEncodingException e) {
            throw new IllegalStateException(e);
        }
    }
}
