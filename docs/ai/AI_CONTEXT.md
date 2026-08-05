# AI_CONTEXT

## Instructions for AI Agents

Always: - Preserve existing architecture. - Extend rather than
rewrite. - Generate incremental changes. - Explain architectural
decisions. - Include concise Java comments. - Suggest tests after major
features.

Avoid: - Massive rewrites - Introducing unnecessary frameworks - Mixing
controller and business logic - Breaking API contracts

When uncertain: Read PROJECT_CONTEXT.md first.

## Current AI Features

- Submission review uses Spring AI `ChatClient` and expects structured
  feedback.
- Study assistant uses stored problem data as prompt context.
- Tests should mock AI services or `ChatClient`; default tests must not
  call OpenAI.

## Current AI Gaps

- No embeddings are generated yet.
- pgvector is available in dependencies and Docker, but no vector search
  flow is implemented.
- Study assistant is not true RAG yet.
