# InterCoach

InterCoach is a Spring Boot backend for coding interview practice. It
stores coding problems, accepts Java submissions, requests AI review
feedback, and exposes early study-coach, recommendation, user, and mock
interview APIs.

## Stack

- Java 21
- Spring Boot
- Spring AI
- Maven
- PostgreSQL
- pgvector
- Docker Compose
- JUnit 5 and Mockito

## Implemented Modules

- Health check: `GET /api/health`
- Problem CRUD: `/api/problems`
- Problem test cases: `/api/problems/{problemId}/test-cases`
- Basic users: `/api/users`
- JWT authentication: `/api/auth/register` and `/api/auth/login`
- Submissions and AI review: `/api/problems/{problemId}/submissions`
- Submission lookup by problem, user, or submission id
- Basic recommendations: `GET /api/recommendations`
- Study assistant: `POST /api/study-assistant/ask`
- Mock interviews: `/api/users/{userId}/mock-interviews`
- Centralized JSON error responses

## Running Locally

Start PostgreSQL:

```bash
docker compose up -d
```

Run the application:

```bash
./mvnw spring-boot:run
```

The OpenAI API key is read from `OPENAI_API_KEY`.

JWT signing uses `JWT_SECRET` when present. The checked-in default is for
local development only.

## Testing

Run the test suite:

```bash
./mvnw test
```

Tests use the `test` profile to avoid requiring local PostgreSQL,
pgvector, or live OpenAI calls. AI behavior is covered through mocked
service tests, and authentication behavior is covered with JWT and auth
service unit tests.

## Documentation

- Project context: `PROJECT_CONTEXT.md`
- Engineering guide: `ENGINEERING_HANDBOOK.md`
- Architecture: `docs/architecture/ARCHITECTURE.md`
- Decisions: `docs/architecture/DECISIONS.md`
- API notes: `docs/api/API_NOTES.md`
- Testing guide: `docs/testing/TESTING_GUIDE.md`
