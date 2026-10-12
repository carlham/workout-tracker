package no.carlham.workouttracker.exercise;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface ExerciseRepository extends JpaRepository<Exercise, Long> {
    @Override
    @EntityGraph(attributePaths = {"muscleGroups", "muscleGroups.muscleGroup"})
    Optional<Exercise> findById(Long id);

    List<Exercise> findByNameIgnoreCase(String name);

    /** Preset exercises plus the given user's own exercises. */
    @Query("select e from Exercise e where e.ownerId is null or e.ownerId = :userId")
    Page<Exercise> findVisibleTo(Long userId, Pageable pageable);
}
