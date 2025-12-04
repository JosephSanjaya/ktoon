# ktoon-ktor-server

TOON format serialization support for Ktor Server applications through ContentNegotiation integration.

## Overview

ktoon-ktor-server enables Ktor Server to automatically serialize response bodies to TOON format and deserialize request bodies from TOON format. TOON (Token-Oriented Object Notation) is a compact serialization format optimized for AI interactions, reducing token usage by 30-60% compared to JSON for tabular data.

This module complements the ktoon-ktor client-side implementation, enabling full-stack TOON format support in Kotlin Multiplatform applications.

## Features

- Automatic request body deserialization from TOON format
- Automatic response body serialization to TOON format
- Custom Toon instance support for advanced configuration
- Full kotlinx.serialization annotation support (@SerialName, @Transient, @Polymorphic)
- Charset handling (UTF-8 default, respects client-specified charset)
- Comprehensive error handling with detailed context
- Seamless integration with Ktor Server pipeline and other plugins
- Zero-configuration usage with sensible defaults

## Installation

### Gradle (Kotlin DSL)

Add the dependency to your `build.gradle.kts`:

```kotlin
dependencies {
    implementation("io.ktoon:ktoon-ktor-server:1.0.0")
}
```

### Gradle (Groovy)

Add the dependency to your `build.gradle`:

```groovy
dependencies {
    implementation 'io.ktoon:ktoon-ktor-server:1.0.0'
}
```

### Maven

Add the dependency to your `pom.xml`:

```xml
<dependency>
    <groupId>io.ktoon</groupId>
    <artifactId>ktoon-ktor-server</artifactId>
    <version>1.0.0</version>
</dependency>
```

## Basic Usage

### Simple Setup

```kotlin
import io.ktor.server.application.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.routing.*
import io.ktoon.ktor.server.toon
import kotlinx.serialization.Serializable

@Serializable
data class User(val id: Int, val name: String)

fun Application.module() {
    install(ContentNegotiation) {
        toon()  // Register TOON format with defaults
    }
    
    routing {
        post("/users") {
            val user = call.receive<User>()  // Automatic deserialization
            // Process user...
            call.respond(user)  // Automatic serialization
        }
        
        get("/users") {
            val users = listOf(
                User(1, "Alice"),
                User(2, "Bob")
            )
            call.respond(users)  // Uses table mode for collections
        }
    }
}
```

### Request/Response Example

**Request (TOON format):**
```
POST /users
Content-Type: application/toon

user:
  id: 1
  name: Alice
```

**Response (TOON format):**
```
HTTP/1.1 200 OK
Content-Type: application/toon; charset=utf-8

user:
  id: 1
  name: Alice
```

**Collection Response (TOON table mode):**
```
GET /users
Accept: application/toon

users[2]{id,name}:
  1,Alice
  2,Bob
```

## Configuration Options

### Custom Content Type

Register TOON format with a custom content type:

```kotlin
install(ContentNegotiation) {
    toon(contentType = ContentType.parse("application/x-toon"))
}
```

### Custom Toon Instance

Use a custom Toon instance with custom serializers:

```kotlin
import io.ktoon.Toon
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.contextual

val customToon = Toon(
    serializersModule = SerializersModule {
        contextual(LocalDateTime::class, LocalDateTimeSerializer)
    }
)

fun Application.module() {
    install(ContentNegotiation) {
        toon(toon = customToon)
    }
}
```

### Multiple Content Types

Support both JSON and TOON formats with content negotiation:

```kotlin
install(ContentNegotiation) {
    json()  // For application/json
    toon()  // For application/toon
}

routing {
    post("/users") {
        // Server automatically uses the appropriate converter
        // based on the Content-Type header
        val user = call.receive<User>()
        call.respond(user)
    }
}
```

Clients can specify their preferred format using the `Accept` header:
- `Accept: application/json` → JSON response
- `Accept: application/toon` → TOON response

### Error Handling

Integrate with StatusPages plugin for custom error responses:

```kotlin
import io.ktor.server.plugins.statuspages.*
import kotlinx.serialization.SerializationException

install(StatusPages) {
    exception<SerializationException> { call, cause ->
        call.respondText(
            text = "Invalid TOON format: ${cause.message}",
            status = HttpStatusCode.BadRequest
        )
    }
}

install(ContentNegotiation) {
    toon()
}
```

## Platform Support

ktoon-ktor-server is a pure Kotlin implementation that works on all Kotlin Multiplatform targets:

- JVM (Desktop, Server)
- Android
- iOS (Kotlin/Native)
- JavaScript (Browser, Node.js)
- WebAssembly

No platform-specific code is required.

## Documentation

- [EXAMPLES.md](EXAMPLES.md) - Comprehensive usage scenarios and examples
- [API.md](API.md) - Complete API reference documentation
- [DEMO.md](DEMO.md) - Demo server application guide

## Requirements

- Kotlin 1.9.0 or higher
- Ktor Server 3.0.0 or higher
- kotlinx.serialization 1.6.0 or higher

## License

[Add your license information here]

## Contributing

[Add contribution guidelines here]

## Related Projects

- [ktoon-core](../ktoon-core) - Core TOON serialization format implementation
- [ktoon-ktor](../ktoon-ktor) - Ktor HttpClient ContentNegotiation integration
