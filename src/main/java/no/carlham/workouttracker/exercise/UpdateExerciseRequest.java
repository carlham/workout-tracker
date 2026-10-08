package no.carlham.workouttracker.exercise;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateExerciseRequest(
    @NotBlank @Size(max = 200) String name,
    @Size(max = 1000) String comment
) {}
