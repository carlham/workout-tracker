-- History is listed per user, newest first
CREATE INDEX ON "workout_sessions" ("user_id", "performed_at" DESC);

-- A set is either reps (optionally with weight) or a duration for timed exercises like planks
ALTER TABLE "session_sets" ADD COLUMN "duration_seconds" int;

ALTER TABLE "session_sets" ADD CONSTRAINT "session_sets_reps_or_duration_check" CHECK (
  ("reps" IS NULL) <> ("duration_seconds" IS NULL)
);

-- No two "set 2 of bench press" in the same session. Also serves as the index for loading a session's sets.
ALTER TABLE "session_sets" ADD CONSTRAINT "session_sets_session_exercise_set_key"
  UNIQUE ("session_id", "exercise_id", "set_number");
