package no.carlham.workouttracker.exercise;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ExerciseRepository extends JpaRepository<Exercise, Long> {
    @Override
    @EntityGraph(attributePaths = {"muscleGroups", "muscleGroups.muscleGroup"})
    Optional<Exercise> findById(Long id);

    List<Exercise> findByNameIgnoreCase(String name);

    /** Preset exercises plus the given user's own exercises. */
    default Page<Exercise> findVisibleTo(Long userId, Pageable pageable) {
        return findByOwnerIdIsNullOrOwnerId(userId, pageable);
    }

    // A derived query rather than @Query, so an unknown sort property fails with PropertyReferenceException
    // like the other paged queries, which ApiExceptionHandler turns into a 400
    Page<Exercise> findByOwnerIdIsNullOrOwnerId(Long ownerId, Pageable pageable);
}
