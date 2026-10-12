package no.carlham.workouttracker.template;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

/**
 * Used for both create and update: an update replaces the whole template, including its exercise list.
 */
public record TemplateRequest(
        @NotBlank @Size(max = 200) String name,
        @Size(max = 1000) String comment,
        Boolean isPublic,
        @NotEmpty List<@NotNull @Valid TemplateExerciseRequest> exercises) {

    // Optional in the JSON and defaults to private. It's a Boolean rather than a boolean because
    // Jackson 3 rejects a missing value for a primitive.
    public TemplateRequest {
        isPublic = Boolean.TRUE.equals(isPublic);
    }

    public record TemplateExerciseRequest(
            @NotNull Long exerciseId,
            @NotNull @Positive @Max(20) Integer targetSets,
            @Positive Integer targetRepsMin,
            @Positive Integer targetRepsMax,
            @Positive Integer targetSeconds,
            @PositiveOrZero Integer restSeconds) {

        // Bean Validation runs this like a getter, and reports failures under the name "targetValid"
        @AssertTrue(message = "set either targetRepsMin and targetRepsMax (min <= max), or targetSeconds")
        public boolean isTargetValid() {
            boolean hasReps = targetRepsMin != null && targetRepsMax != null && targetRepsMin <= targetRepsMax;
            boolean noReps = targetRepsMin == null && targetRepsMax == null;
            return (hasReps && targetSeconds == null) || (noReps && targetSeconds != null);
        }

        ExerciseTarget toTarget() {
            return new ExerciseTarget(targetSets, targetRepsMin, targetRepsMax, targetSeconds, restSeconds);
        }
    }
}
