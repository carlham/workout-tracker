package no.carlham.workouttracker.session;

import java.math.BigDecimal;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import no.carlham.workouttracker.exercise.Exercise;

@Entity
@Table(name = "session_sets")
public class SessionSet {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "session_id")
    private WorkoutSession session;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "exercise_id")
    private Exercise exercise;

    private int setNumber;

    private Integer reps;

    private Integer durationSeconds;

    // BigDecimal, not double: a double can't store values like 82.5 kg exactly
    private BigDecimal weightKg;

    protected SessionSet() {
    }

    SessionSet(WorkoutSession session, Exercise exercise, int setNumber,
            Integer reps, Integer durationSeconds, BigDecimal weightKg) {
        this.session = session;
        this.exercise = exercise;
        this.setNumber = setNumber;
        this.reps = reps;
        this.durationSeconds = durationSeconds;
        this.weightKg = weightKg;
    }

    public Long getId() {
        return id;
    }

    public Exercise getExercise() {
        return exercise;
    }

    public int getSetNumber() {
        return setNumber;
    }

    public Integer getReps() {
        return reps;
    }

    public Integer getDurationSeconds() {
        return durationSeconds;
    }

    public BigDecimal getWeightKg() {
        return weightKg;
    }

    public void update(Integer reps, Integer durationSeconds, BigDecimal weightKg) {
        this.reps = reps;
        this.durationSeconds = durationSeconds;
        this.weightKg = weightKg;
    }
}
