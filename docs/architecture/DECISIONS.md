# DECISIONS

## ADR-001: Use Spring Boot

Spring Boot is the backend framework because it provides mature support
for REST APIs, validation, dependency injection, configuration, and JPA.

## ADR-002: Keep a Layered Architecture

The project uses Controller -> Service -> Repository -> Database. This
keeps HTTP handling, business logic, and persistence separated, which
makes incremental portfolio-quality development easier to review.

## ADR-003: Use PostgreSQL and pgvector

PostgreSQL is the primary relational database. The Docker setup uses the
pgvector image so the project can later support semantic search and RAG
without changing database engines.

## ADR-004: Use Spring AI for LLM Access

Spring AI provides the `ChatClient` abstraction used by submission
review and study-assistant flows. AI integrations should remain behind
services so tests can mock them.

## ADR-005: Preserve Submissions When AI Fails

Submissions are saved before AI review. If the AI provider fails, the
submission is marked `FAILED` instead of losing user work.

## ADR-006: Centralize API Error Responses

API exceptions are handled through a shared `GlobalExceptionHandler`.
This keeps controllers thin and gives clients predictable JSON errors.

## ADR-007: Use an Isolated Test Profile

The `test` profile excludes local database, pgvector, and AI chat-client
auto-configuration. This keeps the default test suite reliable without
requiring Docker, PostgreSQL, or OpenAI credentials.
