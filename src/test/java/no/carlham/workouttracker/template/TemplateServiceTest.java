package no.carlham.workouttracker.template;

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

import no.carlham.workouttracker.exercise.Exercise;
import no.carlham.workouttracker.exercise.ExerciseService;
import no.carlham.workouttracker.security.CurrentUser;
import no.carlham.workouttracker.template.TemplateRequest.TemplateExerciseRequest;
import no.carlham.workouttracker.user.Role;

@ExtendWith(MockitoExtension.class)
class TemplateServiceTest {

    private static final CurrentUser USER_1 = new CurrentUser(1L, Role.USER);
    private static final CurrentUser USER_2 = new CurrentUser(2L, Role.USER);

    private static final Exercise PRESET_SQUAT = new Exercise("Back Squat", null, null);
    private static final Exercise MY_SQUAT = new Exercise("My squat variant", null, 1L);

    @Mock
    private WorkoutTemplateRepository templateRepository;

    @Mock
    private ExerciseService exerciseService;

    @InjectMocks
    private TemplateService service;

    // --- create ---

    @Test
    void createAddsExercisesInOrderWithPositions() {
        when(exerciseService.getById(10L, USER_1)).thenReturn(Optional.of(PRESET_SQUAT));
        when(exerciseService.getById(11L, USER_1)).thenReturn(Optional.of(MY_SQUAT));
        when(templateRepository.save(any(WorkoutTemplate.class))).thenAnswer(invocation -> invocation.getArgument(0));

        WorkoutTemplate created = service.create(request(false, reps(10L), seconds(11L)), USER_1);

        assertThat(created.getOwnerId()).isEqualTo(1L);
        assertThat(created.getExercises())
                .extracting(TemplateExercise::getPosition, item -> item.getExercise().getName())
                .containsExactly(
                        tuple(1, "Back Squat"),
                        tuple(2, "My squat variant"));
    }

    @Test
    void createRejectsExerciseTheUserCannotSee() {
        when(exerciseService.getById(10L, USER_1)).thenReturn(Optional.empty());

        assertStatus(() -> service.create(request(false, reps(10L)), USER_1), HttpStatus.BAD_REQUEST);
        verify(templateRepository, never()).save(any());
    }

    @Test
    void publicTemplateRejectsPrivateExercise() {
        when(exerciseService.getById(11L, USER_1)).thenReturn(Optional.of(MY_SQUAT));

        assertStatus(() -> service.create(request(true, reps(11L)), USER_1), HttpStatus.BAD_REQUEST);
        verify(templateRepository, never()).save(any());
    }

    // --- visibility and ownership ---

    @Test
    void otherUsersPrivateTemplateIsHidden() {
        when(templateRepository.findById(5L)).thenReturn(Optional.of(template(5L, 2L, false)));

        assertThat(service.getById(5L, USER_1)).isEmpty();
    }

    @Test
    void otherUsersPublicTemplateIsVisible() {
        when(templateRepository.findById(5L)).thenReturn(Optional.of(template(5L, 2L, true)));

        assertThat(service.getById(5L, USER_1)).isPresent();
    }

    @Test
    void ownerUpdateReplacesDetailsAndExercises() {
        WorkoutTemplate template = template(5L, 1L, false);
        template.addExercise(PRESET_SQUAT, new ExerciseTarget(3, 8, 12, null, 90));
        when(templateRepository.findById(5L)).thenReturn(Optional.of(template));
        when(exerciseService.getById(11L, USER_1)).thenReturn(Optional.of(MY_SQUAT));
        when(templateRepository.save(template)).thenReturn(template);

        var request = new TemplateRequest("Heavy leg day", "Go heavy", false, List.of(
                new TemplateExerciseRequest(11L, 5, 5, 5, null, 180)));
        WorkoutTemplate updated = service.update(5L, request, USER_1).orElseThrow();

        assertThat(updated.getName()).isEqualTo("Heavy leg day");
        assertThat(updated.getComment()).isEqualTo("Go heavy");
        assertThat(updated.getExercises()).singleElement().satisfies(item -> {
            assertThat(item.getPosition()).isEqualTo(1);
            assertThat(item.getExercise()).isSameAs(MY_SQUAT);
            assertThat(item.getTarget()).isEqualTo(new ExerciseTarget(5, 5, 5, null, 180));
        });
    }

