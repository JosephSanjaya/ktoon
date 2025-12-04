# KToon Backend API Server

A minimal Ktor server demonstrating TOON format serialization capabilities. This server showcases how TOON format can reduce token usage by 40-50% compared to JSON when sending structured data to AI models.

## Overview

This backend API server provides a simple REST endpoint that returns user data in either JSON or TOON format based on the `Accept` header. It demonstrates the token efficiency of TOON format for AI interactions.

## Features

- **Dual Format Support**: Responds with JSON or TOON format based on Accept header
- **Content Negotiation**: Automatic format selection using Ktor's ContentNegotiation plugin
- **Token Efficiency**: TOON format uses ~40-50% fewer tokens than JSON for collections
- **Simple Setup**: Minimal configuration with embedded Netty server

## Prerequisites

- JDK 21 or higher
- Gradle (available in environment)

## Running the Server

### Start the server

```bash
gradle :backend:run
```

The server will start on `http://localhost:8080`

You should see:
```
Server started at http://localhost:8080
```

## API Endpoints

### GET /

Returns a welcome message with available endpoints.

```bash
curl http://localhost:8080/
```

### GET /users

Returns a list of users in JSON or TOON format based on the Accept header.

**Default (JSON):**
```bash
curl http://localhost:8080/users
```

**JSON Format:**
```bash
curl -H "Accept: application/json" http://localhost:8080/users
```

**TOON Format:**
```bash
curl -H "Accept: application/toon" http://localhost:8080/users
```

## Format Comparison

### JSON Response (Verbose)

```json
[
  {
    "id": 1,
    "name": "Alice",
    "email": "alice@example.com",
    "age": 28
  },
  {
    "id": 2,
    "name": "Bob",
    "email": "bob@example.com",
    "age": 35
  },
  {
    "id": 3,
    "name": "Charlie",
    "email": "charlie@example.com",
    "age": 42
  },
  {
    "id": 4,
    "name": "Diana",
    "email": "diana@example.com",
    "age": 31
  },
  {
    "id": 5,
    "name": "Eve",
    "email": "eve@example.com",
    "age": 29
  }
]
```

**Token Count:** ~250 tokens

### TOON Response (Compact - Table Mode)

```
users[5]{id,name,email,age}:
  1,Alice,alice@example.com,28
  2,Bob,bob@example.com,35
  3,Charlie,charlie@example.com,42
  4,Diana,diana@example.com,31
  5,Eve,eve@example.com,29
```

**Token Count:** ~120 tokens

### Token Savings

| Format | Tokens | Savings |
|--------|--------|---------|
| JSON   | ~250   | -       |
| TOON   | ~120   | **~52%** |

**Why TOON is More Efficient:**
- Field names appear once in the header instead of repeating for each object
- CSV-style rows eliminate JSON syntax overhead (braces, quotes, colons)
- Compact table format reduces whitespace and structural tokens

## Use Cases

This demo illustrates TOON format benefits for:

- **RAG Applications**: Fit more documents into LLM context windows
- **Data Analysis**: Send large datasets to AI models efficiently
- **API Responses**: Reduce token costs when integrating with AI services
- **Low-Bandwidth Scenarios**: Minimize data transfer for structured content

## Testing with Different Clients

### Using curl with verbose output

```bash
# See full request/response headers
curl -v -H "Accept: application/toon" http://localhost:8080/users
```

### Using HTTPie

```bash
# Install: pip install httpie
http GET http://localhost:8080/users Accept:application/toon
```

### Using Postman

1. Create a GET request to `http://localhost:8080/users`
2. Add header: `Accept: application/toon`
3. Send request and compare with JSON format

## Architecture

```
┌─────────────────────────────────────┐
│         HTTP Client                 │
│   (Accept: application/toon)        │
└──────────────┬──────────────────────┘
               │
               ▼
┌─────────────────────────────────────┐
│         Ktor Server                 │
│  ┌───────────────────────────────┐  │
│  │  ContentNegotiation Plugin    │  │
│  │  - JSON (kotlinx.serialization)│ │
│  │  - TOON (ktoon-ktor-server)   │  │
│  └───────────────────────────────┘  │
│  ┌───────────────────────────────┐  │
│  │  GET /users                   │  │
│  │  Returns: List<User>          │  │
│  └───────────────────────────────┘  │
└─────────────────────────────────────┘
```

## Dependencies

- **Ktor Server**: 3.3.3 (Core, Netty, ContentNegotiation)
- **kotlinx.serialization**: JSON support
- **ktoon-ktor-server**: TOON format ContentNegotiation
- **ktoon-core**: TOON serialization engine
- **Logback**: Logging

## Configuration

The server is configured in `Application.kt`:

- **Port**: 8080
- **Host**: 0.0.0.0 (accessible from all network interfaces)
- **JSON**: Pretty-printed for readability
- **TOON**: Default configuration (table mode for collections)

## Stopping the Server

Press `Ctrl+C` in the terminal where the server is running.

## Troubleshooting

### Port Already in Use

If port 8080 is already in use, you'll see an error. Either:
1. Stop the process using port 8080
2. Modify the port in `Application.kt`

### Module Not Found

If you get module errors, ensure you're running from the project root:
```bash
gradle :backend:run
```

## Next Steps

- Explore the ktoon-ktor-server module for client-side integration
- Check ktoon-core for the TOON serialization implementation
- Try adding your own endpoints with different data models
- Measure actual token counts using your preferred LLM tokenizer

## Related Documentation

- [ktoon-core README](../ktoon-core/README.md) - TOON serialization format
- [ktoon-ktor README](../ktoon-ktor/README.md) - Ktor client integration
- [ktoon-ktor-server README](../ktoon-ktor-server/README.md) - Ktor server integration

## License

Part of the KToon project - Token-efficient serialization for AI-first Kotlin applications.
