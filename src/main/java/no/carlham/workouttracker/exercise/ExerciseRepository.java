package no.carlham.workouttracker.exercise;

import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ExerciseRepository extends JpaRepository<Exercise, Long> {
    @Override
    @EntityGraph(attributePaths = {"muscleGroups", "muscleGroups.muscleGroup"})
    Optional<Exercise> findById(Long id);
}
