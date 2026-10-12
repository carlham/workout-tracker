package no.carlham.workouttracker.template;

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

/**
 * Ownership rules:
 * - Templates are private to their owner unless marked public.
 * - Anyone can view and copy a public template, but only the owner can change it.
 * - A template can only use exercises its owner can see (presets and their own).
 * - Public templates can only use preset exercises, so they don't expose anyone's private exercises.
 */
@Service
public class TemplateService {
    private final WorkoutTemplateRepository templateRepository;
    private final ExerciseService exerciseService;

    public TemplateService(WorkoutTemplateRepository templateRepository,
        ExerciseService exerciseService
    ) {
        this.templateRepository = templateRepository;
        this.exerciseService = exerciseService;
    }

    @Transactional(readOnly = true)
    public Page<WorkoutTemplate> getMine(Pageable pageable, CurrentUser user) {
        return templateRepository.findByOwnerId(user.id(), pageable);
    }

    @Transactional(readOnly = true)
    public Page<WorkoutTemplate> getPublic(Pageable pageable, CurrentUser user) {
        return templateRepository.findByIsPublicTrueAndOwnerIdNot(user.id(), pageable);
    }

    @Transactional(readOnly = true)
    public Optional<WorkoutTemplate> getById(Long id, CurrentUser user) {
        return findVisible(id, user);
    }

    @Transactional
    public WorkoutTemplate create(TemplateRequest request, CurrentUser user) {
        WorkoutTemplate template = new WorkoutTemplate(
            user.id(), request.name(), request.comment(), request.isPublic(), null);
        addExercises(template, request, user);
        return templateRepository.save(template);
    }

    @Transactional
    public Optional<WorkoutTemplate> update(Long id, TemplateRequest request, CurrentUser user) {
        return findVisible(id, user)
            .map(template -> {
                requireOwner(template, user);
                template.updateDetails(request.name(), request.comment(), request.isPublic());
                template.clearExercises();
                addExercises(template, request, user);
                return templateRepository.save(template);
            });
    }

    @Transactional
    public void deleteById(Long id, CurrentUser user) {
        WorkoutTemplate template = findVisible(id, user)
            .orElseThrow(() -> notFound(id));

        requireOwner(template, user);
        templateRepository.delete(template);
    }

    /** Copies a template into a new private template owned by the current user. */
    @Transactional
    public WorkoutTemplate copy(Long id, CurrentUser user) {
        WorkoutTemplate source = findVisible(id, user)
            .orElseThrow(() -> notFound(id));

        WorkoutTemplate copy = new WorkoutTemplate(
            user.id(), source.getName(), source.getComment(), false, source.getId());
        for (TemplateExercise item : source.getExercises()) {
            copy.addExercise(item.getExercise(), item.getTarget());
        }

        return templateRepository.save(copy);
    }

    private void addExercises(WorkoutTemplate template, TemplateRequest request, CurrentUser user) {
        for (var item : request.exercises()) {
            Exercise exercise = exerciseService.getById(item.exerciseId(), user)
                .orElseThrow(() -> new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Unknown exercise id " + item.exerciseId()));

            if (request.isPublic() && !exercise.isPreset()) {
                throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Public templates can only contain preset exercises");
            }

            template.addExercise(exercise, item.toTarget());
        }
    }

    // Other users' private templates are reported as "not found", like exercises
    private Optional<WorkoutTemplate> findVisible(Long id, CurrentUser user) {
        return templateRepository.findById(id)
            .filter(template -> template.isPublic() || template.getOwnerId().equals(user.id()));
    }

    private static void requireOwner(WorkoutTemplate template, CurrentUser user) {
        if (!template.getOwnerId().equals(user.id())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You can't modify this template");
        }
    }

    private static ResponseStatusException notFound(Long id) {
        return new ResponseStatusException(HttpStatus.NOT_FOUND, "Unknown template id " + id);
    }
}
