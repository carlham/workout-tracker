package no.carlham.workouttracker.template;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Pageable;

import jakarta.persistence.EntityManager;
import no.carlham.workouttracker.TestcontainersConfiguration;
import no.carlham.workouttracker.exercise.Exercise;
import no.carlham.workouttracker.exercise.ExerciseRepository;
import no.carlham.workouttracker.user.User;
import no.carlham.workouttracker.user.UserRepository;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(TestcontainersConfiguration.class)
class WorkoutTemplateRepositoryTest {

    @Autowired
    private WorkoutTemplateRepository templateRepository;

    @Autowired
    private ExerciseRepository exerciseRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private EntityManager entityManager;

    @Test
    void replacingExerciseListKeepsPositionsValid() {
        Long ownerId = userRepository.save(new User("planner", "not-a-real-hash")).getId();
        Exercise squat = preset("Back Squat");
        Exercise plank = preset("Plank");

        WorkoutTemplate template = new WorkoutTemplate(ownerId, "Leg day", null, false, null);
        template.addExercise(squat, new ExerciseTarget(3, 8, 12, null, 90));
        template.addExercise(plank, new ExerciseTarget(3, null, null, 60, 60));
        Long id = templateRepository.saveAndFlush(template).getId();

        // Replace the list in reverse order: new rows reuse positions 1 and 2 before the old rows are deleted
        template.clearExercises();
        template.addExercise(plank, new ExerciseTarget(2, null, null, 45, null));
        template.addExercise(squat, new ExerciseTarget(5, 5, 5, null, 180));
        templateRepository.flush();

        // The position constraint is deferred to commit, and this test never commits, so check it now
        entityManager.createNativeQuery("SET CONSTRAINTS ALL IMMEDIATE").executeUpdate();
        entityManager.clear();

        WorkoutTemplate loaded = templateRepository.findById(id).orElseThrow();
        assertThat(loaded.getExercises())
                .extracting(item -> item.getExercise().getName())
                .containsExactly("Plank", "Back Squat");
    }

    @Test
    void publicListingShowsOnlyOtherUsersPublicTemplates() {
        Long me = userRepository.save(new User("lister", "not-a-real-hash")).getId();
        Long someoneElse = userRepository.save(new User("sharer", "not-a-real-hash")).getId();
        Exercise squat = preset("Back Squat");

        saveTemplate(me, "My public", true, squat);
        saveTemplate(me, "My private", false, squat);
        saveTemplate(someoneElse, "Their public", true, squat);
        saveTemplate(someoneElse, "Their private", false, squat);

        assertThat(templateRepository.findByIsPublicTrueAndOwnerIdNot(me, Pageable.unpaged()))
                .extracting(WorkoutTemplate::getName)
                .containsExactly("Their public");
        assertThat(templateRepository.findByOwnerId(me, Pageable.unpaged()))
                .extracting(WorkoutTemplate::getName)
                .containsExactlyInAnyOrder("My public", "My private");
    }

    @Test
    void databaseRejectsTargetWithBothRepsAndSeconds() {
        Long ownerId = userRepository.save(new User("rule-breaker", "not-a-real-hash")).getId();

        WorkoutTemplate template = new WorkoutTemplate(ownerId, "Broken", null, false, null);
        template.addExercise(preset("Plank"), new ExerciseTarget(3, 8, 12, 60, null));

        assertThatThrownBy(() -> templateRepository.saveAndFlush(template))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    private void saveTemplate(Long ownerId, String name, boolean isPublic, Exercise exercise) {
        WorkoutTemplate template = new WorkoutTemplate(ownerId, name, null, isPublic, null);
        template.addExercise(exercise, new ExerciseTarget(3, 8, 12, null, null));
        templateRepository.save(template);
    }

    private Exercise preset(String name) {
        return exerciseRepository.findByNameIgnoreCase(name).stream()
                .filter(Exercise::isPreset)
                .findFirst()
                .orElseThrow();
    }
}
