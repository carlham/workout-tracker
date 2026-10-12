-- Templates always belong to a user; sharing goes through is_public
ALTER TABLE "workout_templates" ALTER COLUMN "owner_id" SET NOT NULL;

CREATE INDEX ON "workout_templates" ("owner_id");

-- Targets are either a rep range (e.g. 8-12, or 10-10 for a fixed number) or a duration for timed exercises
ALTER TABLE "template_exercises" RENAME COLUMN "target_reps" TO "target_reps_min";
ALTER TABLE "template_exercises" ALTER COLUMN "target_reps_min" DROP NOT NULL;
ALTER TABLE "template_exercises" ADD COLUMN "target_reps_max" int;
ALTER TABLE "template_exercises" ADD COLUMN "target_seconds" int;

ALTER TABLE "template_exercises" ADD CONSTRAINT "template_exercises_target_check" CHECK (
  ("target_reps_min" IS NOT NULL AND "target_reps_max" IS NOT NULL AND "target_seconds" IS NULL
    AND "target_reps_min" <= "target_reps_max")
  OR ("target_reps_min" IS NULL AND "target_reps_max" IS NULL AND "target_seconds" IS NOT NULL)
);

-- Replacing a template's exercise list inserts the new rows before deleting the old ones,
-- so positions briefly overlap. A deferred constraint is only checked at commit.
DROP INDEX "template_exercises_template_id_position_idx";

ALTER TABLE "template_exercises" ADD CONSTRAINT "template_exercises_template_position_key"
  UNIQUE ("template_id", "position") DEFERRABLE INITIALLY DEFERRED;
