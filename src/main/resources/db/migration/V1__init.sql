CREATE TABLE "users" (
  "id" bigserial PRIMARY KEY,
  "username" varchar(50) UNIQUE NOT NULL,
  "role" varchar(20) NOT NULL DEFAULT 'USER',
  "created_at" timestamptz NOT NULL DEFAULT (now()),
  "password_hash" varchar(255) NOT NULL
);

CREATE TABLE "exercises" (
  "id" bigserial PRIMARY KEY,
  "name" varchar NOT NULL,
  "comment" text,
  "owner_id" bigint,
  "created_at" timestamptz DEFAULT (now())
);

CREATE TABLE "workout_templates" (
  "id" bigserial PRIMARY KEY,
  "owner_id" bigint,
  "name" text NOT NULL,
  "is_public" boolean NOT NULL DEFAULT false,
  "comment" text,
  "copied_from_id" bigint
);

CREATE TABLE "workout_sessions" (
  "id" bigserial PRIMARY KEY,
  "template_id" bigint,
  "user_id" bigint NOT NULL,
  "name" text,
  "comment" text,
  "duration_minutes" int,
  "performed_at" timestamptz NOT NULL DEFAULT (now())
);

CREATE TABLE "template_exercises" (
  "id" bigserial PRIMARY KEY,
  "template_id" bigint NOT NULL,
  "exercise_id" bigint NOT NULL,
  "position" int NOT NULL,
  "target_sets" int NOT NULL,
  "target_reps" int NOT NULL,
  "rest_seconds" int
);

CREATE TABLE "exercise_muscle_groups" (
  "id" bigserial PRIMARY KEY,
  "exercise_id" bigint NOT NULL,
  "muscle_group_id" bigint NOT NULL,
  "role" varchar(10) NOT NULL
);

CREATE TABLE "session_sets" (
  "id" bigserial PRIMARY KEY,
  "session_id" bigint NOT NULL,
  "exercise_id" bigint NOT NULL,
  "set_number" int NOT NULL,
  "reps" int,
  "weight_kg" numeric(5,2)
);

CREATE TABLE "muscle_groups" (
  "id" bigserial PRIMARY KEY,
  "name" varchar(50) UNIQUE NOT NULL
);

CREATE UNIQUE INDEX ON "template_exercises" ("template_id", "position");

CREATE UNIQUE INDEX ON "exercise_muscle_groups" ("exercise_id", "muscle_group_id");

ALTER TABLE "exercises" ADD FOREIGN KEY ("owner_id") REFERENCES "users" ("id") DEFERRABLE INITIALLY IMMEDIATE;

ALTER TABLE "workout_templates" ADD FOREIGN KEY ("owner_id") REFERENCES "users" ("id") DEFERRABLE INITIALLY IMMEDIATE;

ALTER TABLE "workout_sessions" ADD FOREIGN KEY ("user_id") REFERENCES "users" ("id") DEFERRABLE INITIALLY IMMEDIATE;

ALTER TABLE "template_exercises" ADD FOREIGN KEY ("exercise_id") REFERENCES "exercises" ("id") DEFERRABLE INITIALLY IMMEDIATE;

ALTER TABLE "exercise_muscle_groups" ADD FOREIGN KEY ("muscle_group_id") REFERENCES "muscle_groups" ("id") DEFERRABLE INITIALLY IMMEDIATE;

ALTER TABLE "session_sets" ADD FOREIGN KEY ("exercise_id") REFERENCES "exercises" ("id") DEFERRABLE INITIALLY IMMEDIATE;

ALTER TABLE "template_exercises" ADD FOREIGN KEY ("template_id") REFERENCES "workout_templates" ("id") ON DELETE CASCADE DEFERRABLE INITIALLY IMMEDIATE;

ALTER TABLE "exercise_muscle_groups" ADD FOREIGN KEY ("exercise_id") REFERENCES "exercises" ("id") ON DELETE CASCADE DEFERRABLE INITIALLY IMMEDIATE;

ALTER TABLE "session_sets" ADD FOREIGN KEY ("session_id") REFERENCES "workout_sessions" ("id") ON DELETE CASCADE DEFERRABLE INITIALLY IMMEDIATE;

ALTER TABLE "workout_sessions" ADD FOREIGN KEY ("template_id") REFERENCES "workout_templates" ("id") ON DELETE SET NULL DEFERRABLE INITIALLY IMMEDIATE;

ALTER TABLE "workout_templates" ADD FOREIGN KEY ("copied_from_id") REFERENCES "workout_templates" ("id") ON DELETE SET NULL DEFERRABLE INITIALLY IMMEDIATE;
