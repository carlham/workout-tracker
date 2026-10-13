package no.carlham.workouttracker.session;

import java.time.Instant;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

/** Replaces a session's details. Sets are changed through their own endpoints. */
public record UpdateSessionRequest(
        @Size(max = 200) String name,
        @Size(max = 1000) String comment,
        @Positive Integer durationMinutes,
        @NotNull Instant performedAt) {
}
