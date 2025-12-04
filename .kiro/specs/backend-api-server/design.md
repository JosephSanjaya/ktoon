# Design Document

## Overview

This document describes a minimal Ktor server that demonstrates TOON format serialization. The server has a single endpoint that returns user data in either JSON or TOON format based on the Accept header, showcasing the token efficiency of TOON format.

## Architecture

### Simple Structure

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
│  │  - JSON                       │  │
│  │  - TOON                       │  │
│  └───────────────────────────────┘  │
│  ┌───────────────────────────────┐  │
│  │  GET /users                   │  │
│  │  Returns: List<User>          │  │
│  └───────────────────────────────┘  │
└─────────────────────────────────────┘
```

### Module Structure

```
backend/
├── build.gradle.kts
├── src/
│   └── main/
│       ├── kotlin/
│       │   └── io/
│       │       └── ktoon/
│       │           └── backend/
│       │               ├── Application.kt    # Main entry point + routes
│       │               └── models/
│       │                   └── User.kt       # User data model
│       └── resources/
│           └── logback.xml                   # Logging configuration
└── README.md
```

## Components

### Application Entry Point

**File:** `Application.kt`

```kotlin
package io.ktoon.backend

import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.serialization.kotlinx.json.*
import io.ktoon.ktor.server.toon
import kotlinx.serialization.json.Json

fun main() {
    embeddedServer(Netty, port = 8080, host = "0.0.0.0") {
        // Install ContentNegotiation with JSON and TOON support
        install(ContentNegotiation) {
            json(Json {
                prettyPrint = true
            })
            toon()
        }
        
        // Define routes
        routing {
            get("/users") {
                val users = listOf(
                    User(1, "Alice", "alice@example.com", 28),
                    User(2, "Bob", "bob@example.com", 35),
                    User(3, "Charlie", "charlie@example.com", 42),
                    User(4, "Diana", "diana@example.com", 31),
                    User(5, "Eve", "eve@example.com", 29)
                )
                call.respond(users)
            }
            
            get("/") {
                call.respondText("""
                    KToon Backend Demo
                    
                    Available endpoints:
                    - GET /users (supports JSON and TOON formats)
                    
                    Try:
                    curl -H "Accept: application/json" http://localhost:8080/users
                    curl -H "Accept: application/toon" http://localhost:8080/users
                """.trimIndent())
            }
        }
        
        println("Server started at http://localhost:8080")
    }.start(wait = true)
}
```

### Data Model

**File:** `models/User.kt`

```kotlin
package io.ktoon.backend.models

import kotlinx.serialization.Serializable

@Serializable
data class User(
    val id: Int,
    val name: String,
    val email: String,
    val age: Int
)
```

## TOON Format Example

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
  }
]
```

### TOON Response (Compact - Table Mode)
```
users[5]{id,name,email,age}:
  1,Alice,alice@example.com,28
  2,Bob,bob@example.com,35
  3,Charlie,charlie@example.com,42
  4,Diana,diana@example.com,31
  5,Eve,eve@example.com,29
```

**Token Savings:** TOON format uses ~40-50% fewer tokens by avoiding field name repetition!

## Build Configuration

**File:** `build.gradle.kts`

```kotlin
plugins {
    kotlin("jvm") version "2.2.21"
    kotlin("plugin.serialization") version "2.2.21"
    application
}

group = "io.ktoon"
version = "1.0.0"

repositories {
    mavenCentral()
}

dependencies {
    // Ktor Server
    implementation("io.ktor:ktor-server-core:3.3.3")
    implementation("io.ktor:ktor-server-netty:3.3.3")
    implementation("io.ktor:ktor-server-content-negotiation:3.3.3")
    
    // Serialization
    implementation("io.ktor:ktor-serialization-kotlinx-json:3.3.3")
    implementation(project(":ktoon-ktor-server"))
    implementation(project(":ktoon-core"))
    
    // Logging
    implementation("ch.qos.logback:logback-classic:1.5.15")
}

application {
    mainClass.set("io.ktoon.backend.ApplicationKt")
}
```

## Testing the Server

### Start the server
```bash
gradle :backend:run
```

### Test with curl

**JSON format:**
```bash
curl -H "Accept: application/json" http://localhost:8080/users
```

**TOON format:**
```bash
curl -H "Accept: application/toon" http://localhost:8080/users
```

**Compare token counts:**
- JSON: ~250 tokens
- TOON: ~120 tokens
- **Savings: ~52%**

## Correctness Properties

**Property 1: Content negotiation**
*For any* request with Accept header "application/toon", the response should be in TOON format
**Validates: Requirements 2.2**

**Property 2: TOON table mode**
*For any* collection response in TOON format, the output should use table mode with header-row syntax
**Validates: Requirements 3.2**

**Property 3: Format equivalence**
*For any* request, the data returned in JSON and TOON formats should be semantically equivalent
**Validates: Requirements 2.3**
