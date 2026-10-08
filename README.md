# Workout Tracker

![CI](https://github.com/carlham/workout-tracker/actions/workflows/ci.yml/badge.svg)

Spring Boot + PostgreSQL backend for planning and logging workouts. Work in progress.

## Status
- Database schema and Flyway migrations set up
- Muscle groups seeded, with endpoints to list and add them (`/api/muscle-groups`)
- Exercise CRUD endpoints with validation and paging (`/api/exercises`)
- Tests: unit, web layer and repository tests (Testcontainers), run by GitHub Actions on every push
- Next: seed exercises (V3), user login, then workout templates and sessions

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
