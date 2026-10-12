package no.carlham.workouttracker.template;

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
@Table(name = "template_exercises")
public class TemplateExercise {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "template_id")
    private WorkoutTemplate template;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "exercise_id")
    private Exercise exercise;

    private int position;

    private int targetSets;

    private Integer targetRepsMin;

    private Integer targetRepsMax;

    private Integer targetSeconds;

    private Integer restSeconds;

    protected TemplateExercise() {
    }

    TemplateExercise(WorkoutTemplate template, Exercise exercise, int position, ExerciseTarget target) {
        this.template = template;
        this.exercise = exercise;
        this.position = position;
        this.targetSets = target.sets();
        this.targetRepsMin = target.repsMin();
        this.targetRepsMax = target.repsMax();
        this.targetSeconds = target.seconds();
        this.restSeconds = target.restSeconds();
    }

    public Long getId() {
        return id;
    }

    public Exercise getExercise() {
        return exercise;
    }

    public int getPosition() {
        return position;
    }

    public ExerciseTarget getTarget() {
        return new ExerciseTarget(targetSets, targetRepsMin, targetRepsMax, targetSeconds, restSeconds);
    }
}
