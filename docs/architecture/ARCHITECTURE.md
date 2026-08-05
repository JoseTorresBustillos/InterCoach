# ARCHITECTURE

## Overview

InterCoach uses a conventional Spring Boot layered backend:

Client -> Controller -> Service -> Repository -> PostgreSQL

Controllers own HTTP concerns and validation. Services own business
rules. Repositories are Spring Data JPA interfaces and contain no
business logic.

## Packages

- `controller`: REST endpoints.
- `service`: Application and business logic.
- `repository`: Spring Data JPA persistence interfaces.
- `model`: JPA entities and enums.
- `dto`: Request and response objects used at API boundaries.
- `exception`: Shared API exception types and JSON error handling.
- `resources`: Runtime Spring configuration.

## Implemented Modules

- Problem management with CRUD operations.
- Test case management per problem.
- Basic user records with unique username and email constraints.
- Authentication with Spring Security, BCrypt password hashes, and
  signed JWT bearer tokens.
- Submission creation, lookup, and AI review status tracking.
- Structured AI feedback persisted on submissions.
- Recommendation ranking based on failed or weak reviewed submissions.
- Study assistant backed by stored problem context.
- Mock interview sessions with random problem selection by difficulty.
- Centralized error response handling.

## Current Data Model

- `Problem`: coding prompt, metadata, examples, constraints, starter
  code, and solution explanation.
- `TestCase`: input/output examples associated with a problem.
- `AppUser`: user identity, email, BCrypt password hash, and role.
- `Submission`: submitted code, language, status, AI feedback, problem,
  and optional user relationship.
- `MockInterviewSession`: user, selected problem, interview status,
  duration, and timestamps.

## AI Flow

Submission review:

Submission -> persist as `PENDING` -> Spring AI `ChatClient` -> OpenAI
response -> structured feedback fields -> status `REVIEWED`.

If AI review fails, the submission remains saved and is marked `FAILED`.

Study assistant:

Question -> load all stored problems -> prompt with problem-bank context
-> Spring AI `ChatClient` -> answer.

This is not true RAG yet. pgvector dependencies and Docker support are
present, but the application does not currently generate embeddings or
retrieve vector matches.

## Error Handling

`GlobalExceptionHandler` maps project exceptions and validation failures
to consistent JSON error responses:

- `ResourceNotFoundException`: 404
- `DuplicateResourceException`: 409
- validation and malformed requests: 400
- `AuthenticationFailedException`: 401
- unhandled exceptions: 500

Spring Security also returns JSON `401` and `403` responses for
unauthenticated or forbidden requests.

## Security Flow

Registration:

`POST /api/auth/register` -> validate unique username/email -> hash
password with BCrypt -> save user -> issue signed JWT.

Login:

`POST /api/auth/login` -> find by username or email -> verify BCrypt
password -> issue signed JWT.

Protected requests:

Authorization header -> `JwtAuthenticationFilter` -> validate token
signature and expiration -> load user details -> set Spring Security
authentication context.

## Testing Architecture

The current test suite includes:

- A Spring context smoke test using the `test` profile.
- Unit tests for JWT generation and validation.
- Unit tests for registration and login behavior.
- Unit tests for submission AI success and failure behavior.
- Unit tests for centralized API error mappings.

The `test` profile excludes database, JPA repository, pgvector, and
Spring AI chat-client auto-configuration so tests do not require local
PostgreSQL, pgvector, Docker, or live OpenAI calls.

## Known Gaps

- No controller integration tests yet.
- No Testcontainers-backed repository integration tests yet.
- No refresh tokens or password reset flow.
- No role-based admin endpoints yet.
- No pgvector embedding pipeline or semantic retrieval yet.
- No code execution sandbox.
- No frontend.
