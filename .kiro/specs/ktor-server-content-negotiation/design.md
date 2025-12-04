# Design Document

## Overview

This document describes the design for integrating the TOON serialization format with Ktor Server through the ContentNegotiation plugin. The integration provides a ContentConverter implementation that enables automatic serialization and deserialization of Kotlin objects using the token-efficient TOON format for HTTP request and response bodies in server applications.

The design leverages Ktor Server's ContentNegotiation infrastructure and the ktoon-core library's StringFormat implementation to provide seamless integration with minimal code. This complements the existing ktoon-ktor client-side implementation, enabling full-stack TOON format support.

## Architecture

### High-Level Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                    Ktor Server                              │
│  ┌───────────────────────────────────────────────────────┐  │
│  │         ContentNegotiation Plugin                     │  │
│  │  ┌─────────────────────────────────────────────────┐  │  │
│  │  │      ToonContentConverter                       │  │  │
│  │  │  ┌───────────────┐    ┌───────────────────┐    │  │  │
│  │  │  │  serialize()  │    │  deserialize()    │    │  │  │
│  │  │  └───────┬───────┘    └────────┬──────────┘    │  │  │
│  │  └──────────┼──────────────────────┼──────────────┘  │  │
│  └─────────────┼──────────────────────┼─────────────────┘  │
└────────────────┼──────────────────────┼────────────────────┘
                 │                      │
                 ▼                      ▼
        ┌────────────────────────────────────┐
        │         ktoon-core                 │
        │  ┌──────────────────────────────┐  │
        │  │  Toon.encodeToString()       │  │
        │  │  Toon.decodeFromString()     │  │
        │  └──────────────────────────────┘  │
        └────────────────────────────────────┘
```

### Module Structure

The ktoon-ktor-server module will contain:

```
ktoon-ktor-server/
└── src/
    ├── commonMain/
    │   └── kotlin/
    │       └── io/
    │           └── ktoon/
    │               └── ktor/
    │                   └── server/
    │                       ├── ToonContentConverter.kt    # ContentConverter implementation
    │                       └── ContentNegotiationExt.kt   # Extension functions for registration
    └── commonTest/
        └── kotlin/
            └── io/
                └── ktoon/
                    └── ktor/
                        └── server/
                            ├── ToonContentConverterTest.kt
                            ├── ToonServerAnnotationTest.kt
                            ├── ToonServerErrorHandlingTest.kt
                            └── ToonServerIntegrationTest.kt
