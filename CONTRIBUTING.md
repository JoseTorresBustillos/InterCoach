# CONTRIBUTING

## Coding Standards

-   Java 21
-   Constructor injection
-   Small focused methods
-   Meaningful names
-   Concise comments explaining what/why
-   Unit tests for new business logic

## Pull Request Checklist

-   Builds successfully
-   Tests pass
-   No dead code
-   Comments added where appropriate
-   API changes documented
-   Error behavior uses centralized exceptions where possible
-   AI behavior is covered with mocked tests when practical

## Local Verification

```bash
./mvnw test
```

The default tests use the `test` profile and should not require local
PostgreSQL, Docker, pgvector, or OpenAI credentials.
