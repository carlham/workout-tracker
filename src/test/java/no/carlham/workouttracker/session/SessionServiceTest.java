package no.carlham.workouttracker.session;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Instant;
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
import no.carlham.workouttracker.template.TemplateService;
import no.carlham.workouttracker.template.WorkoutTemplate;
import no.carlham.workouttracker.user.Role;

@ExtendWith(MockitoExtension.class)
class SessionServiceTest {

    private static final CurrentUser USER_1 = new CurrentUser(1L, Role.USER);
    private static final CurrentUser USER_2 = new CurrentUser(2L, Role.USER);

    private static final Exercise BENCH = withId(10L, new Exercise("Barbell Bench Press", null, null));
    private static final Exercise SQUAT = withId(11L, new Exercise("Back Squat", null, null));
    private static final Exercise PLANK = withId(12L, new Exercise("Plank", null, null));

    @Mock
    private WorkoutSessionRepository sessionRepository;

    @Mock
    private ExerciseService exerciseService;

    @Mock
    private TemplateService templateService;

    @InjectMocks
    private SessionService service;

    // --- create ---

    @Test
    void createNumbersSetsPerExercise() {
        when(exerciseService.getById(10L, USER_1)).thenReturn(Optional.of(BENCH));
        when(exerciseService.getById(11L, USER_1)).thenReturn(Optional.of(SQUAT));
        when(sessionRepository.save(any(WorkoutSession.class))).thenAnswer(invocation -> invocation.getArgument(0));

        WorkoutSession session = service.create(request(null, List.of(
                reps(10L), reps(10L), reps(11L), reps(10L))), USER_1);

        assertThat(session.getSets())
                .extracting(set -> set.getExercise().getName(), SessionSet::getSetNumber)
                .containsExactly(
                        tuple("Barbell Bench Press", 1),
                        tuple("Barbell Bench Press", 2),
                        tuple("Back Squat", 1),
                        tuple("Barbell Bench Press", 3));
    }

    @Test
    void createStoresTimedSetAndWeight() {
        when(exerciseService.getById(10L, USER_1)).thenReturn(Optional.of(BENCH));
        when(exerciseService.getById(12L, USER_1)).thenReturn(Optional.of(PLANK));
        when(sessionRepository.save(any(WorkoutSession.class))).thenAnswer(invocation -> invocation.getArgument(0));

        WorkoutSession session = service.create(request(null, List.of(
                new SetRequest(10L, 8, null, new BigDecimal("82.5")),
                new SetRequest(12L, null, 60, null))), USER_1);

        assertThat(session.getSets())
                .extracting(SessionSet::getReps, SessionSet::getDurationSeconds, SessionSet::getWeightKg)
                .containsExactly(
                        tuple(8, null, new BigDecimal("82.5")),
                        tuple(null, 60, null));
    }

    @Test
    void createFromTemplateUsesTemplateName() {
        when(templateService.getById(5L, USER_1))
                .thenReturn(Optional.of(new WorkoutTemplate(1L, "Leg day", null, false, null)));
        when(sessionRepository.save(any(WorkoutSession.class))).thenAnswer(invocation -> invocation.getArgument(0));

        WorkoutSession session = service.create(request(5L, List.of()), USER_1);

        assertThat(session.getName()).isEqualTo("Leg day");
        assertThat(session.getTemplateId()).isEqualTo(5L);
        assertThat(session.getUserId()).isEqualTo(1L);
    }

    @Test
    void createDefaultsPerformedAtToNow() {
        when(sessionRepository.save(any(WorkoutSession.class))).thenAnswer(invocation -> invocation.getArgument(0));
        Instant before = Instant.now();

        WorkoutSession session = service.create(request(null, List.of()), USER_1);

        assertThat(session.getPerformedAt()).isBetween(before, Instant.now());
    }

    @Test
    void createRejectsTemplateTheUserCannotSee() {
        when(templateService.getById(5L, USER_1)).thenReturn(Optional.empty());

        assertStatus(() -> service.create(request(5L, List.of()), USER_1), HttpStatus.BAD_REQUEST);
        verify(sessionRepository, never()).save(any());
    }

