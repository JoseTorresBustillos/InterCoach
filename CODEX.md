# CODEX.md

# InterCoach --- Codex Development Guide

This file provides repository-specific instructions for AI coding agents
(especially Codex). Read this after `README.md` and
`PROJECT_CONTEXT.md`.

------------------------------------------------------------------------

## Mission

Build InterCoach into a production-quality AI-powered interview platform
while preserving a clean, maintainable Spring Boot architecture.

Quality is more important than speed.

------------------------------------------------------------------------

## Read These First

1.  README.md
2.  PROJECT_CONTEXT.md
3.  ENGINEERING_HANDBOOK.md
4.  docs/architecture/ARCHITECTURE.md
5.  docs/architecture/DECISIONS.md
6.  ROADMAP.md

Do not begin implementation until you understand the existing
architecture.

------------------------------------------------------------------------

## Tech Stack

-   Java 21
-   Spring Boot
-   Maven
-   PostgreSQL
-   pgvector
-   Spring AI
-   Docker

------------------------------------------------------------------------

## Architectural Rules

Always preserve the layered architecture:

Controller → Service → Repository → Database

Rules:

-   Controllers validate requests and delegate.
-   Services contain business logic.
-   Repositories only perform persistence.
-   Keep dependencies flowing downward only.
-   Prefer constructor injection.
-   Avoid static state.

------------------------------------------------------------------------

## Coding Standards

Generate production-quality Java.

Always:

-   Use meaningful names.
-   Keep methods focused.
-   Keep classes cohesive.
-   Include concise comments explaining *what* important code does and
    *why* it exists.
-   Prefer readability over cleverness.

Avoid:

-   Giant methods
-   Copy/paste code
-   Unnecessary abstraction
-   Premature optimization

------------------------------------------------------------------------

## Development Workflow

For every feature:

1.  Explain the design.
2.  Identify affected packages.
3.  Generate small, reviewable changes.
4.  Suggest tests.
5.  Wait for verification before moving to the next phase.

Never rewrite working code unless necessary.

------------------------------------------------------------------------

## Preferred Order for New Features

1.  Domain model
2.  Repository
3.  Service
4.  DTO
5.  Controller
6.  Tests
7.  Documentation

------------------------------------------------------------------------

## Testing

Every business feature should include:

-   Unit tests
-   Edge-case considerations
-   Failure-path handling

Mock AI integrations when appropriate.

------------------------------------------------------------------------

## Documentation

Whenever a significant feature is completed, update:

-   CHANGELOG.md
-   ROADMAP.md
-   docs/architecture/DECISIONS.md

If architecture changes, also update:

-   docs/architecture/ARCHITECTURE.md
-   ENGINEERING_HANDBOOK.md

------------------------------------------------------------------------

## Future Priorities

Current roadmap:

1.  Controller integration tests
2.  Testcontainers PostgreSQL tests
3.  User Profiles
4.  User-scoped Recommendation Engine
5.  pgvector Search
6.  RAG Study Assistant
7.  Execution Sandbox
8.  Analytics
9.  Deployment

------------------------------------------------------------------------

## Definition of Done

A feature is considered complete when:

-   It compiles.
-   Tests pass.
-   Documentation is updated.
-   Existing behavior is preserved.
-   Code follows project conventions.

------------------------------------------------------------------------

## Final Principle

When multiple implementations are possible, choose the one that is
easiest to maintain six months from now.

Consistency is preferred over novelty.
