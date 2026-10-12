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

    // How to perform the exercise; comment is for notes about it
    private String description;

    private Long ownerId;

    @OneToMany(mappedBy = "exercise", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ExerciseMuscleGroup> muscleGroups = new ArrayList<>();


    protected Exercise() {
    }

    /**
     * @param ownerId the user who owns this exercise, or null for a preset exercise shared by everyone
     */
    public Exercise(String name, String comment, String description, Long ownerId) {
        this.name = name;
        this.comment = comment;
        this.description = description;
        this.ownerId = ownerId;
    }

    public Exercise(String name, String comment, Long ownerId) {
        this(name, comment, null, ownerId);
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

    public boolean isPreset() {
        return ownerId == null;
    }

    public String getComment() {
        return comment;
    }

    public String getDescription() {
        return description;
    }

    public void updateDetails(String name, String comment, String description) {
        this.name = name;
        this.comment = comment;
        this.description = description;
    }

    public List<ExerciseMuscleGroup> getMuscleGroups() {
        return muscleGroups;
    }

    public void addMuscleGroup(MuscleGroup muscleGroup, MuscleRole role) {
        muscleGroups.add(new ExerciseMuscleGroup(this, muscleGroup, role));
    }

}
