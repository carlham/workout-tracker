package no.carlham.workouttracker.template;

import java.util.List;

public record TemplateResponse(
    Long id,
    Long ownerId,
    String name,
    String comment,
    boolean isPublic,
    Long copiedFromId,
    List<TemplateExerciseResponse> exercises
) {
    public record TemplateExerciseResponse(
        int position,
        Long exerciseId,
        String exerciseName,
        int targetSets,
        Integer targetRepsMin,
        Integer targetRepsMax,
        Integer targetSeconds,
        Integer restSeconds
    ) {}

    public static TemplateResponse from(WorkoutTemplate template) {
        return new TemplateResponse(
            template.getId(),
            template.getOwnerId(),
            template.getName(),
            template.getComment(),
            template.isPublic(),
            template.getCopiedFromId(),
            template.getExercises().stream()
                .map(item -> {
                    ExerciseTarget target = item.getTarget();
                    return new TemplateExerciseResponse(
                        item.getPosition(),
                        item.getExercise().getId(),
                        item.getExercise().getName(),
                        target.sets(),
                        target.repsMin(),
                        target.repsMax(),
                        target.seconds(),
                        target.restSeconds());
                })
                .toList()
        );
    }
}
