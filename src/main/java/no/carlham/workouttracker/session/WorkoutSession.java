package no.carlham.workouttracker.session;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import no.carlham.workouttracker.exercise.Exercise;

@Entity
@Table(name = "workout_sessions")
public class WorkoutSession {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long userId;

    // The template this session was started from, if any. Set to NULL by the database if the template is deleted.
    private Long templateId;

    private String name;

    private String comment;

    private Integer durationMinutes;

    private Instant performedAt;

    // In the order they were logged
    @OneToMany(mappedBy = "session", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("id")
    private List<SessionSet> sets = new ArrayList<>();

    protected WorkoutSession() {
    }

    public WorkoutSession(Long userId, Long templateId, String name, String comment,
            Integer durationMinutes, Instant performedAt) {
        this.userId = userId;
        this.templateId = templateId;
        this.name = name;
        this.comment = comment;
        this.durationMinutes = durationMinutes;
        this.performedAt = performedAt;
    }

    public Long getId() {
        return id;
    }

    public Long getUserId() {
        return userId;
    }

    public Long getTemplateId() {
        return templateId;
    }

    public String getName() {
        return name;
    }

    public String getComment() {
        return comment;
    }

    public Integer getDurationMinutes() {
        return durationMinutes;
    }

    public Instant getPerformedAt() {
        return performedAt;
    }

    public List<SessionSet> getSets() {
        return sets;
    }

    public void updateDetails(String name, String comment, Integer durationMinutes, Instant performedAt) {
        this.name = name;
        this.comment = comment;
        this.durationMinutes = durationMinutes;
        this.performedAt = performedAt;
    }

    /** Logs a set. Set numbers count up per exercise: the third bench press set logged is set 3. */
    public SessionSet addSet(Exercise exercise, Integer reps, Integer durationSeconds, BigDecimal weightKg) {
        int setNumber = sets.stream()
            .filter(set -> set.getExercise().getId().equals(exercise.getId()))
            .mapToInt(SessionSet::getSetNumber)
            .max()
            .orElse(0) + 1;

        SessionSet set = new SessionSet(this, exercise, setNumber, reps, durationSeconds, weightKg);
        sets.add(set);
        return set;
    }

    public Optional<SessionSet> findSet(Long setId) {
        return sets.stream()
            .filter(set -> set.getId().equals(setId))
            .findFirst();
    }

    public void removeSet(SessionSet set) {
        sets.remove(set);
    }
}
