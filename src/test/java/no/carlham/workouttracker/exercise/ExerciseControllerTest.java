package no.carlham.workouttracker.exercise;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.web.server.ResponseStatusException;

import no.carlham.workouttracker.musclegroup.MuscleGroup;

@WebMvcTest(ExerciseController.class)
class ExerciseControllerTest {

    @Autowired
    private MockMvcTester mvc;

    @MockitoBean
    private ExerciseService service;

    @Test
    void getAllReturnsPageOfExercises() {
        when(service.getAll(any(Pageable.class))).thenReturn(new PageImpl<>(List.of(exercise(1L, "Squat"))));

        assertThat(mvc.get().uri("/api/exercises"))
                .hasStatusOk()
                .bodyJson()
                .extractingPath("$.content[0].name").isEqualTo("Squat");
    }

    @Test
    void getByIdReturnsExercise() {
        when(service.getById(1L)).thenReturn(Optional.of(exercise(1L, "Squat")));

        assertThat(mvc.get().uri("/api/exercises/1"))
                .hasStatusOk()
                .bodyJson()
                .isLenientlyEqualTo("""
                        {
                          "id": 1,
                          "name": "Squat",
                          "muscleGroups": [ { "name": "Quadriceps", "role": "PRIMARY" } ]
                        }
                        """);
    }

    @Test
    void getByIdReturns404WhenMissing() {
        when(service.getById(1L)).thenReturn(Optional.empty());

        assertThat(mvc.get().uri("/api/exercises/1")).hasStatus(HttpStatus.NOT_FOUND);
    }

    @Test
    void getByIdRejectsNonPositiveId() {
        assertThat(mvc.get().uri("/api/exercises/0")).hasStatus(HttpStatus.BAD_REQUEST);
    }

    @Test
    void createReturns201WithCreatedExercise() {
        when(service.create(any(CreateExerciseRequest.class))).thenReturn(exercise(7L, "Squat"));

        assertThat(mvc.post().uri("/api/exercises")
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
                .when(service).deleteById(1L);

        assertThat(mvc.delete().uri("/api/exercises/1"))
                .hasStatus(HttpStatus.NOT_FOUND)
                .bodyJson()
                .extractingPath("$.detail").isEqualTo("Unknown exercise id 1");
    }

    private static Exercise exercise(Long id, String name) {
        Exercise exercise = new Exercise(name, null);
        // The id is normally set by the database; set it by hand since nothing is saved here
        ReflectionTestUtils.setField(exercise, "id", id);
        exercise.addMuscleGroup(new MuscleGroup("Quadriceps"), MuscleRole.PRIMARY);
        return exercise;
    }
}
