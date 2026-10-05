package no.carlham.workouttracker.exercise;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import no.carlham.workouttracker.musclegroup.MuscleGroup;

@Entity
@Table(name = "exercise_muscle_groups")
public class ExerciseMuscleGroup {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "exercise_id")
    private Exercise exercise;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "muscle_group_id")
    private MuscleGroup muscleGroup;

    @Enumerated(EnumType.STRING)
    private MuscleRole role;


    protected ExerciseMuscleGroup() {}

    public ExerciseMuscleGroup(Exercise exercise, MuscleGroup muscleGroup, MuscleRole role) {
        this.exercise = exercise;
        this.muscleGroup = muscleGroup;
        this.role = role;
    }


    public Long getId() {
        return id;
    }

    public Exercise getExercise() {
        return exercise;
    }

    public MuscleGroup getMuscleGroup() {
        return muscleGroup;
    }

    public MuscleRole getRole() {
        return role;
    }
}
