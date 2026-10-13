package no.carlham.workouttracker.exercise;

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
import no.carlham.workouttracker.musclegroup.MuscleGroupRepository;
import no.carlham.workouttracker.user.User;
import no.carlham.workouttracker.user.UserRepository;

/**
 * Deleting an exercise that templates or sessions use must give a 409, not a 500.
 * Runs against the real database, since it's the database's foreign keys that detect the usage.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class ExerciseDeleteIntegrationTest {

    @Autowired
    private MockMvcTester mvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private MuscleGroupRepository muscleGroupRepository;

    @Test
    void exerciseUsedInSessionCannotBeDeleted() {
        long me = userRepository.save(new User("session-user", "not-a-real-hash")).getId();
        long exerciseId = createExercise(me, "Box squat");

        assertThat(mvc.post().uri("/api/sessions")
                .with(user(me))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        { "sets": [ { "exerciseId": %d, "reps": 5 } ] }
                        """.formatted(exerciseId)))
                .hasStatus(HttpStatus.CREATED);

        assertThat(mvc.delete().uri("/api/exercises/" + exerciseId).with(user(me)))
                .hasStatus(HttpStatus.CONFLICT)
                .bodyJson()
                .extractingPath("$.detail").isEqualTo("Exercise is used in a template or session and can't be deleted");

        // Nothing was deleted
        assertThat(mvc.get().uri("/api/exercises/" + exerciseId).with(user(me))).hasStatusOk();
    }

    @Test
    void exerciseUsedInTemplateCannotBeDeleted() {
        long me = userRepository.save(new User("template-user", "not-a-real-hash")).getId();
        long exerciseId = createExercise(me, "Pause squat");

        assertThat(mvc.post().uri("/api/templates")
                .with(user(me))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                          "name": "Squat focus",
                          "exercises": [ { "exerciseId": %d, "targetSets": 3, "targetRepsMin": 3, "targetRepsMax": 5 } ]
                        }
                        """.formatted(exerciseId)))
                .hasStatus(HttpStatus.CREATED);

        assertThat(mvc.delete().uri("/api/exercises/" + exerciseId).with(user(me)))
                .hasStatus(HttpStatus.CONFLICT);
    }

    @Test
    void unusedExerciseCanStillBeDeleted() {
        long me = userRepository.save(new User("tidy-user", "not-a-real-hash")).getId();
        long exerciseId = createExercise(me, "Never used");

        assertThat(mvc.delete().uri("/api/exercises/" + exerciseId).with(user(me)))
                .hasStatus(HttpStatus.NO_CONTENT);
        assertThat(mvc.get().uri("/api/exercises/" + exerciseId).with(user(me)))
                .hasStatus(HttpStatus.NOT_FOUND);
    }

    private long createExercise(long userId, String name) {
        long quadriceps = muscleGroupRepository.findAll().stream()
                .filter(group -> group.getName().equals("Quadriceps"))
                .findFirst()
                .orElseThrow()
                .getId();

        MvcTestResult result = mvc.post().uri("/api/exercises")
                .with(user(userId))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        { "name": "%s", "muscleGroups": [ { "muscleGroupId": %d, "role": "PRIMARY" } ] }
                        """.formatted(name, quadriceps))
                .exchange();
        assertThat(result).hasStatus(HttpStatus.CREATED);

        try {
            Number id = JsonPath.read(result.getResponse().getContentAsString(), "$.id");
            return id.longValue();
        } catch (UnsupportedEncodingException e) {
            throw new IllegalStateException(e);
        }
    }
}
