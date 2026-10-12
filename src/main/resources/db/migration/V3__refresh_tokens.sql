CREATE TABLE "refresh_tokens" (
  "id" bigserial PRIMARY KEY,
  "user_id" bigint NOT NULL,
  "token_hash" varchar(64) UNIQUE NOT NULL,
  "expires_at" timestamptz NOT NULL,
  "revoked_at" timestamptz,
  "created_at" timestamptz NOT NULL DEFAULT (now())
);

CREATE INDEX ON "refresh_tokens" ("user_id");

ALTER TABLE "refresh_tokens" ADD FOREIGN KEY ("user_id") REFERENCES "users" ("id") ON DELETE CASCADE;