    @Test
    void createRejectsExerciseTheUserCannotSee() {
        when(exerciseService.getById(10L, USER_1)).thenReturn(Optional.empty());

        assertStatus(() -> service.create(request(null, List.of(reps(10L))), USER_1), HttpStatus.BAD_REQUEST);
        verify(sessionRepository, never()).save(any());
    }

    // --- privacy ---

    @Test
    void otherUsersSessionIsHidden() {
        when(sessionRepository.findById(7L)).thenReturn(Optional.of(session(7L, 2L)));

        assertThat(service.getById(7L, USER_1)).isEmpty();
    }

    @Test
    void cannotAddSetToOtherUsersSession() {
        when(sessionRepository.findById(7L)).thenReturn(Optional.of(session(7L, 2L)));

        assertStatus(() -> service.addSet(7L, reps(10L), USER_1), HttpStatus.NOT_FOUND);
        verify(sessionRepository, never()).flush();
    }

    @Test
    void cannotDeleteOtherUsersSession() {
        when(sessionRepository.findById(7L)).thenReturn(Optional.of(session(7L, 1L)));

        assertStatus(() -> service.deleteById(7L, USER_2), HttpStatus.NOT_FOUND);
        verify(sessionRepository, never()).delete(any());
    }

    // --- sets ---

    @Test
    void addSetContinuesNumberingForThatExercise() {
        WorkoutSession session = session(7L, 1L);
        session.addSet(BENCH, 8, null, null);
        when(sessionRepository.findById(7L)).thenReturn(Optional.of(session));
        when(exerciseService.getById(10L, USER_1)).thenReturn(Optional.of(BENCH));

        SessionSet added = service.addSet(7L, reps(10L), USER_1);

        assertThat(added.getSetNumber()).isEqualTo(2);
    }

    @Test
    void updateSetChangesValues() {
        WorkoutSession session = session(7L, 1L);
        SessionSet set = withId(30L, session.addSet(BENCH, 8, null, new BigDecimal("80")));
        when(sessionRepository.findById(7L)).thenReturn(Optional.of(session));

        service.updateSet(7L, 30L, new UpdateSetRequest(6, null, new BigDecimal("85")), USER_1);

        assertThat(set.getReps()).isEqualTo(6);
        assertThat(set.getWeightKg()).isEqualByComparingTo("85");
    }

    @Test
    void deleteSetRemovesIt() {
        WorkoutSession session = session(7L, 1L);
        withId(30L, session.addSet(BENCH, 8, null, null));
        when(sessionRepository.findById(7L)).thenReturn(Optional.of(session));

        service.deleteSet(7L, 30L, USER_1);

        assertThat(session.getSets()).isEmpty();
    }

    @Test
    void deleteUnknownSetIsNotFound() {
        when(sessionRepository.findById(7L)).thenReturn(Optional.of(session(7L, 1L)));

        assertStatus(() -> service.deleteSet(7L, 99L, USER_1), HttpStatus.NOT_FOUND);
    }

    private static CreateSessionRequest request(Long templateId, List<SetRequest> sets) {
        return new CreateSessionRequest(templateId, null, null, null, null, sets);
    }

    private static SetRequest reps(Long exerciseId) {
        return new SetRequest(exerciseId, 8, null, new BigDecimal("60"));
    }

    // A session as if loaded from the database
    private static WorkoutSession session(Long id, Long userId) {
        return withId(id, new WorkoutSession(userId, null, "Workout", null, null, Instant.now()));
    }

    // Gives an entity the id the database would have assigned
    private static <T> T withId(Long id, T entity) {
        ReflectionTestUtils.setField(entity, "id", id);
        return entity;
    }

    private static void assertStatus(ThrowingCallable call, HttpStatus expected) {
        assertThatThrownBy(call)
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        e -> assertThat(e.getStatusCode()).isEqualTo(expected));
    }
}
