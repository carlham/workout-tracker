package no.carlham.workouttracker.template;

import java.util.ArrayList;
import java.util.List;

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
@Table(name = "workout_templates")
public class WorkoutTemplate {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long ownerId;

    private String name;

    private String comment;

    private boolean isPublic;

    // The template this one was copied from, if any
    private Long copiedFromId;

    @OneToMany(mappedBy = "template", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("position")
    private List<TemplateExercise> exercises = new ArrayList<>();

    protected WorkoutTemplate() {
    }

    public WorkoutTemplate(Long ownerId, String name, String comment, boolean isPublic, Long copiedFromId) {
        this.ownerId = ownerId;
        this.name = name;
        this.comment = comment;
        this.isPublic = isPublic;
        this.copiedFromId = copiedFromId;
    }

    public Long getId() {
        return id;
    }

    public Long getOwnerId() {
        return ownerId;
    }

    public String getName() {
        return name;
    }

    public String getComment() {
        return comment;
    }

    public boolean isPublic() {
        return isPublic;
    }

    public Long getCopiedFromId() {
        return copiedFromId;
    }

    public List<TemplateExercise> getExercises() {
        return exercises;
    }

    public void updateDetails(String name, String comment, boolean isPublic) {
        this.name = name;
        this.comment = comment;
        this.isPublic = isPublic;
    }

    /** Adds an exercise at the end of the template. Positions start at 1. */
    public void addExercise(Exercise exercise, ExerciseTarget target) {
        exercises.add(new TemplateExercise(this, exercise, exercises.size() + 1, target));
    }

    public void clearExercises() {
        exercises.clear();
    }
}
