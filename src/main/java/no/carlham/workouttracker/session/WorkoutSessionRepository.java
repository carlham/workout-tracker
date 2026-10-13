package no.carlham.workouttracker.session;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WorkoutSessionRepository extends JpaRepository<WorkoutSession, Long> {
    @Override
    @EntityGraph(attributePaths = {"sets", "sets.exercise"})
    Optional<WorkoutSession> findById(Long id);

    Page<WorkoutSession> findByUserId(Long userId, Pageable pageable);
}
