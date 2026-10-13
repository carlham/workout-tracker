package no.carlham.workouttracker.exercise;

import java.util.HashSet;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import no.carlham.workouttracker.musclegroup.MuscleGroup;
import no.carlham.workouttracker.musclegroup.MuscleGroupRepository;
import no.carlham.workouttracker.security.CurrentUser;

/**
 * Ownership rules:
 * - Preset exercises (no owner) are visible to everyone and can only be changed by admins.
 * - A user's own exercises are only visible to and changeable by that user.
 * - Exercises created by an admin become presets.
 * - Names are unique among presets and within each user's own exercises.
 */
@Service
public class ExerciseService {
    private final ExerciseRepository exerciseRepository;
    private final MuscleGroupRepository muscleGroupRepository;

    public ExerciseService(ExerciseRepository exerciseRepository,
        MuscleGroupRepository muscleGroupRepository
    ) {
        this.exerciseRepository = exerciseRepository;
        this.muscleGroupRepository = muscleGroupRepository;
    }

    @Transactional(readOnly = true)
    public Page<Exercise> getAll(Pageable pageable, CurrentUser user) {
        return exerciseRepository.findVisibleTo(user.id(), pageable);
    }

    @Transactional(readOnly = true)
    public Optional<Exercise> getById(Long id, CurrentUser user) {
        return findVisible(id, user);
    }

    @Transactional
    public Exercise create(CreateExerciseRequest request, CurrentUser user) {
        Long ownerId = user.isAdmin() ? null : user.id();
        requireNameAvailable(request.name(), ownerId, null);

        Exercise exercise = new Exercise(request.name(), request.comment(), request.description(), ownerId);
        Set<Long> seenIds = new HashSet<>();

        for (var link : request.muscleGroups()) {
            if (!seenIds.add(link.muscleGroupId())) {
                throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Duplicate muscle group id " + link.muscleGroupId());
            }

            MuscleGroup musclegroup = muscleGroupRepository.findById(link.muscleGroupId())
                .orElseThrow(() -> new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Unknown muscle group id " + link.muscleGroupId()));

            exercise.addMuscleGroup(musclegroup, link.role());
        }

        return exerciseRepository.save(exercise);
    }

    @Transactional
    public Optional<Exercise> update(Long id, UpdateExerciseRequest request, CurrentUser user) {
        return findVisible(id, user)
            .map(exercise -> {
                requireCanModify(exercise, user);
                requireNameAvailable(request.name(), exercise.getOwnerId(), exercise.getId());
                exercise.updateDetails(request.name(), request.comment(), request.description());
                return exerciseRepository.save(exercise);
            });
    }

    @Transactional
    public void deleteById(Long id, CurrentUser user) {
        Exercise exercise = findVisible(id, user)
            .orElseThrow(() -> new ResponseStatusException(
                HttpStatus.NOT_FOUND,
                "Unknown exercise id " + id));

        requireCanModify(exercise, user);

        // Templates and sessions reference exercises, and the database blocks deleting one that's in use.
        // We let it check rather than asking those packages, which already depend on this one.
        // Flushing makes the DELETE run now, so the error can be caught here.
        try {
            exerciseRepository.delete(exercise);
            exerciseRepository.flush();
        } catch (DataIntegrityViolationException e) {
            throw new ResponseStatusException(
                HttpStatus.CONFLICT,
                "Exercise is used in a template or session and can't be deleted");
        }
    }

    // Other users' exercises are reported as "not found" rather than "forbidden",
    // so you can't discover which ids exist
    private Optional<Exercise> findVisible(Long id, CurrentUser user) {
        return exerciseRepository.findById(id)
            .filter(exercise -> canView(exercise, user));
    }

    // Names are unique (ignoring case) among presets and within each user's own exercises.
    // The database enforces this too (V5 migration); checking here gives a clear 409 instead of a 500.
    private void requireNameAvailable(String name, Long ownerId, Long exerciseIdBeingUpdated) {
        boolean taken = exerciseRepository.findByNameIgnoreCase(name).stream()
            .filter(existing -> !existing.getId().equals(exerciseIdBeingUpdated))
            .anyMatch(existing -> Objects.equals(existing.getOwnerId(), ownerId));

        if (taken) {
            throw new ResponseStatusException(
                HttpStatus.CONFLICT,
                "An exercise named '" + name + "' already exists");
        }
    }

    private static boolean canView(Exercise exercise, CurrentUser user) {
        return exercise.isPreset() || exercise.getOwnerId().equals(user.id());
    }

    private static void requireCanModify(Exercise exercise, CurrentUser user) {
        boolean allowed = exercise.isPreset()
            ? user.isAdmin()
            : exercise.getOwnerId().equals(user.id());

        if (!allowed) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You can't modify this exercise");
        }
    }
}
