package no.carlham.workouttracker.exercise;
import no.carlham.workouttracker.musclegroup.MuscleGroup;

import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

@Entity
@Table(name = "exercises")
public class Exercise {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    private String comment;

    private Long ownerId;

    @OneToMany(mappedBy = "exercise", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ExerciseMuscleGroup> muscleGroups = new ArrayList<>();


    protected Exercise() {
    }

    public Exercise(String name, String comment ) {
        this.name = name;
        this.comment = comment;
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public Long getOwnerId() {
        return ownerId;
    }

    public String getComment() {
        return comment;
    }

    public void updateDetails(String name, String comment) {
        this.name = name;
        this.comment = comment;
    }

    public List<ExerciseMuscleGroup> getMuscleGroups() {
        return muscleGroups;
    }

    public void addMuscleGroup(MuscleGroup muscleGroup, MuscleRole role) {
        muscleGroups.add(new ExerciseMuscleGroup(this, muscleGroup, role));
    }

}
