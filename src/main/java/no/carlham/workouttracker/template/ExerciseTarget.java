package no.carlham.workouttracker.template;

/**
 * What to aim for with one exercise in a template: a number of sets of either a rep range
 * (e.g. 8-12, or 10-10 for a fixed number) or a duration for timed exercises like planks.
 */
public record ExerciseTarget(
        int sets,
        Integer repsMin,
        Integer repsMax,
        Integer seconds,
        Integer restSeconds) {
}
