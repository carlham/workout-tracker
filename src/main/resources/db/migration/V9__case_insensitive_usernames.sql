-- Usernames are unique ignoring case ("Bob" and "bob" are the same user), matching how registration
-- and login treat them. The existing UNIQUE constraint on username only catches exact matches.
CREATE UNIQUE INDEX "users_username_lower_key" ON "users" (lower("username"));