```

### Design Principles

1. **Minimal API Surface**: Provide a simple extension function for registration
2. **Zero Configuration**: Work out-of-the-box with sensible defaults
3. **Customizable**: Allow custom Toon instances for advanced use cases
4. **Platform Agnostic**: Pure Kotlin implementation with no platform-specific code
5. **Error Transparent**: Preserve original error messages and stack traces
6. **Type Safe**: Leverage kotlinx.serialization's type system
7. **Pipeline Compatible**: Integrate seamlessly with Ktor Server's pipeline and other plugins

## Components and Interfaces

### ToonContentConverter

The core component implementing Ktor Server's ContentConverter interface.

**Responsibilities:**
- Deserialize TOON format request bodies to Kotlin objects
- Serialize Kotlin objects to TOON format for response bodies
- Handle charset encoding/decoding
- Propagate serialization errors with context

**Interface:**
```kotlin
class ToonContentConverter(
    private val toon: Toon = Toon.Default
) : ContentConverter {
    override suspend fun serialize(
        contentType: ContentType,
        charset: Charset,
        typeInfo: TypeInfo,
        value: Any
    ): OutgoingContent
    
    override suspend fun deserialize(
        charset: Charset,
        typeInfo: TypeInfo,
        content: ByteReadChannel
    ): Any?
}
```

**Key Design Decisions:**
- Accept custom Toon instance in constructor for flexibility
- Use Toon.Default as default for zero-configuration usage
- Return OutgoingContent.ByteArrayContent for serialized responses
- Use ByteReadChannel for streaming deserialization
- Preserve charset information from HTTP headers
- Set Content-Type header with charset in responses

### ContentNegotiation Extension

Extension function for easy registration with ContentNegotiation plugin.

**API:**
```kotlin
fun ContentNegotiation.Configuration.toon(
    contentType: ContentType = ContentType.parse("application/toon"),
    toon: Toon = Toon.Default
)
```

**Usage Example:**
```kotlin
fun Application.module() {
    install(ContentNegotiation) {
        toon()  // Use defaults
        // OR
        toon(toon = Toon(customSerializersModule))  // Custom configuration
    }
    
    routing {
        post("/users") {
            val user = call.receive<User>()  // Automatic deserialization
            // Process user...
            call.respond(user)  // Automatic serialization
        }
    }
}
```

## Data Models

### Content Type

The TOON format uses a custom content type:

```
Content-Type: application/toon; charset=UTF-8
```

**Components:**
- **Type**: `application`
- **Subtype**: `toon`
- **Charset**: `UTF-8` (default, but respects client-specified charset)

### Serialization Flow

**Request Deserialization:**
```
ByteReadChannel → ByteArray → String (charset) → Toon.decodeFromString() → Kotlin Object
```

**Response Serialization:**
```
Kotlin Object → Toon.encodeToString() → String → ByteArray (UTF-8) → OutgoingContent
```

## Correctness Properties

*A property is a characteristic or behavior that should hold true across all valid executions of a system-essentially, a formal statement about what the system should do. Properties serve as the bridge between human-readable specifications and machine-verifiable correctness guarantees.*

### Property Reflection

After analyzing all acceptance criteria, I've identified the following consolidations:

- Properties for annotation support (@SerialName, @Transient, @Polymorphic) are redundant since they're already tested in ktoon-core and we're delegating to Toon
- Error handling properties are specific examples, not universal properties
- Platform-specific tests are redundant since the implementation is pure Kotlin
- Properties 2.1 and 3.1 can be combined into a single round-trip property

After consolidation, we have the following unique, non-redundant properties:

Property 1: Round-trip serialization through HTTP server
*For any* @Serializable object, receiving it in a request body and sending it in a response body should produce an equivalent object on the client side
**Validates: Requirements 2.2, 3.2**

Property 2: Content-Type header correctness
*For any* serializable object, when serialized as a response, the Content-Type header should be "application/toon; charset=utf-8"
**Validates: Requirements 3.2, 4.3**

Property 3: Request Content-Type recognition
*For any* request with Content-Type "application/toon", the ToonContentConverter should be selected for deserialization
**Validates: Requirements 2.1**

Property 4: Charset handling in requests
*For any* charset specified in the request Content-Type header, the ContentConverter should decode the byte stream using that charset
**Validates: Requirements 2.2, 4.2**

Property 5: Error preservation
*For any* serialization or deserialization error, the ContentConverter should propagate the exception with its original message and stack trace intact
**Validates: Requirements 2.3, 3.3, 5.1, 5.2, 5.3, 5.4, 5.5**

Property 6: Annotation support delegation
*For any* @Serializable class with @SerialName or @Transient annotations, the ContentConverter should produce the same TOON output as calling Toon.encodeToString directly
**Validates: Requirements 6.1, 6.2**

Property 7: Polymorphic type preservation
*For any* polymorphic type hierarchy, deserializing a request and serializing a response should preserve the concrete type
**Validates: Requirements 2.5, 6.3**

Property 8: Collection table mode usage
*For any* response containing a collection, the serialized TOON output should use table mode format
**Validates: Requirements 3.5**

Property 9: Custom Toon instance delegation
*For any* custom Toon instance with custom serializers, the ContentConverter should produce the same output as calling that Toon instance's encodeToString directly
**Validates: Requirements 1.3, 6.4**

Property 10: Pipeline integration
*For any* Ktor Server with multiple plugins installed, the ToonContentConverter should participate correctly in content negotiation based on Accept headers
**Validates: Requirements 7.1, 7.2**

## Error Handling

### Deserialization Errors (Request Bodies)

**Error Types:**
1. **Malformed TOON**: Invalid TOON format syntax in request body
2. **Type Mismatch**: TOON data doesn't match expected type
3. **Missing Fields**: Required fields not present in TOON data
4. **Charset Decoding Failure**: Cannot decode bytes with specified charset

**Handling Strategy:**
- Catch SerializationException from Toon.decodeFromString()
- Preserve line numbers and context snippets from ToonDecoder
- Propagate exception without modification
- Let Ktor's StatusPages plugin handle HTTP error responses (400 Bad Request)

**Example:**
```kotlin
try {
    val content = channel.readRemaining().readText(charset)
    return toon.decodeFromString(deserializer, content)
} catch (e: SerializationException) {
    throw e  // Propagate with full context
}
```

### Serialization Errors (Response Bodies)

**Error Types:**
1. **Unsupported Type**: When attempting to serialize a type not supported by TOON format
2. **Encoding Failure**: When the Toon encoder encounters an invalid state
3. **Charset Encoding Failure**: When converting string to bytes fails

**Handling Strategy:**
- Catch SerializationException from Toon.encodeToString()
- Wrap in appropriate Ktor exception if needed
- Preserve original error message and stack trace
- Let Ktor's StatusPages plugin handle HTTP error responses (500 Internal Server Error)

**Example:**
```kotlin
try {
    val toonString = toon.encodeToString(serializer, value)
    val bytes = toonString.toByteArray(charset)
    return OutgoingContent.ByteArrayContent(bytes, contentType.withCharset(charset))
} catch (e: SerializationException) {
    throw e  // Propagate as-is
}
```

### Null Handling

**Request Body:**
- If content is empty and type is nullable, return null
- If content is empty and type is non-nullable, let ToonDecoder throw appropriate error
- Rely on Toon format's null handling for "null" keyword in TOON format

**Response Body:**
- If value is null, serialize "null" keyword in TOON format
- Ktor will handle null responses appropriately based on route configuration

## Testing Strategy

### Unit Tests

Unit tests will cover specific scenarios and edge cases:

1. **Registration Test**: Verify toon() extension function can be called and registers converter
2. **Content-Type Test**: Verify "application/toon" content type is recognized
3. **Null Body Test**: Verify null request body is handled correctly
4. **Empty Request Test**: Verify empty request body is handled correctly
5. **Charset Test**: Verify non-UTF-8 charsets are handled correctly
6. **Error Message Test**: Verify error messages are preserved from ToonDecoder
7. **Response Header Test**: Verify Content-Type header is set correctly in responses

### Property-Based Tests

Property-based tests will verify universal correctness properties using kotest-property:

**Test Configuration:**
- Minimum 100 iterations per property
- Use arbitrary generators for @Serializable data classes
- Test across different data sizes and structures

**Property Tests:**

1. **Property 1: Round-trip serialization through HTTP server**
   - Generate random @Serializable objects
   - Send as request body to test server
   - Receive as response body
   - Verify equality with original
   - **Feature: ktor-server-content-negotiation, Property 1: Round-trip serialization through HTTP server**

2. **Property 2: Content-Type header correctness**
   - Generate random @Serializable objects
   - Serialize as response through ContentConverter
   - Verify Content-Type is "application/toon; charset=utf-8"
   - **Feature: ktor-server-content-negotiation, Property 2: Content-Type header correctness**

3. **Property 3: Request Content-Type recognition**
   - Send requests with "application/toon" Content-Type
   - Verify ToonContentConverter is selected for deserialization
   - **Feature: ktor-server-content-negotiation, Property 3: Request Content-Type recognition**

4. **Property 4: Charset handling in requests**
   - Generate random @Serializable objects
   - Send with different charsets (UTF-8, UTF-16, ISO-8859-1)
   - Verify correct deserialization
   - **Feature: ktor-server-content-negotiation, Property 4: Charset handling in requests**

5. **Property 5: Error preservation**
   - Generate invalid TOON strings
   - Send as request body
   - Verify SerializationException is thrown
   - Verify error message contains line numbers and context
   - **Feature: ktor-server-content-negotiation, Property 5: Error preservation**

6. **Property 6: Annotation support delegation**
   - Generate @Serializable classes with @SerialName and @Transient
   - Serialize as response through ContentConverter
   - Serialize through Toon.encodeToString directly
   - Verify outputs are identical
   - **Feature: ktor-server-content-negotiation, Property 6: Annotation support delegation**

7. **Property 7: Polymorphic type preservation**
   - Generate polymorphic type hierarchies
   - Send concrete instances in request
   - Return in response
   - Verify concrete type is preserved
   - **Feature: ktor-server-content-negotiation, Property 7: Polymorphic type preservation**

8. **Property 8: Collection table mode usage**
   - Generate collections of objects
   - Serialize as response
   - Verify output uses table mode format (header-row syntax)
   - **Feature: ktor-server-content-negotiation, Property 8: Collection table mode usage**

9. **Property 9: Custom Toon instance delegation**
   - Create custom Toon instance with custom serializers
   - Serialize response through ContentConverter with custom Toon
   - Serialize through custom Toon.encodeToString directly
   - Verify outputs are identical
   - **Feature: ktor-server-content-negotiation, Property 9: Custom Toon instance delegation**

10. **Property 10: Pipeline integration**
    - Install multiple plugins (ContentNegotiation, StatusPages, CallLogging)
    - Send requests with different Accept headers
    - Verify correct converter is selected
    - Verify other plugins function correctly
    - **Feature: ktor-server-content-negotiation, Property 10: Pipeline integration**

### Integration Tests

Integration tests will verify end-to-end functionality with real HTTP servers:

1. **Test Server**: Use Ktor's testApplication to verify request/response handling
2. **Multiple Routes**: Test different route configurations (GET, POST, PUT, DELETE)
3. **Error Handling**: Test with StatusPages plugin for error responses
4. **Platform Test**: Verify on at least JVM and one other platform (if applicable)

### Testing Framework

- **Unit Tests**: kotlin.test
- **Property-Based Tests**: kotest-property
- **Integration Tests**: ktor-server-test-host for test server
- **Assertions**: kotlin.test assertions

## Implementation Notes

### Dependencies

**Required:**
- `io.ktor:ktor-server-core` - Core Ktor server functionality
- `io.ktor:ktor-server-content-negotiation` - ContentNegotiation plugin
- `ktoon-core` - TOON format implementation

**Optional (for testing):**
- `io.ktor:ktor-server-test-host` - Test server for integration tests
- `kotest-property` - Property-based testing
- `kotlin-test` - Test framework

### Platform Considerations

**Pure Kotlin Implementation:**
- No platform-specific code required
- All code in `commonMain` source set
- Uses only standard Kotlin stdlib and Ktor APIs
- Relies on ktoon-core's platform-agnostic implementation

**Charset Handling:**
- Ktor provides charset handling through `Charset` class
- Default to UTF-8 for TOON format
- Respect charset from Content-Type header for requests
- Always send UTF-8 for responses (with charset parameter in Content-Type)

### Performance Considerations

**Deserialization (Request Bodies):**
- Read entire request into String (required for TOON parsing)
- Single-pass O(N) deserialization from ktoon-core
- No additional allocations beyond Toon's internal structures

**Serialization (Response Bodies):**
- Single-pass O(N) serialization from ktoon-core
- No intermediate buffering beyond Toon's internal StringBuilder
- Direct conversion to ByteArray for OutgoingContent

**Memory:**
- Request/response bodies held in memory as strings
- Suitable for typical API payloads (< 10MB)
- Not optimized for streaming large files

## API Design

### Public API

```kotlin
// Extension function for ContentNegotiation.Configuration
fun ContentNegotiation.Configuration.toon(
    contentType: ContentType = ContentType.parse("application/toon"),
    toon: Toon = Toon.Default
)

