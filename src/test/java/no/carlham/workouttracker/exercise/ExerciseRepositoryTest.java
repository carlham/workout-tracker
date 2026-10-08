package no.carlham.workouttracker.exercise;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;

import jakarta.persistence.EntityManager;
import no.carlham.workouttracker.TestcontainersConfiguration;
import no.carlham.workouttracker.musclegroup.MuscleGroup;
import no.carlham.workouttracker.musclegroup.MuscleGroupRepository;

// Runs against real Postgres with the Flyway migrations applied, so it also catches
// mismatches between the entity mappings and the SQL schema.
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(TestcontainersConfiguration.class)
class ExerciseRepositoryTest {

    @Autowired
    private ExerciseRepository exerciseRepository;

    @Autowired
    private MuscleGroupRepository muscleGroupRepository;

    @Autowired
    private EntityManager entityManager;

    @Test
    void savesAndLoadsExerciseWithMuscleGroups() {
        // Seeded by V2__seed_muscle_groups.sql
        MuscleGroup chest = findMuscleGroup("Chest");
        MuscleGroup triceps = findMuscleGroup("Triceps");

        Exercise exercise = new Exercise("Bench press", "Flat bench");
        exercise.addMuscleGroup(chest, MuscleRole.PRIMARY);
        exercise.addMuscleGroup(triceps, MuscleRole.SECONDARY);
        Long id = exerciseRepository.save(exercise).getId();

        // Write to the database and forget cached entities, so findById reads from Postgres
        entityManager.flush();
        entityManager.clear();

        Exercise loaded = exerciseRepository.findById(id).orElseThrow();
        assertThat(loaded.getName()).isEqualTo("Bench press");
        assertThat(loaded.getMuscleGroups())
                .extracting(link -> link.getMuscleGroup().getName(), ExerciseMuscleGroup::getRole)
                .containsExactlyInAnyOrder(
                        tuple("Chest", MuscleRole.PRIMARY),
                        tuple("Triceps", MuscleRole.SECONDARY));
    }

    private MuscleGroup findMuscleGroup(String name) {
        return muscleGroupRepository.findAll().stream()
                .filter(group -> group.getName().equals(name))
                .findFirst()
                .orElseThrow();
    }
}
