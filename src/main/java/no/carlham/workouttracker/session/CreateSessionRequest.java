package no.carlham.workouttracker.session;

import java.time.Instant;
import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

/**
 * Starts a session. Everything is optional, so this works both for starting a workout live
 * (send nothing, then log sets one by one) and for logging one afterwards (send it all at once).
 *
 * @param templateId  the template being followed; its name is used if no name is given
 * @param performedAt when the workout happened; defaults to now
 */
public record CreateSessionRequest(
        Long templateId,
        @Size(max = 200) String name,
        @Size(max = 1000) String comment,
        @Positive Integer durationMinutes,
        Instant performedAt,
        List<@NotNull @Valid SetRequest> sets) {

    public CreateSessionRequest {
        sets = sets == null ? List.of() : sets;
    }
}
