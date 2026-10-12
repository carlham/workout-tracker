package no.carlham.workouttracker.exercise;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateExerciseRequest(
        @NotBlank @Size(max = 200) String name,
        @Size(max = 1000) String comment,
        @Size(max = 1000) String description,
        @NotEmpty List<@Valid MuscleGroupLink> muscleGroups) {

        public record MuscleGroupLink(
                @NotNull Long muscleGroupId,
                @NotNull MuscleRole role
        ) {}
}