// ContentConverter implementation (internal)
internal class ToonContentConverter(
    private val toon: Toon
) : ContentConverter
```

### Usage Examples

**Basic Usage:**
```kotlin
fun Application.module() {
    install(ContentNegotiation) {
        toon()
    }
    
    routing {
        post("/users") {
            val user = call.receive<User>()
            // Process user...
            call.respond(user)
        }
        
        get("/users") {
            val users = listOf(
                User(1, "Alice"),
                User(2, "Bob")
            )
            call.respond(users)  // Will use table mode
        }
    }
}
```

**Custom Configuration:**
```kotlin
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

**Multiple Content Types:**
```kotlin
fun Application.module() {
    install(ContentNegotiation) {
        json()  // For application/json
        toon()  // For application/toon
    }
    
    routing {
        post("/users") {
            // Server will use appropriate converter based on Content-Type
            val user = call.receive<User>()
            call.respond(user)
        }
    }
}
```

**Error Handling:**
```kotlin
fun Application.module() {
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
}
```

## Demo Application

### Demo Server Structure

The demo application will showcase TOON server integration:

```
ktoon-ktor-server/
└── src/
    └── jvmMain/
        └── kotlin/
            └── io/
                └── ktoon/
                    └── ktor/
                        └── server/
                            └── demo/
                                ├── DemoServer.kt       # Main server application
                                ├── DemoModels.kt       # Sample data models
                                └── DemoRoutes.kt       # Route definitions
```

