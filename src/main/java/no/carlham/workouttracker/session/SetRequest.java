package no.carlham.workouttracker.session;

import java.math.BigDecimal;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;

/** Logs one set: reps (optionally with weight) or a duration for timed exercises. */
public record SetRequest(
        @NotNull Long exerciseId,
        @Positive Integer reps,
        @Positive Integer durationSeconds,
        // Matches the numeric(5,2) column: up to 999.99 kg
        @PositiveOrZero @Digits(integer = 3, fraction = 2) BigDecimal weightKg) {

    @AssertTrue(message = "set either reps or durationSeconds, not both")
    public boolean isRepsOrDurationValid() {
        return (reps == null) != (durationSeconds == null);
    }
}
