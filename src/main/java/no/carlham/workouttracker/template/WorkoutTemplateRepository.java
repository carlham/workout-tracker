package no.carlham.workouttracker.template;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WorkoutTemplateRepository extends JpaRepository<WorkoutTemplate, Long> {
    @Override
    @EntityGraph(attributePaths = {"exercises", "exercises.exercise"})
    Optional<WorkoutTemplate> findById(Long id);

    Page<WorkoutTemplate> findByOwnerId(Long ownerId, Pageable pageable);

    /** Public templates made by other users. */
    Page<WorkoutTemplate> findByIsPublicTrueAndOwnerIdNot(Long ownerId, Pageable pageable);
}
