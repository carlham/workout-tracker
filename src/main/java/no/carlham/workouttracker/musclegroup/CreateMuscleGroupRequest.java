package no.carlham.workouttracker.musclegroup;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateMuscleGroupRequest(
        @NotBlank @Size(max = 50) String name) {
}
