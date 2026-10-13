package no.carlham.workouttracker.session;

import java.time.Instant;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import no.carlham.workouttracker.exercise.Exercise;
import no.carlham.workouttracker.exercise.ExerciseService;
import no.carlham.workouttracker.security.CurrentUser;
import no.carlham.workouttracker.template.TemplateService;
import no.carlham.workouttracker.template.WorkoutTemplate;

/**
 * Sessions are private: only the user who logged a session can see or change it, admins included.
 * Other users' sessions are reported as "not found".
 */
@Service
public class SessionService {
    private final WorkoutSessionRepository sessionRepository;
    private final ExerciseService exerciseService;
    private final TemplateService templateService;

    public SessionService(WorkoutSessionRepository sessionRepository,
        ExerciseService exerciseService,
        TemplateService templateService
    ) {
        this.sessionRepository = sessionRepository;
        this.exerciseService = exerciseService;
        this.templateService = templateService;
    }

    @Transactional(readOnly = true)
    public Page<WorkoutSession> getMine(Pageable pageable, CurrentUser user) {
        return sessionRepository.findByUserId(user.id(), pageable);
    }

    @Transactional(readOnly = true)
    public Optional<WorkoutSession> getById(Long id, CurrentUser user) {
        return findOwn(id, user);
    }

    @Transactional
    public WorkoutSession create(CreateSessionRequest request, CurrentUser user) {
        String name = request.name();
        if (request.templateId() != null) {
            WorkoutTemplate template = templateService.getById(request.templateId(), user)
                .orElseThrow(() -> new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Unknown template id " + request.templateId()));
            if (name == null) {
                name = template.getName();
            }
        }

        Instant performedAt = request.performedAt() != null ? request.performedAt() : Instant.now();
        WorkoutSession session = new WorkoutSession(
            user.id(), request.templateId(), name, request.comment(), request.durationMinutes(), performedAt);

        for (SetRequest set : request.sets()) {
            session.addSet(findExercise(set.exerciseId(), user), set.reps(), set.durationSeconds(), set.weightKg());
        }

        return sessionRepository.save(session);
    }

    @Transactional
    public Optional<WorkoutSession> update(Long id, UpdateSessionRequest request, CurrentUser user) {
        return findOwn(id, user)
            .map(session -> {
                session.updateDetails(
                    request.name(), request.comment(), request.durationMinutes(), request.performedAt());
                return sessionRepository.save(session);
            });
    }

    @Transactional
    public void deleteById(Long id, CurrentUser user) {
        sessionRepository.delete(requireOwn(id, user));
    }

    @Transactional
    public SessionSet addSet(Long sessionId, SetRequest request, CurrentUser user) {
        WorkoutSession session = requireOwn(sessionId, user);
        SessionSet set = session.addSet(
            findExercise(request.exerciseId(), user), request.reps(), request.durationSeconds(), request.weightKg());

        // Flush so the database assigns the new set's id before we return it. Not saveAndFlush: the session
        // is already managed, so save() would merge it, and merging stores a copy of the new set instead.
        sessionRepository.flush();
        return set;
    }

    @Transactional
    public SessionSet updateSet(Long sessionId, Long setId, UpdateSetRequest request, CurrentUser user) {
        SessionSet set = requireSet(requireOwn(sessionId, user), setId);
        set.update(request.reps(), request.durationSeconds(), request.weightKg());
        return set;
    }

    @Transactional
    public void deleteSet(Long sessionId, Long setId, CurrentUser user) {
        WorkoutSession session = requireOwn(sessionId, user);
        session.removeSet(requireSet(session, setId));
    }

    private Exercise findExercise(Long exerciseId, CurrentUser user) {
        return exerciseService.getById(exerciseId, user)
            .orElseThrow(() -> new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "Unknown exercise id " + exerciseId));
    }

    private Optional<WorkoutSession> findOwn(Long id, CurrentUser user) {
        return sessionRepository.findById(id)
            .filter(session -> session.getUserId().equals(user.id()));
    }

    private WorkoutSession requireOwn(Long id, CurrentUser user) {
        return findOwn(id, user)
            .orElseThrow(() -> new ResponseStatusException(
                HttpStatus.NOT_FOUND,
                "Unknown session id " + id));
    }

    private static SessionSet requireSet(WorkoutSession session, Long setId) {
        return session.findSet(setId)
            .orElseThrow(() -> new ResponseStatusException(
                HttpStatus.NOT_FOUND,
                "Unknown set id " + setId));
    }
}
