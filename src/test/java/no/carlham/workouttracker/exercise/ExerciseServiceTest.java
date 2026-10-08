package no.carlham.workouttracker.exercise;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import no.carlham.workouttracker.exercise.CreateExerciseRequest.MuscleGroupLink;
import no.carlham.workouttracker.musclegroup.MuscleGroup;
import no.carlham.workouttracker.musclegroup.MuscleGroupRepository;

@ExtendWith(MockitoExtension.class)
class ExerciseServiceTest {

    @Mock
    private ExerciseRepository exerciseRepository;

    @Mock
    private MuscleGroupRepository muscleGroupRepository;

    @InjectMocks
    private ExerciseService service;

    @Test
    void createLinksRequestedMuscleGroups() {
        when(muscleGroupRepository.findById(1L)).thenReturn(Optional.of(new MuscleGroup("Chest")));
        when(muscleGroupRepository.findById(5L)).thenReturn(Optional.of(new MuscleGroup("Triceps")));
        when(exerciseRepository.save(any(Exercise.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Exercise created = service.create(new CreateExerciseRequest("Bench press", "Flat bench", List.of(
                new MuscleGroupLink(1L, MuscleRole.PRIMARY),
                new MuscleGroupLink(5L, MuscleRole.SECONDARY))));

        assertThat(created.getName()).isEqualTo("Bench press");
        assertThat(created.getMuscleGroups())
                .extracting(link -> link.getMuscleGroup().getName(), ExerciseMuscleGroup::getRole)
                .containsExactly(
                        tuple("Chest", MuscleRole.PRIMARY),
                        tuple("Triceps", MuscleRole.SECONDARY));
    }

    @Test
    void createRejectsDuplicateMuscleGroup() {
        when(muscleGroupRepository.findById(1L)).thenReturn(Optional.of(new MuscleGroup("Chest")));

        var request = new CreateExerciseRequest("Bench press", null, List.of(
                new MuscleGroupLink(1L, MuscleRole.PRIMARY),
                new MuscleGroupLink(1L, MuscleRole.SECONDARY)));

        assertThatThrownBy(() -> service.create(request))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        e -> assertThat(e.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST));
        verify(exerciseRepository, never()).save(any());
    }

    @Test
    void createRejectsUnknownMuscleGroup() {
        when(muscleGroupRepository.findById(99L)).thenReturn(Optional.empty());

        var request = new CreateExerciseRequest("Bench press", null, List.of(
                new MuscleGroupLink(99L, MuscleRole.PRIMARY)));

        assertThatThrownBy(() -> service.create(request))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        e -> assertThat(e.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST));
        verify(exerciseRepository, never()).save(any());
    }

    @Test
    void deleteByIdThrowsNotFoundForUnknownExercise() {
        when(exerciseRepository.findById(42L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.deleteById(42L))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        e -> assertThat(e.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND));
    }
}
