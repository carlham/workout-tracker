package no.carlham.workouttracker.session;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record SessionResponse(
    Long id,
    Long templateId,
    String name,
    String comment,
    Integer durationMinutes,
    Instant performedAt,
    List<SetResponse> sets
) {
    public record SetResponse(
        Long id,
        Long exerciseId,
        String exerciseName,
        int setNumber,
        Integer reps,
        Integer durationSeconds,
        BigDecimal weightKg
    ) {
        public static SetResponse from(SessionSet set) {
            return new SetResponse(
                set.getId(),
                set.getExercise().getId(),
                set.getExercise().getName(),
                set.getSetNumber(),
                set.getReps(),
                set.getDurationSeconds(),
                set.getWeightKg());
        }
    }

    public static SessionResponse from(WorkoutSession session) {
        return new SessionResponse(
            session.getId(),
            session.getTemplateId(),
            session.getName(),
            session.getComment(),
            session.getDurationMinutes(),
            session.getPerformedAt(),
            session.getSets().stream().map(SetResponse::from).toList()
        );
    }
}
