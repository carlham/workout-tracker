package no.carlham.workouttracker.template;

import static no.carlham.workouttracker.security.TestJwt.user;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

import no.carlham.workouttracker.exercise.Exercise;
import no.carlham.workouttracker.security.CurrentUser;
import no.carlham.workouttracker.security.SecurityConfig;
import no.carlham.workouttracker.user.Role;

@WebMvcTest(TemplateController.class)
@Import(SecurityConfig.class)
class TemplateControllerTest {

    private static final CurrentUser USER_1 = new CurrentUser(1L, Role.USER);

    @Autowired
    private MockMvcTester mvc;

    @MockitoBean
    private TemplateService service;

    @Test
    void createReturns201WithTemplate() {
        when(service.create(any(TemplateRequest.class), eq(USER_1))).thenReturn(legDay());

        assertThat(mvc.post().uri("/api/templates")
                .with(user(1))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                          "name": "Leg day",
                          "exercises": [
                            { "exerciseId": 1, "targetSets": 3, "targetRepsMin": 8, "targetRepsMax": 12 }
                          ]
                        }
                        """))
                .hasStatus(HttpStatus.CREATED)
                .bodyJson()
                .isLenientlyEqualTo("""
                        {
                          "name": "Leg day",
                          "isPublic": false,
                          "exercises": [
                            { "position": 1, "exerciseName": "Back Squat", "targetSets": 3,
                              "targetRepsMin": 8, "targetRepsMax": 12 }
                          ]
                        }
                        """);
    }

    @Test
    void createRejectsExerciseWithBothRepsAndSeconds() {
        assertThat(mvc.post().uri("/api/templates")
                .with(user(1))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                          "name": "Leg day",
                          "exercises": [
                            { "exerciseId": 1, "targetSets": 3, "targetRepsMin": 8, "targetRepsMax": 12,
                              "targetSeconds": 60 }
                          ]
                        }
                        """))
                .hasStatus(HttpStatus.BAD_REQUEST)
                .bodyJson()
                .extractingPath("$.errors").asMap().containsKey("exercises[0].targetValid");
    }

    @Test
    void createRejectsRepRangeWithMinAboveMax() {
        assertThat(mvc.post().uri("/api/templates")
                .with(user(1))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                          "name": "Leg day",
                          "exercises": [ { "exerciseId": 1, "targetSets": 3, "targetRepsMin": 12, "targetRepsMax": 8 } ]
                        }
                        """))
                .hasStatus(HttpStatus.BAD_REQUEST);
    }

    @Test
    void copyReturns201() {
        when(service.copy(5L, USER_1)).thenReturn(legDay());

        assertThat(mvc.post().uri("/api/templates/5/copy").with(user(1)))
                .hasStatus(HttpStatus.CREATED);
    }

    private static WorkoutTemplate legDay() {
        WorkoutTemplate template = new WorkoutTemplate(1L, "Leg day", null, false, null);
        ReflectionTestUtils.setField(template, "id", 7L);
        template.addExercise(new Exercise("Back Squat", null, null), new ExerciseTarget(3, 8, 12, null, null));
        return template;
    }
}
