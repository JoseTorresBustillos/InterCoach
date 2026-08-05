# CHANGELOG

## Unreleased

-   Added centralized JSON API error handling.
-   Added project exceptions for missing and duplicate resources.
-   Updated services to return consistent 404 and 409 errors.
-   Added a reliable `test` profile that avoids local PostgreSQL,
    pgvector, and live OpenAI dependencies.
-   Added mocked submission AI review tests.
-   Added API error handler tests.
-   Added `README.md`.
-   Added canonical architecture docs under `docs/architecture/`.
-   Updated roadmap, API notes, testing guide, and project context to
    match the implementation.

## Current Snapshot

-   Initial backend established.
-   CRUD API implemented.
-   Submission persistence implemented.
-   Spring AI integrated.
-   AI feedback pipeline operational.
-   Dockerized development environment.
-   PostgreSQL configured.

## Logging Policy

Record: - Feature additions - Refactors - Breaking changes - Dependency
upgrades - Database migrations
