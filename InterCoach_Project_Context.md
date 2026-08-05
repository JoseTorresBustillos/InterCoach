# InterCoach Project Context

## Project Overview

InterCoach is an AI-powered interview practice platform built with:

-   Java 21
-   Spring Boot
-   Spring AI
-   PostgreSQL + pgvector
-   Docker
-   Maven
-   JUnit + Mockito

The long-term goal is to provide coding interview practice, AI code
reviews, personalized recommendations, retrieval-augmented study
assistance, and mock technical interviews.

------------------------------------------------------------------------

# Completed Work

## Phase 1 -- Project Foundation

-   Spring Boot project created
-   Maven configured
-   Docker environment established
-   PostgreSQL configured

## Phase 2 -- Database Layer

-   Entity models created
-   Repository layer implemented
-   Database connectivity verified

## Phase 3 -- CRUD API

-   Problem CRUD endpoints implemented
-   Basic REST architecture established

## Phase 4 -- Submission System

-   Submission model created
-   Submission persistence implemented
-   Relationship between problems and submissions established

## Phase 5 -- AI Feedback

Completed: - Spring AI integrated - OpenAI API configured - AI feedback
service created - Prompt engineering for interview-style reviews -
Submission review flow implemented

Result: 1. Submission saved 2. AI reviews solution 3. Feedback stored 4.
Submission marked as REVIEWED

## Phase 6

Completed successfully. Infrastructure improvements and AI integration
finalized.

## Phase 7

Completed successfully.

## Phase 8

Completed successfully.

## Phase 9

Completed after debugging.

## Phase 10

Completed.

## Phase 11

Completed stabilization work:

-   Centralized JSON API error handling.
-   Reliable test profile that avoids local PostgreSQL, pgvector, and
    OpenAI.
-   Mocked submission AI review tests.
-   API error handler tests.
-   Documentation synchronized with implemented endpoints.

Current stopping point: authentication is the next major feature
milestone.

------------------------------------------------------------------------

# Development Practices

## Code Style

Always include concise comments that explain: - what an important
section does - why it exists

Avoid obvious or excessive comments.

## Architecture

Continue favoring: - Service layer - Repository layer - DTOs where
appropriate - Constructor dependency injection - Clean package
organization

------------------------------------------------------------------------

# Logical Next Steps

The next major milestone is turning the application into a complete
interview platform.

## 1. Secure the Application

-   Spring Security
-   User authentication
-   Password hashing
-   Roles
-   JWT authentication

## 2. User Profiles

Store: - interview history - solved problems - strengths - weaknesses -
preferred language - statistics

## 3. Recommendation Engine

Generate personalized: - next problems - difficulty progression - study
plans

using per-user submissions and AI feedback. A basic global
recommendation endpoint already exists.

## 4. pgvector / RAG

Expand the vector database:

Store: - problems - solutions - AI feedback - interview notes -
documentation

Enable semantic search.

## 5. AI Study Assistant

Allow users to ask:

-   "Why was my solution inefficient?"
-   "Show similar DFS problems."
-   "Explain dynamic programming."

using RAG.

Current implementation uses stored problem context directly. Semantic
retrieval is future work.

## 6. Mock Interview Mode

Create interview sessions:

-   richer timed question flows
-   AI interviewer
-   hints
-   follow-up questions
-   final evaluation

Basic session start, complete, and abandon endpoints already exist.

## 7. Code Execution Sandbox

Safely execute submitted code.

Possible future technologies: - Docker isolation - Test-case execution -
Resource limits

## 8. Testing

Increase coverage: - controller tests - repository integration tests -
Testcontainers - AI prompt tests

## 9. Documentation

Add: - architecture diagrams - API documentation - setup guide -
deployment guide

## 10. Deployment

Potential stack: - Docker Compose - Fly.io / Railway / Render - GitHub
Actions CI/CD

------------------------------------------------------------------------

# High-Level Vision

InterCoach should evolve into a platform where users can:

1.  Solve coding problems
2.  Receive AI interview feedback
3.  Track long-term progress
4.  Receive personalized recommendations
5.  Chat with an AI study coach
6.  Practice full mock interviews
7.  Build a portfolio of interview performance

------------------------------------------------------------------------

# Notes for Future Conversations

-   Preserve the layered Spring Boot architecture.
-   Prefer clean, maintainable code over shortcuts.
-   Keep AI prompts focused and deterministic.
-   Continue adding concise explanatory comments to generated Java code.
-   Build features incrementally with testing after each major
    milestone.
