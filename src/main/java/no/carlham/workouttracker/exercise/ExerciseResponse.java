package no.carlham.workouttracker.exercise;

import java.util.List;

public record ExerciseResponse(
    Long id,
    String name,
    String comment,
    List<MuscleGroupResponse> muscleGroups
) {
    public record MuscleGroupResponse(Long id, String name, MuscleRole role) {}

    public static ExerciseResponse from(Exercise exercise) {
        return new ExerciseResponse(
            exercise.getId(),
            exercise.getName(),
            exercise.getComment(),
            exercise.getMuscleGroups().stream()
                .map(link -> new MuscleGroupResponse(
                    link.getMuscleGroup().getId(),
                    link.getMuscleGroup().getName(),
                    link.getRole()))
                .toList()      
        );
    }
}