    @Test
    void nonOwnerCannotUpdatePublicTemplate() {
        when(templateRepository.findById(5L)).thenReturn(Optional.of(template(5L, 2L, true)));

        assertStatus(() -> service.update(5L, request(true, reps(10L)), USER_1), HttpStatus.FORBIDDEN);
    }

    @Test
    void nonOwnerCannotDeletePublicTemplate() {
        when(templateRepository.findById(5L)).thenReturn(Optional.of(template(5L, 2L, true)));

        assertStatus(() -> service.deleteById(5L, USER_1), HttpStatus.FORBIDDEN);
        verify(templateRepository, never()).delete(any());
    }

    @Test
    void ownerCanDeleteTemplate() {
        WorkoutTemplate template = template(5L, 1L, false);
        when(templateRepository.findById(5L)).thenReturn(Optional.of(template));

        service.deleteById(5L, USER_1);

        verify(templateRepository).delete(template);
    }

    // --- copy ---

    @Test
    void copyCreatesPrivateTemplateOwnedByCurrentUser() {
        WorkoutTemplate source = template(5L, 2L, true);
        source.addExercise(PRESET_SQUAT, new ExerciseTarget(3, 8, 12, null, 90));
        when(templateRepository.findById(5L)).thenReturn(Optional.of(source));
        when(templateRepository.save(any(WorkoutTemplate.class))).thenAnswer(invocation -> invocation.getArgument(0));

        WorkoutTemplate copy = service.copy(5L, USER_1);

        assertThat(copy.getOwnerId()).isEqualTo(1L);
        assertThat(copy.isPublic()).isFalse();
        assertThat(copy.getCopiedFromId()).isEqualTo(5L);
        assertThat(copy.getExercises()).singleElement()
                .satisfies(item -> assertThat(item.getTarget()).isEqualTo(new ExerciseTarget(3, 8, 12, null, 90)));
    }

    @Test
    void cannotCopyOtherUsersPrivateTemplate() {
        when(templateRepository.findById(5L)).thenReturn(Optional.of(template(5L, 2L, false)));

        assertStatus(() -> service.copy(5L, USER_1), HttpStatus.NOT_FOUND);
        verify(templateRepository, never()).save(any());
    }

    @Test
    void ownerCanCopyOwnPrivateTemplate() {
        when(templateRepository.findById(5L)).thenReturn(Optional.of(template(5L, 2L, false)));
        when(templateRepository.save(any(WorkoutTemplate.class))).thenAnswer(invocation -> invocation.getArgument(0));

        assertThat(service.copy(5L, USER_2).getOwnerId()).isEqualTo(2L);
    }

    private static TemplateRequest request(boolean isPublic, TemplateExerciseRequest... exercises) {
        return new TemplateRequest("Leg day", null, isPublic, List.of(exercises));
    }

    private static TemplateExerciseRequest reps(Long exerciseId) {
        return new TemplateExerciseRequest(exerciseId, 3, 8, 12, null, 90);
    }

    private static TemplateExerciseRequest seconds(Long exerciseId) {
        return new TemplateExerciseRequest(exerciseId, 3, null, null, 60, 60);
    }

    // A template as if loaded from the database
    private static WorkoutTemplate template(Long id, Long ownerId, boolean isPublic) {
        WorkoutTemplate template = new WorkoutTemplate(ownerId, "Leg day", null, isPublic, null);
        ReflectionTestUtils.setField(template, "id", id);
        return template;
    }

    private static void assertStatus(ThrowingCallable call, HttpStatus expected) {
        assertThatThrownBy(call)
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        e -> assertThat(e.getStatusCode()).isEqualTo(expected));
    }
}
