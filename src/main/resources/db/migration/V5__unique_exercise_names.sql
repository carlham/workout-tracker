-- Exercise names are unique (ignoring case) among presets, and within each user's own exercises.
-- A user may still create a private exercise with the same name as a preset.
CREATE UNIQUE INDEX "exercises_preset_name_key" ON "exercises" (lower("name")) WHERE "owner_id" IS NULL;

CREATE UNIQUE INDEX "exercises_owner_name_key" ON "exercises" ("owner_id", lower("name")) WHERE "owner_id" IS NOT NULL;
