# ENGINEERING_HANDBOOK.md

# InterCoach Engineering Handbook

> Canonical technical reference for the InterCoach project. This
> document is intended to onboard both human developers and AI coding
> agents.

------------------------------------------------------------------------

# 1. Vision

InterCoach is an AI-powered interview preparation platform focused on
helping users become stronger software engineers through iterative
practice.

Core workflow:

1.  Solve coding problems.
2.  Submit solutions.
3.  Receive AI interview feedback.
4.  Track long-term improvement.
5.  Receive personalized recommendations.
6.  Practice realistic mock interviews.

The project prioritizes maintainability over rapid feature growth.

------------------------------------------------------------------------

# 2. Engineering Principles

-   Layered architecture
-   SOLID where practical
-   Constructor dependency injection
-   Small focused classes
-   Thin controllers
-   Business logic in services
-   Repository-only persistence
-   Clear package boundaries
-   Favor readability over cleverness

------------------------------------------------------------------------

# 3. Technology Stack

Backend - Java 21 - Spring Boot - Maven - Spring AI

Database - PostgreSQL - pgvector

Infrastructure - Docker - Docker Compose

Testing - JUnit 5 - Mockito

Future - Spring Security - JWT - Testcontainers - GitHub Actions

------------------------------------------------------------------------

# 4. Current Features

Completed

- Problem CRUD
- Problem test cases
- Basic user records
- Submission persistence
- AI review pipeline
- Structured AI feedback storage
- Spring AI integration
- Basic recommendations
- Basic study assistant
- Basic mock interviews
- Centralized API error handling
- Dockerized development
- PostgreSQL persistence
- Reliable default test suite

------------------------------------------------------------------------

# 5. Architecture

Request

Client

↓

Controller

↓

Service

↓

Repository

↓

Database

Controllers validate and delegate.

Services own business rules.

Repositories never contain business logic.

------------------------------------------------------------------------

# 6. Packages

Recommended layout

controller/ service/ repository/ model/ dto/ config/ exception/
security/ util/

Each package should have a single responsibility.

------------------------------------------------------------------------

# 7. Database Direction

Current entities

-   Problem
-   TestCase
-   AppUser
-   Submission
-   MockInterviewSession

Planned

-   AIFeedback
-   Recommendation
-   Embedding
-   StudyPlan

------------------------------------------------------------------------

# 8. AI Integration

Current

Submission → Prompt → ChatClient → OpenAI → Feedback → Database

Study assistant → Problem context → ChatClient → OpenAI → Answer

Future

RAG

Vector search

Interview simulation

Adaptive tutoring

------------------------------------------------------------------------

# 9. Coding Standards

Always include concise Java comments explaining:

-   what important code does
-   why it exists

Avoid comments that simply restate code.

Methods should generally do one thing.

Favor composition over inheritance.

------------------------------------------------------------------------

# 10. Testing Strategy

Priority

1.  Services
2.  Controllers
3.  Repository integration
4.  AI mocks
5.  End-to-end

Every new business feature should include tests.

------------------------------------------------------------------------

# 11. API Philosophy

RESTful endpoints.

DTOs at API boundaries.

Never expose persistence entities unnecessarily.

------------------------------------------------------------------------

# 12. Error Handling

Centralize exception handling.

Return meaningful HTTP status codes.

Log unexpected failures.

Avoid leaking implementation details.

Current API errors are handled by `GlobalExceptionHandler`, which maps
not-found, duplicate-resource, validation, malformed-request, and
unexpected failures to a shared response body.

------------------------------------------------------------------------

# 13. Security Roadmap

Spring Security

JWT

Password hashing

Role-based authorization

Input validation

Rate limiting (future)

------------------------------------------------------------------------

# 14. Future Modules

Authentication

Profiles

User-scoped Recommendation Engine

Analytics

Semantic Search

RAG Assistant with pgvector retrieval

Full Mock Interviews

Execution Sandbox

Deployment

------------------------------------------------------------------------

# 15. AI Agent Instructions

Before writing code:

Read: - PROJECT_CONTEXT.md - ARCHITECTURE.md - ROADMAP.md

When implementing features:

-   preserve architecture
-   generate incremental commits
-   explain design decisions
-   include concise Java comments
-   suggest tests

Never perform unnecessary rewrites.

------------------------------------------------------------------------

# 16. Release Philosophy

Every completed phase should leave the application in a deployable
state.

Refactor continuously instead of allowing technical debt to accumulate.

------------------------------------------------------------------------

# 17. Repository Maintenance

Keep documentation synchronized with code.

Update:

CHANGELOG.md

ROADMAP.md

DECISIONS.md

after every major feature.

------------------------------------------------------------------------

# 18. Long-Term Goal

InterCoach should become a polished interview platform comparable in
experience to commercial interview-preparation tools while remaining an
excellent example of clean Spring Boot architecture for portfolio
purposes.
