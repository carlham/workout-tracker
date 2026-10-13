package no.carlham.workouttracker.session;

import java.math.BigDecimal;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;

/** Fixes a logged set. To change the exercise, delete the set and log a new one. */
public record UpdateSetRequest(
        @Positive Integer reps,
        @Positive Integer durationSeconds,
        @PositiveOrZero @Digits(integer = 3, fraction = 2) BigDecimal weightKg) {

    @AssertTrue(message = "set either reps or durationSeconds, not both")
    public boolean isRepsOrDurationValid() {
        return (reps == null) != (durationSeconds == null);
    }
}
