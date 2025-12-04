# Requirements Document

## Introduction

This document specifies the requirements for a minimal backend API server that demonstrates the TOON format serialization capabilities using the ktoon-ktor-server library. The server will provide a simple endpoint that returns data in TOON format to showcase the token-efficient serialization.

## Glossary

- **Backend API Server**: A standalone Ktor server application providing REST endpoints
- **TOON Format**: Token-Oriented Object Notation, a compact serialization format optimized for AI interactions
- **ktoon-ktor-server**: The library module providing TOON format ContentNegotiation for Ktor Server
- **ContentNegotiation**: Ktor plugin that handles automatic serialization based on Accept headers

## Requirements

### Requirement 1

**User Story:** As a developer, I want to set up a minimal Ktor server with TOON format support, so that I can demonstrate token-efficient serialization.

#### Acceptance Criteria

1. WHEN the server starts THEN the system SHALL initialize on port 8080
2. WHEN the server initializes THEN the system SHALL install ContentNegotiation with TOON format support
3. WHEN the server starts THEN the system SHALL log the server URL
4. WHEN the server receives requests THEN the system SHALL accept both JSON and TOON formats
5. WHEN building the project THEN the system SHALL use Gradle with minimal dependencies

### Requirement 2

**User Story:** As an API consumer, I want a simple endpoint that returns data in TOON format, so that I can see the token-efficient serialization in action.

#### Acceptance Criteria

1. WHEN sending GET /users THEN the system SHALL return a list of users in TOON table mode format
2. WHEN the Accept header is "application/toon" THEN the response SHALL use TOON format
3. WHEN the Accept header is "application/json" THEN the response SHALL use JSON format
4. WHEN no Accept header is provided THEN the system SHALL default to JSON format
5. WHEN users are returned THEN the system SHALL include sample data with multiple users

### Requirement 3

**User Story:** As a developer, I want simple data models with kotlinx.serialization annotations, so that I can see how TOON serialization works.

#### Acceptance Criteria

1. WHEN defining models THEN the system SHALL use @Serializable annotation
2. WHEN models are serialized as collections THEN the system SHALL demonstrate TOON table mode
3. WHEN models have multiple fields THEN the system SHALL show field compression in TOON format
4. WHEN comparing formats THEN TOON SHALL use fewer tokens than JSON for collections
5. WHEN models are defined THEN the system SHALL use simple, clear field names