### Demo Features

1. **User Management API**:
   - POST /users - Create user (TOON request body)
   - GET /users - List users (TOON response with table mode)
   - GET /users/{id} - Get user by ID (TOON response)
   - PUT /users/{id} - Update user (TOON request/response)
   - DELETE /users/{id} - Delete user

2. **Error Demonstration**:
   - POST /users/invalid - Endpoint that triggers serialization errors
   - Demonstrates error handling with StatusPages

3. **Logging**:
   - CallLogging plugin to show TOON serialization in action
   - Log request/response bodies for debugging

4. **Multiple Content Types**:
   - Support both JSON and TOON formats
   - Demonstrate content negotiation based on Accept header

### Demo Models

```kotlin
@Serializable
data class User(
    val id: Int,
    val name: String,
    val email: String,
    val createdAt: String
)

@Serializable
data class CreateUserRequest(
    val name: String,
    val email: String
)

@Serializable
data class ErrorResponse(
    val error: String,
    val message: String
)
```

## Security Considerations

### Input Validation

- Rely on ToonDecoder's validation for TOON format
- No additional validation needed at converter level
- Malformed input will throw SerializationException
- Use StatusPages plugin to convert exceptions to appropriate HTTP responses

### Error Information Disclosure

- Error messages from ToonDecoder include line numbers and context
- This is acceptable for debugging and does not expose sensitive data
- Do not add request/response headers or URLs to error messages
- Consider sanitizing error messages in production environments

### Charset Handling

- Default to UTF-8 to prevent encoding attacks
- Validate charset from Content-Type header
- Reject unsupported charsets with clear error message
- Always send UTF-8 responses to ensure consistency

### Request Size Limits

- Rely on Ktor's built-in request size limits
- Consider adding custom limits for TOON requests if needed
- Document recommended size limits for TOON payloads

## Future Enhancements

### Potential Improvements

1. **Streaming Support**: Add streaming serialization/deserialization for large payloads
2. **Compression**: Integrate with Ktor's compression features
3. **Validation**: Add schema validation for TOON format
4. **Metrics**: Add serialization/deserialization metrics
5. **Caching**: Cache serialized representations for immutable objects
6. **WebSocket Support**: Extend TOON format to WebSocket frames

### Compatibility

- Maintain backward compatibility with ktoon-core
- Follow semantic versioning
- Deprecate features before removal
- Provide migration guides for breaking changes
- Ensure compatibility with ktoon-ktor client implementation
