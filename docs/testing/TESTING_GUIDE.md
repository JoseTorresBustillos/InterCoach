# TESTING_GUIDE

## Current Test Command

```bash
./mvnw test
```

The default suite is designed to pass without local PostgreSQL, Docker,
pgvector, or OpenAI credentials.

## Current Coverage

- Spring Boot context smoke test using the `test` profile.
- Submission service tests for AI review success and AI provider
  failure.
- Global exception handler tests for 404 and 409 responses.

## Test Profile

`src/test/resources/application-test.properties` excludes:

- JDBC datasource auto-configuration.
- Hibernate JPA auto-configuration.
- Spring Data JPA repository auto-configuration.
- Spring AI chat-client auto-configuration.
- Spring AI pgvector auto-configuration.

Repository beans and AI infrastructure are mocked in tests so local
developer machines do not need running services for fast feedback.

## Priorities

1. Service unit tests.
2. Controller tests with mocked services.
3. Repository integration tests with Testcontainers PostgreSQL.
4. AI service tests with mocked `ChatClient`.
5. End-to-end happy paths.

## Recommended Stack

- JUnit 5.
- Mockito.
- Spring Boot Test.
- Testcontainers for future database integration tests.
