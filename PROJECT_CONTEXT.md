# PROJECT_CONTEXT.md

> Living documentation for the InterCoach project. Update this file
> after every completed feature or milestone.

------------------------------------------------------------------------

# Project

**Name:** InterCoach

InterCoach is an AI-powered coding interview platform built with Java
and Spring Boot. Its objective is to simulate technical interviews while
giving personalized AI feedback, tracking long-term progress, and acting
as an interview study companion.

------------------------------------------------------------------------

# Technology Stack

## Backend

-   Java 21
-   Spring Boot
-   Spring AI
-   Maven

## Database

-   PostgreSQL
-   pgvector

## Infrastructure

-   Docker
-   Docker Compose

## Testing

-   JUnit
-   Mockito

------------------------------------------------------------------------

# Core Architecture

    Controller
        ↓
    Service
        ↓
    Repository
        ↓
    PostgreSQL

Principles: - Constructor injection - Single responsibility - Clean
layered architecture - Small focused services

------------------------------------------------------------------------

# Current Features

## Problems

-   CRUD operations
-   Persistent storage
-   Metadata fields for difficulty, category, tags, examples,
    constraints, starter code, and solution explanation

## Test Cases

-   Create test cases for problems
-   List test cases for a problem
-   Hidden test case flag

## Submissions

-   Submit Java solutions
-   Persist submissions
-   Submission status tracking
-   Associate submissions with problems and users

## AI Review

-   Spring AI integration
-   OpenAI API integration
-   Interview-focused prompt
-   Structured feedback persistence
-   REVIEWED status
-   FAILED status when AI feedback cannot be generated

## Users

-   Basic user records
-   Unique username and email constraints
-   User lookup endpoints
-   No authentication yet

## Recommendations

-   Basic recommendation endpoint
-   Uses failed and weak reviewed submissions to rank problem topics
-   Global history only; user-scoped recommendations are future work

## Study Assistant

-   AI-powered question endpoint
-   Uses stored problem-bank context
-   Does not use pgvector retrieval yet

## Mock Interviews

-   Start interview sessions for a user
-   Random problem selection by requested difficulty
-   Complete and abandon interview states

## API Stability

-   Centralized JSON error responses
-   Validation errors include field-level details

------------------------------------------------------------------------

# Development Conventions

## Java Code

Always include concise comments explaining:

-   what a section does
-   why it exists

Avoid obvious comments.

Example:

``` java
// Save the submission before requesting AI feedback so failures
// never lose user work.
submissionRepository.save(submission);
```

------------------------------------------------------------------------

# Git Workflow

Recommended:

    git pull
    git checkout -b feature/name

    git add .
    git commit -m "Implement feature"

    git push

Merge only after testing.

------------------------------------------------------------------------

# Testing Checklist

Before every commit:

-   Project builds
-   Application starts
-   Database connects
-   Docker services running
-   Endpoint manually tested
-   No failing tests

------------------------------------------------------------------------

# Current Folder Structure

    src/
     ├── controller/
     ├── service/
     ├── repository/
     ├── model/
     ├── dto/
     ├── exception/
     ├── config/
     └── resources/

------------------------------------------------------------------------

# Future Modules

## Authentication

-   Spring Security
-   JWT
-   Password hashing
-   Login flow

## User Profiles

-   Statistics
-   Progress
-   Strengths
-   Weaknesses

## Recommendation Engine

-   User-scoped personalized practice
-   Difficulty progression
-   Study plans

## Vector Search

-   pgvector
-   Embeddings
-   Semantic retrieval

## RAG Study Assistant

Questions like: - Explain this algorithm - Find similar problems -
Review previous mistakes

Current assistant is prompt-context based; true RAG is still future
work.

## Mock Interviews

-   Timed rounds
-   AI interviewer
-   Behavioral questions
-   Coding sessions

## Code Runner

-   Sandboxed execution
-   Hidden test cases
-   Resource limits

## Dashboard

-   Graphs
-   Progress tracking
-   Accuracy
-   Completion history

------------------------------------------------------------------------

# Documentation To Add

-   Architecture diagram
-   ER diagram
-   API reference
-   Sequence diagrams
-   Deployment guide
-   Docker guide
-   Environment setup

------------------------------------------------------------------------

# Technical Debt

Keep track of:

-   TODOs
-   Refactoring opportunities
-   Performance improvements

------------------------------------------------------------------------

# Milestone Checklist

-   [x] Project setup
-   [x] Database
-   [x] CRUD
-   [x] Submission system
-   [x] AI feedback
-   [x] Docker integration
-   [x] Initial testing
-   [x] Basic user records
-   [x] Basic recommendations
-   [x] Basic study assistant
-   [x] Basic mock interviews
-   [x] Centralized API error handling
-   [ ] Authentication
-   [ ] User profiles and dashboard
-   [ ] User-scoped recommendations
-   [ ] pgvector RAG
-   [ ] Sandbox
-   [ ] Frontend
-   [ ] CI/CD
-   [ ] Production deployment

------------------------------------------------------------------------

# Commands

Run application

``` bash
docker compose up -d
./mvnw spring-boot:run
```

Run tests

``` bash
./mvnw test
```

The default tests use the `test` profile and do not require local
PostgreSQL, pgvector, Docker, or OpenAI credentials.

------------------------------------------------------------------------

# Vision

The end goal is to build a polished portfolio-quality application
demonstrating:

-   Spring Boot
-   AI integration
-   Vector databases
-   Clean architecture
-   Secure authentication
-   Testing
-   Docker
-   Production deployment

This repository should showcase full-stack software engineering
practices suitable for internships and entry-level backend software
engineering roles.
