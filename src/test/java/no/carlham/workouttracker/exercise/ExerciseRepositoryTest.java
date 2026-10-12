package no.carlham.workouttracker.exercise;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Pageable;

import jakarta.persistence.EntityManager;
import no.carlham.workouttracker.TestcontainersConfiguration;
import no.carlham.workouttracker.musclegroup.MuscleGroup;
import no.carlham.workouttracker.musclegroup.MuscleGroupRepository;
import no.carlham.workouttracker.user.User;
import no.carlham.workouttracker.user.UserRepository;

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
    private UserRepository userRepository;

    @Autowired
    private EntityManager entityManager;

    @Test
    void savesAndLoadsExerciseWithMuscleGroups() {
        // Seeded by V2__seed_muscle_groups.sql
        MuscleGroup chest = findMuscleGroup("Chest");
        MuscleGroup triceps = findMuscleGroup("Triceps");

        Exercise exercise = new Exercise("Bench press", "Flat bench", null);
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

    @Test
    void findVisibleToReturnsPresetsAndOwnExercisesOnly() {
        Long me = userRepository.save(new User("me", "not-a-real-hash")).getId();
        Long someoneElse = userRepository.save(new User("someone-else", "not-a-real-hash")).getId();

        exerciseRepository.save(new Exercise("Preset squat", null, null));
        exerciseRepository.save(new Exercise("My squat", null, me));
        exerciseRepository.save(new Exercise("Their squat", null, someoneElse));

        // Seeded presets are also visible, so check for ours rather than an exact list
        assertThat(exerciseRepository.findVisibleTo(me, Pageable.unpaged()))
                .extracting(Exercise::getName)
                .contains("Preset squat", "My squat")
                .doesNotContain("Their squat");
    }

    @Test
    void databaseRejectsDuplicatePresetNameIgnoringCase() {
        // "Back Squat" is seeded by V4; the unique index from V5 should block a second one
        assertThatThrownBy(() -> exerciseRepository.saveAndFlush(new Exercise("back squat", null, null)))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void everySeededPresetHasADescriptionAndAPrimaryMuscleGroup() {
        // Seeded by V4 and V6. Both match on exercise names, so a typo there would silently skip a row.
        List<Exercise> presets = exerciseRepository.findAll().stream()
                .filter(Exercise::isPreset)
                .toList();

        assertThat(presets).isNotEmpty();
        assertThat(presets).allSatisfy(exercise -> {
            assertThat(exercise.getDescription())
                    .as("description of %s", exercise.getName())
                    .isNotBlank();
            assertThat(exercise.getMuscleGroups())
                    .as("muscle groups of %s", exercise.getName())
                    .extracting(ExerciseMuscleGroup::getRole)
                    .contains(MuscleRole.PRIMARY);
        });
    }

    private MuscleGroup findMuscleGroup(String name) {
        return muscleGroupRepository.findAll().stream()
                .filter(group -> group.getName().equals(name))
                .findFirst()
                .orElseThrow();
    }
}
