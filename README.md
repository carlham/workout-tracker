# Workout Tracker

![CI](https://github.com/carlham/workout-tracker/actions/workflows/ci.yml/badge.svg)

A REST API for planning and logging strength training, built with Spring Boot and PostgreSQL.
Users plan workouts from a library of preset exercises or their own, share plans with others,
and log what they actually lifted: live in the gym, set by set, or afterwards.

## Features

- **Accounts:** registration and login with short-lived JWT access tokens and rotating refresh tokens
- **Exercise library:** 41 preset exercises with technique cues and primary/secondary muscle groups, plus private custom exercises
- **Workout templates:** ordered exercises with rep-range targets (e.g. 3 × 8–12) or timed targets (e.g. 3 × 60 s); can be made public, and others can copy them
- **Session logging:** reps and weight or a duration per set, with set numbers assigned automatically; sessions can follow a template
- **Ownership and privacy:** users only see and change what they're allowed to; workout history is private to its owner
- **API documentation:** interactive Swagger UI

## Tech stack

Java 25 · Spring Boot 4 (Web MVC, Data JPA, Security, Validation) · PostgreSQL 17 · Flyway ·
JUnit 5, Mockito, AssertJ, Testcontainers · Docker Compose · GitHub Actions · springdoc-openapi

## Quick start

Requires Java 25 and Docker.

```bash
docker compose up -d db
./mvnw spring-boot:run
```

Then open **http://localhost:8080/swagger-ui.html** and try it out:

1. `POST /api/auth/register` with a username and password (at least 8 characters)
2. `POST /api/auth/login` with the same details and copy the `accessToken` from the response
3. Click **Authorize**, paste the token, and every other endpoint is unlocked

## API overview

| Area | Endpoints |
|---|---|
| Auth | `POST /api/auth/register`, `/login`, `/refresh`, `/logout` |
| Users | `GET /api/users/me` |
| Exercises | `GET`, `POST /api/exercises` · `GET`, `PUT`, `DELETE /api/exercises/{id}` |
| Muscle groups | `GET /api/muscle-groups` · `POST` (admin only) |
| Templates | `GET`, `POST /api/templates` · `GET /api/templates/public` · `GET`, `PUT`, `DELETE /api/templates/{id}` · `POST /api/templates/{id}/copy` |
| Sessions | `GET`, `POST /api/sessions` · `GET`, `PUT`, `DELETE /api/sessions/{id}` · `POST /api/sessions/{id}/sets` · `PUT`, `DELETE /api/sessions/{id}/sets/{setId}` |

Errors are returned as [RFC 9457](https://www.rfc-editor.org/rfc/rfc9457) `ProblemDetail` JSON. Validation
errors include an `errors` object naming each invalid field.

## Design decisions

- **JWT through Spring Security's resource server.** Token validation uses Spring's built-in OAuth2
  resource server support rather than a hand-written filter; the app only issues tokens.
- **Refresh token rotation with reuse detection.** Access tokens live 15 minutes. Refresh tokens live 14 days
  in an HttpOnly, SameSite=Strict cookie, are stored only as SHA-256 hashes, and work once. Reusing a spent
  token suggests theft, so all of that user's refresh tokens are revoked, logging them out everywhere.
- **Ownership on top-level records.** Exercises, templates and sessions carry an owner; template exercises
  and logged sets inherit it from their parent. Other users' private data returns 404 rather than 403, so
  ids can't be probed.
- **The database enforces the rules too.** Validation gives clear 400/409 responses, and constraints back
  it up: partial unique indexes for exercise names, `CHECK` constraints for "rep range or duration", and
  foreign keys that protect history. Exercises in use can't be deleted (409).
- **Deferred unique constraint on template positions.** Replacing a template's exercise list makes Hibernate
  insert new rows before deleting old ones; checking the constraint at commit avoids false conflicts.
- **`BigDecimal` for weights**, since a `double` can't represent values like 82.5 kg exactly.

## Project structure

Code is organised by feature, each package holding its entity, repository, service, controller and DTOs:

```
src/main/java/no/carlham/workouttracker/
├── auth/         login, registration, refresh tokens
├── user/         users and roles
├── security/     security configuration, JWT setup, current user
├── exercise/     exercises and their muscle groups
├── musclegroup/  muscle groups
├── template/     workout templates
├── session/      logged workouts and sets
└── api/          error handling and API documentation
```

Database changes are versioned Flyway migrations in `src/main/resources/db/migration`.

## Tests

```bash
./mvnw test
```

Requires Docker: Testcontainers starts a throwaway PostgreSQL for tests that need a real database.
The suite has three layers:

- **Unit tests** for business rules (ownership, set numbering, token rotation) with mocked repositories
- **Web layer tests** (`@WebMvcTest`) for status codes, validation and security
- **Integration tests** against PostgreSQL for mappings, constraints and complete flows such as
  register → login → refresh, and plan a template → log a session from it

GitHub Actions runs the full suite on every push to `main` and on every pull request.

## Configuration

| Variable | Purpose | Default |
|---|---|---|
| `DB_PASSWORD` | PostgreSQL password | `workout` (matches `docker-compose.yml`) |
| `JWT_SECRET` | Key for signing access tokens, at least 32 characters | A local development value; **always set it in a real deployment** |

Swagger UI is public. To turn it off in a deployment, set `springdoc.api-docs.enabled=false`.

## Roles and ownership

- Preset exercises are visible to everyone and can only be changed by admins. Exercises an admin creates
  become presets; exercises a user creates are private to them.
- Exercise names are unique (ignoring case) among presets and within each user's own exercises.
- Templates are private unless marked public. Anyone can view and copy a public template; only the owner can
  change it. Public templates can only contain preset exercises, so they never expose private ones.
- Sessions are private to the user who logged them, admins included. Deleting a template keeps the sessions
  logged from it.

Usernames are case-insensitive: "Bob" and "bob" are the same account. New users get the `USER` role.
To make someone an admin:

```sql
UPDATE users SET role = 'ADMIN' WHERE username = '<username>';
```

They need to log in again afterwards, since the role is stored in the access token.

## Roadmap

- A Next.js frontend for logging workouts and browsing history
- Progress per exercise over time (e.g. heaviest set and estimated one-rep max)
