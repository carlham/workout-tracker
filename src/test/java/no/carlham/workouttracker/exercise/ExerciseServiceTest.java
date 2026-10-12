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

import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

import no.carlham.workouttracker.exercise.CreateExerciseRequest.MuscleGroupLink;
import no.carlham.workouttracker.musclegroup.MuscleGroup;
import no.carlham.workouttracker.musclegroup.MuscleGroupRepository;
import no.carlham.workouttracker.security.CurrentUser;
import no.carlham.workouttracker.user.Role;

@ExtendWith(MockitoExtension.class)
class ExerciseServiceTest {

    private static final CurrentUser USER_1 = new CurrentUser(1L, Role.USER);
    private static final CurrentUser USER_2 = new CurrentUser(2L, Role.USER);
    private static final CurrentUser ADMIN = new CurrentUser(99L, Role.ADMIN);

    @Mock
    private ExerciseRepository exerciseRepository;

    @Mock
    private MuscleGroupRepository muscleGroupRepository;

    @InjectMocks
    private ExerciseService service;

    // --- create ---

    @Test
    void createLinksRequestedMuscleGroups() {
        when(muscleGroupRepository.findById(1L)).thenReturn(Optional.of(new MuscleGroup("Chest")));
        when(muscleGroupRepository.findById(5L)).thenReturn(Optional.of(new MuscleGroup("Triceps")));
        when(exerciseRepository.save(any(Exercise.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Exercise created = service.create(new CreateExerciseRequest("Bench press", "Flat bench", "Lower to mid-chest",
                List.of(
                        new MuscleGroupLink(1L, MuscleRole.PRIMARY),
                        new MuscleGroupLink(5L, MuscleRole.SECONDARY))), USER_1);

        assertThat(created.getName()).isEqualTo("Bench press");
        assertThat(created.getDescription()).isEqualTo("Lower to mid-chest");
        assertThat(created.getMuscleGroups())
                .extracting(link -> link.getMuscleGroup().getName(), ExerciseMuscleGroup::getRole)
                .containsExactly(
                        tuple("Chest", MuscleRole.PRIMARY),
                        tuple("Triceps", MuscleRole.SECONDARY));
    }

    @Test
    void createByUserIsOwnedByThatUser() {
        when(muscleGroupRepository.findById(1L)).thenReturn(Optional.of(new MuscleGroup("Chest")));
        when(exerciseRepository.save(any(Exercise.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Exercise created = service.create(benchPressRequest(), USER_1);

        assertThat(created.getOwnerId()).isEqualTo(1L);
    }

    @Test
    void createByAdminIsPreset() {
        when(muscleGroupRepository.findById(1L)).thenReturn(Optional.of(new MuscleGroup("Chest")));
        when(exerciseRepository.save(any(Exercise.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Exercise created = service.create(benchPressRequest(), ADMIN);

        assertThat(created.isPreset()).isTrue();
    }

    @Test
    void createRejectsDuplicateMuscleGroup() {
        when(muscleGroupRepository.findById(1L)).thenReturn(Optional.of(new MuscleGroup("Chest")));

        var request = new CreateExerciseRequest("Bench press", null, null, List.of(
                new MuscleGroupLink(1L, MuscleRole.PRIMARY),
                new MuscleGroupLink(1L, MuscleRole.SECONDARY)));

        assertStatus(() -> service.create(request, USER_1), HttpStatus.BAD_REQUEST);
        verify(exerciseRepository, never()).save(any());
    }

    @Test
    void createRejectsUnknownMuscleGroup() {
        when(muscleGroupRepository.findById(99L)).thenReturn(Optional.empty());

        var request = new CreateExerciseRequest("Bench press", null, null, List.of(
                new MuscleGroupLink(99L, MuscleRole.PRIMARY)));

        assertStatus(() -> service.create(request, USER_1), HttpStatus.BAD_REQUEST);
        verify(exerciseRepository, never()).save(any());
    }

    // --- unique names ---

    @Test
    void createRejectsNameAlreadyUsedByOwnExercise() {
        when(exerciseRepository.findByNameIgnoreCase("bench press"))
                .thenReturn(List.of(saved(5L, new Exercise("Bench Press", null, 1L))));

        var request = new CreateExerciseRequest("bench press", null, null, List.of(new MuscleGroupLink(1L, MuscleRole.PRIMARY)));

        assertStatus(() -> service.create(request, USER_1), HttpStatus.CONFLICT);
        verify(exerciseRepository, never()).save(any());
    }

    @Test
    void createAllowsPrivateExerciseWithSameNameAsPreset() {
        when(exerciseRepository.findByNameIgnoreCase("Bench press"))
                .thenReturn(List.of(saved(5L, new Exercise("Bench Press", null, null))));
        when(muscleGroupRepository.findById(1L)).thenReturn(Optional.of(new MuscleGroup("Chest")));
        when(exerciseRepository.save(any(Exercise.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Exercise created = service.create(benchPressRequest(), USER_1);

        assertThat(created.getOwnerId()).isEqualTo(1L);
    }

    @Test
    void createByAdminRejectsDuplicatePresetName() {
        when(exerciseRepository.findByNameIgnoreCase("Bench press"))
                .thenReturn(List.of(saved(5L, new Exercise("Bench Press", null, null))));

        assertStatus(() -> service.create(benchPressRequest(), ADMIN), HttpStatus.CONFLICT);
    }

    @Test
    void updateAllowsKeepingTheSameName() {
        Exercise exercise = saved(10L, new Exercise("Squat", null, 1L));
        when(exerciseRepository.findById(10L)).thenReturn(Optional.of(exercise));
        when(exerciseRepository.findByNameIgnoreCase("squat")).thenReturn(List.of(exercise));
        when(exerciseRepository.save(exercise)).thenReturn(exercise);

        assertThat(service.update(10L, new UpdateExerciseRequest("squat", "lowercase now", null), USER_1)).isPresent();
    }

    @Test
    void updateRejectsRenamingToTakenName() {
        when(exerciseRepository.findById(10L)).thenReturn(Optional.of(saved(10L, new Exercise("Squat", null, 1L))));
        when(exerciseRepository.findByNameIgnoreCase("Deadlift"))
                .thenReturn(List.of(saved(11L, new Exercise("Deadlift", null, 1L))));

        assertStatus(() -> service.update(10L, new UpdateExerciseRequest("Deadlift", null, null), USER_1),
                HttpStatus.CONFLICT);
    }

    // --- visibility ---

    @Test
    void getByIdShowsPresetToAnyUser() {
        when(exerciseRepository.findById(10L)).thenReturn(Optional.of(new Exercise("Squat", null, null)));

        assertThat(service.getById(10L, USER_1)).isPresent();
    }

    @Test
    void getByIdHidesOtherUsersExercise() {
        when(exerciseRepository.findById(10L)).thenReturn(Optional.of(new Exercise("Squat", null, 2L)));

        assertThat(service.getById(10L, USER_1)).isEmpty();
    }

    // --- modifying ---

    @Test
    void ownerCanDeleteOwnExercise() {
        Exercise exercise = new Exercise("Squat", null, 1L);
        when(exerciseRepository.findById(10L)).thenReturn(Optional.of(exercise));

        service.deleteById(10L, USER_1);

        verify(exerciseRepository).delete(exercise);
    }

    @Test
    void userCannotDeletePreset() {
        when(exerciseRepository.findById(10L)).thenReturn(Optional.of(new Exercise("Squat", null, null)));

        assertStatus(() -> service.deleteById(10L, USER_1), HttpStatus.FORBIDDEN);
        verify(exerciseRepository, never()).delete(any());
    }

    @Test
    void adminCanDeletePreset() {
        Exercise preset = new Exercise("Squat", null, null);
        when(exerciseRepository.findById(10L)).thenReturn(Optional.of(preset));

        service.deleteById(10L, ADMIN);

        verify(exerciseRepository).delete(preset);
    }

    @Test
    void deletingOtherUsersExerciseLooksLikeNotFound() {
        when(exerciseRepository.findById(10L)).thenReturn(Optional.of(new Exercise("Squat", null, 1L)));

        assertStatus(() -> service.deleteById(10L, USER_2), HttpStatus.NOT_FOUND);
        verify(exerciseRepository, never()).delete(any());
    }

    @Test
    void deleteByIdThrowsNotFoundForUnknownExercise() {
        when(exerciseRepository.findById(42L)).thenReturn(Optional.empty());

        assertStatus(() -> service.deleteById(42L, USER_1), HttpStatus.NOT_FOUND);
    }

    @Test
    void userCannotUpdatePreset() {
        when(exerciseRepository.findById(10L)).thenReturn(Optional.of(new Exercise("Squat", null, null)));

        assertStatus(() -> service.update(10L, new UpdateExerciseRequest("Renamed", null, null), USER_1),
                HttpStatus.FORBIDDEN);
    }

    private static CreateExerciseRequest benchPressRequest() {
        return new CreateExerciseRequest("Bench press", null, null, List.of(new MuscleGroupLink(1L, MuscleRole.PRIMARY)));
    }

    // Gives an exercise the id the database would have assigned
    private static Exercise saved(Long id, Exercise exercise) {
        ReflectionTestUtils.setField(exercise, "id", id);
        return exercise;
    }

    private static void assertStatus(ThrowingCallable call, HttpStatus expected) {
        assertThatThrownBy(call)
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        e -> assertThat(e.getStatusCode()).isEqualTo(expected));
    }
}
