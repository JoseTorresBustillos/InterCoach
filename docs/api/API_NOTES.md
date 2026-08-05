# API_NOTES

## Health

- `GET /api/health`

## Authentication

- `POST /api/auth/register`
- `POST /api/auth/login`

Both endpoints return a bearer token, expiration timestamp, and user
summary. Passwords are stored as BCrypt hashes and are never returned in
API responses.

Use the returned token on protected endpoints:

```http
Authorization: Bearer <token>
```

## Problems

- `GET /api/problems`
- `GET /api/problems/{id}`
- `POST /api/problems`
- `PUT /api/problems/{id}`
- `DELETE /api/problems/{id}`

## Test Cases

- `POST /api/problems/{problemId}/test-cases`
- `GET /api/problems/{problemId}/test-cases`

## Users

- `POST /api/users`
- `GET /api/users`
- `GET /api/users/{userId}`

These are protected user-management endpoints. General account creation
should use `/api/auth/register`.

All endpoints except `GET /api/health` and `/api/auth/*` require a
bearer token.

## Submissions

- `POST /api/problems/{problemId}/submissions`
- `GET /api/submissions/{submissionId}`
- `GET /api/problems/{problemId}/submissions`
- `GET /api/users/{userId}/submissions`

Submission creation persists the submission before AI review. Successful
AI review marks the submission `REVIEWED`; AI failure marks it `FAILED`.

## Recommendations

- `GET /api/recommendations`

Recommendations currently use global submission history, not
authenticated user-specific history.

## Study Assistant

- `POST /api/study-assistant/ask`

The assistant currently sends stored problem context to the AI model. It
does not use pgvector retrieval yet.

## Mock Interviews

- `POST /api/users/{userId}/mock-interviews`
- `GET /api/mock-interviews/{sessionId}`
- `GET /api/users/{userId}/mock-interviews`
- `PATCH /api/mock-interviews/{sessionId}/complete`
- `PATCH /api/mock-interviews/{sessionId}/abandon`

## Error Responses

Errors use a shared JSON shape:

```json
{
  "timestamp": "2026-08-05T00:00:00Z",
  "status": 404,
  "error": "Not Found",
  "message": "Problem not found with id: 1",
  "path": "/api/problems/1",
  "fieldErrors": {}
}
```

Validation failures return `400` and include field-level messages in
`fieldErrors`.

Authentication failures return `401`. Authenticated requests without
authorization return `403`.

## Future Endpoints

- `/dashboard/*`
- `/analytics/*`
- Vector-search and RAG endpoints.
