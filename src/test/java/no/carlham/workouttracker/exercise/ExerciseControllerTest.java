package no.carlham.workouttracker.exercise;

import static no.carlham.workouttracker.security.TestJwt.user;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.web.server.ResponseStatusException;

import no.carlham.workouttracker.musclegroup.MuscleGroup;
import no.carlham.workouttracker.security.CurrentUser;
import no.carlham.workouttracker.security.SecurityConfig;
import no.carlham.workouttracker.user.Role;

@WebMvcTest(ExerciseController.class)
@Import(SecurityConfig.class)
class ExerciseControllerTest {

    // The CurrentUser the controller builds from TestJwt.user(1)
    private static final CurrentUser USER_1 = new CurrentUser(1L, Role.USER);

    @Autowired
    private MockMvcTester mvc;

    @MockitoBean
    private ExerciseService service;

    @Test
    void requestsWithoutTokenAreRejected() {
        assertThat(mvc.get().uri("/api/exercises"))
                .hasStatus(HttpStatus.UNAUTHORIZED)
                .bodyJson()
                .extractingPath("$.detail").isEqualTo("Authentication required");
    }

    @Test
    void getAllReturnsPageOfExercises() {
        when(service.getAll(any(Pageable.class), eq(USER_1)))
                .thenReturn(new PageImpl<>(List.of(exercise(1L, "Squat"))));

        assertThat(mvc.get().uri("/api/exercises").with(user(1)))
                .hasStatusOk()
                .bodyJson()
                .extractingPath("$.content[0].name").isEqualTo("Squat");
    }

    @Test
    void getByIdReturnsExercise() {
        when(service.getById(1L, USER_1)).thenReturn(Optional.of(exercise(1L, "Squat")));

        assertThat(mvc.get().uri("/api/exercises/1").with(user(1)))
                .hasStatusOk()
                .bodyJson()
                .isLenientlyEqualTo("""
                        {
                          "id": 1,
                          "name": "Squat",
                          "preset": true,
                          "muscleGroups": [ { "name": "Quadriceps", "role": "PRIMARY" } ]
                        }
                        """);
    }

    @Test
    void getByIdReturns404WhenMissing() {
        when(service.getById(1L, USER_1)).thenReturn(Optional.empty());

        assertThat(mvc.get().uri("/api/exercises/1").with(user(1))).hasStatus(HttpStatus.NOT_FOUND);
    }

    @Test
    void getByIdRejectsNonPositiveId() {
        assertThat(mvc.get().uri("/api/exercises/0").with(user(1))).hasStatus(HttpStatus.BAD_REQUEST);
    }

    @Test
    void createReturns201WithCreatedExercise() {
        when(service.create(any(CreateExerciseRequest.class), eq(USER_1))).thenReturn(exercise(7L, "Squat"));

        assertThat(mvc.post().uri("/api/exercises")
                .with(user(1))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                          "name": "Squat",
                          "muscleGroups": [ { "muscleGroupId": 6, "role": "PRIMARY" } ]
                        }
                        """))
                .hasStatus(HttpStatus.CREATED)
                .bodyJson()
                .extractingPath("$.id").isEqualTo(7);
    }

    @Test
    void createRejectsInvalidRequest() {
        assertThat(mvc.post().uri("/api/exercises")
                .with(user(1))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        { "name": "", "muscleGroups": [] }
                        """))
                .hasStatus(HttpStatus.BAD_REQUEST)
                .bodyJson()
                .extractingPath("$.errors").asMap().containsKeys("name", "muscleGroups");
    }

    @Test
    void deleteByIdReturns404WhenMissing() {
        doThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "Unknown exercise id 1"))
                .when(service).deleteById(1L, USER_1);

        assertThat(mvc.delete().uri("/api/exercises/1").with(user(1)))
                .hasStatus(HttpStatus.NOT_FOUND)
                .bodyJson()
                .extractingPath("$.detail").isEqualTo("Unknown exercise id 1");
    }

    @Test
    void deleteByIdReturns403WhenNotAllowed() {
        doThrow(new ResponseStatusException(HttpStatus.FORBIDDEN, "You can't modify this exercise"))
                .when(service).deleteById(1L, USER_1);

        assertThat(mvc.delete().uri("/api/exercises/1").with(user(1)))
                .hasStatus(HttpStatus.FORBIDDEN);
    }

    private static Exercise exercise(Long id, String name) {
        Exercise exercise = new Exercise(name, null, null);
        // The id is normally set by the database; set it by hand since nothing is saved here
        ReflectionTestUtils.setField(exercise, "id", id);
        exercise.addMuscleGroup(new MuscleGroup("Quadriceps"), MuscleRole.PRIMARY);
        return exercise;
    }
}
