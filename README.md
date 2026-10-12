# Workout Tracker

![CI](https://github.com/carlham/workout-tracker/actions/workflows/ci.yml/badge.svg)

Spring Boot + PostgreSQL backend for planning and logging workouts. Work in progress.

## Status
- Database schema and Flyway migrations set up
- User registration and login with JWT access tokens and rotating refresh tokens (`/api/auth`)
- Muscle groups seeded, with endpoints to list and add them (`/api/muscle-groups`, adding is admin-only)
- 41 preset exercises seeded with short technique cues, each linked to its primary and secondary muscle groups
- Exercise CRUD endpoints with validation, paging and ownership (`/api/exercises`)
- Tests: unit, web layer, repository and full auth flow tests (Testcontainers), run by GitHub Actions on every push
- Next: workout templates and sessions

## Running locally
```bash
docker compose up -d db
./mvnw spring-boot:run
```

## Running tests
Requires Docker running (Testcontainers starts its own Postgres).
```bash
./mvnw test
```

## Authentication
- `POST /api/auth/register` creates a user, `POST /api/auth/login` returns a 15-minute access token.
- Send the access token as `Authorization: Bearer <token>` on every other request.
- The refresh token (valid 14 days) is set as an HttpOnly cookie. `POST /api/auth/refresh` exchanges it
  for a new access token and a new refresh token. Each refresh token works once; reusing one logs the user
  out everywhere. `POST /api/auth/logout` revokes it.
- Set `JWT_SECRET` (at least 32 characters) in any real deployment. Without it, a local development default is used.

### Ownership
- Preset exercises are visible to everyone and can only be changed by admins.
- Exercises a user creates are private to that user.
- Exercises an admin creates become presets.
- Names are unique (ignoring case) among presets and within each user's own exercises; duplicates return 409.

New users get the `USER` role. To make someone an admin:
```sql
UPDATE users SET role = 'ADMIN' WHERE username = '<username>';
```
They need to log in again afterwards, since the role is stored in the access token.
