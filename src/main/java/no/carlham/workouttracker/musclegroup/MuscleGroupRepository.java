package no.carlham.workouttracker.musclegroup;

import org.springframework.data.jpa.repository.JpaRepository;

public interface MuscleGroupRepository extends JpaRepository<MuscleGroup, Long> {
    boolean existsByNameIgnoreCase(String name);
}
