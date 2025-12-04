# Design Document

## Overview

This document describes the design for integrating the TOON serialization format with Ktor's HttpClient through the ContentNegotiation plugin. The integration provides a ContentConverter implementation that enables automatic serialization and deserialization of Kotlin objects using the token-efficient TOON format for HTTP requests and responses.

The design leverages Ktor's existing ContentNegotiation infrastructure and the ktoon-core library's StringFormat implementation to provide seamless integration with minimal code and maximum compatibility across all Kotlin Multiplatform targets.

## Architecture

### High-Level Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                    Ktor HttpClient                          │
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

The ktoon-ktor module will contain:

```
ktoon-ktor/
└── src/
    └── commonMain/
        └── kotlin/
            └── io/
                └── ktoon/
                    └── ktor/
                        ├── ToonContentConverter.kt    # ContentConverter implementation
                        └── ContentNegotiationExt.kt   # Extension functions for registration
```

### Design Principles

1. **Minimal API Surface**: Provide a simple extension function for registration
2. **Zero Configuration**: Work out-of-the-box with sensible defaults
3. **Customizable**: Allow custom Toon instances for advanced use cases
4. **Platform Agnostic**: Pure Kotlin implementation with no platform-specific code
5. **Error Transparent**: Preserve original error messages and stack traces
6. **Type Safe**: Leverage kotlinx.serialization's type system

## Components and Interfaces

### ToonContentConverter

The core component implementing Ktor's ContentConverter interface.

**Responsibilities:**
- Serialize Kotlin objects to TOON format for request bodies
- Deserialize TOON format responses to Kotlin objects
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
        value: Any?
    ): OutgoingContent?
    
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
- Return null from serialize() for null values (Ktor handles this)
- Use ByteReadChannel for streaming deserialization
- Preserve charset information from HTTP headers

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
val client = HttpClient(CIO) {
    install(ContentNegotiation) {
        toon()  // Use defaults
        // OR
        toon(toon = Toon(customSerializersModule))  // Custom configuration
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
- **Charset**: `UTF-8` (default, but respects server-specified charset)

### Serialization Flow

**Request Serialization:**
```
Kotlin Object → Toon.encodeToString() → String → ByteArray (UTF-8) → ByteReadChannel
```

**Response Deserialization:**
```
ByteReadChannel → ByteArray → String (charset) → Toon.decodeFromString() → Kotlin Object
```

## Correctness Properties

*A property is a characteristic or behavior that should hold true across all valid executions of a system-essentially, a formal statement about what the system should do. Properties serve as the bridge between human-readable specifications and machine-verifiable correctness guarantees.*


### Property Reflection

After analyzing all acceptance criteria, I've identified the following redundancies and consolidations:

- Properties 5.1-5.5 (platform-specific tests) are redundant since the implementation is pure Kotlin and platform support is already verified by ktoon-core
- Properties 6.1-6.4 (annotation support) are redundant since they're already tested in ktoon-core and we're just delegating to Toon
- Property 2.1 and 3.1 can be combined into a single round-trip property
- Properties 2.2 and 2.3 can be combined into a single property about correct output format
- Error handling properties (4.1-4.4) are specific examples, not universal properties

After consolidation, we have the following unique, non-redundant properties:

Property 1: Round-trip serialization through HTTP
*For any* @Serializable object, serializing it through the ContentConverter and then deserializing the result should produce an equivalent object
**Validates: Requirements 2.1, 3.1, 3.2, 6.1**

Property 2: Content-Type header correctness
*For any* serializable object, when serialized through the ContentConverter, the output should have Content-Type "application/toon" with UTF-8 charset
**Validates: Requirements 1.2, 1.3, 2.2, 2.3**

Property 3: Content-Type selection
*For any* set of registered content converters, when a request/response has Content-Type "application/toon", the ToonContentConverter should be selected
**Validates: Requirements 1.4**

Property 4: Charset handling
*For any* charset specified in the response Content-Type header, the ContentConverter should decode the byte stream using that charset
**Validates: Requirements 3.5**

Property 5: Error preservation
*For any* serialization or deserialization error, the ContentConverter should propagate the exception with its original message and stack trace intact
**Validates: Requirements 2.5, 3.4, 4.5**

Property 6: Annotation support delegation
*For any* @Serializable class with @SerialName or @Transient annotations, the ContentConverter should produce the same TOON output as calling Toon.encodeToString directly
**Validates: Requirements 6.2, 6.3**

Property 7: Polymorphic type preservation
*For any* polymorphic type hierarchy, serializing and deserializing through the ContentConverter should preserve the concrete type
**Validates: Requirements 6.4**

Property 8: TypeInfo usage
*For any* generic type parameter, the ContentConverter should use the provided TypeInfo to deserialize to the correct type
**Validates: Requirements 6.5**

Property 9: Custom Toon instance delegation
*For any* custom Toon instance with custom serializers, the ContentConverter should produce the same output as calling that Toon instance's encodeToString directly
**Validates: Requirements 7.3**

## Error Handling

### Serialization Errors

**Error Types:**
1. **Unsupported Type**: When attempting to serialize a type not supported by TOON format
2. **Encoding Failure**: When the Toon encoder encounters an invalid state
3. **Charset Encoding Failure**: When converting string to bytes fails

**Handling Strategy:**
- Catch SerializationException from Toon.encodeToString()
- Wrap in appropriate Ktor exception if needed
- Preserve original error message and stack trace
- Do not add additional context that might confuse debugging

**Example:**
```kotlin
try {
    val toonString = toon.encodeToString(serializer, value)
    // ... convert to ByteReadChannel
} catch (e: SerializationException) {
    throw e  // Propagate as-is
}
```

### Deserialization Errors

**Error Types:**
1. **Malformed TOON**: Invalid TOON format syntax
2. **Type Mismatch**: TOON data doesn't match expected type
3. **Missing Fields**: Required fields not present in TOON data
4. **Charset Decoding Failure**: Cannot decode bytes with specified charset

**Handling Strategy:**
- Catch SerializationException from Toon.decodeFromString()
- Preserve line numbers and context snippets from ToonDecoder
- Propagate exception without modification
- Let Ktor handle HTTP-level error responses

**Example:**
```kotlin
try {
    val content = channel.readRemaining().readText(charset)
    return toon.decodeFromString(deserializer, content)
} catch (e: SerializationException) {
    throw e  // Propagate with full context
}
```

### Null Handling

**Request Body:**
- If value is null, return null from serialize()
- Ktor will handle null body appropriately (empty body or no Content-Length)

**Response Body:**
- If content is empty and type is nullable, return null
- If content is empty and type is non-nullable, let ToonDecoder throw appropriate error
- Rely on Toon format's null handling for "null" keyword in TOON format

## Testing Strategy

### Unit Tests

Unit tests will cover specific scenarios and edge cases:

1. **Registration Test**: Verify toon() extension function can be called and registers converter
2. **Content-Type Test**: Verify "application/toon" content type is recognized
3. **Null Body Test**: Verify null request body is handled correctly
4. **Empty Response Test**: Verify empty response body is handled correctly
5. **Charset Test**: Verify non-UTF-8 charsets are handled correctly
6. **Error Message Test**: Verify error messages are preserved from ToonDecoder

### Property-Based Tests

Property-based tests will verify universal correctness properties using a PBT library (kotest-property or kotlinx-kover):

**Test Configuration:**
- Minimum 100 iterations per property
- Use arbitrary generators for @Serializable data classes
- Test across different data sizes and structures

**Property Tests:**

1. **Property 1: Round-trip serialization through HTTP**
   - Generate random @Serializable objects
   - Serialize through ContentConverter
   - Deserialize the result
   - Verify equality with original
   - **Feature: ktor-content-negotiation, Property 1: Round-trip serialization through HTTP**

2. **Property 2: Content-Type header correctness**
   - Generate random @Serializable objects
   - Serialize through ContentConverter
   - Verify Content-Type is "application/toon; charset=UTF-8"
   - **Feature: ktor-content-negotiation, Property 2: Content-Type header correctness**

3. **Property 3: Content-Type selection**
   - Register multiple converters (JSON, TOON, etc.)
   - Make requests with "application/toon" Content-Type
   - Verify ToonContentConverter is selected
   - **Feature: ktor-content-negotiation, Property 3: Content-Type selection**

4. **Property 4: Charset handling**
   - Generate random @Serializable objects
   - Serialize with different charsets (UTF-8, UTF-16, ISO-8859-1)
   - Deserialize with same charset
   - Verify round-trip equality
   - **Feature: ktor-content-negotiation, Property 4: Charset handling**

5. **Property 5: Error preservation**
   - Generate invalid TOON strings
   - Attempt deserialization
   - Verify SerializationException is thrown
   - Verify error message contains line numbers and context
   - **Feature: ktor-content-negotiation, Property 5: Error preservation**

6. **Property 6: Annotation support delegation**
   - Generate @Serializable classes with @SerialName and @Transient
   - Serialize through ContentConverter
   - Serialize through Toon.encodeToString directly
   - Verify outputs are identical
   - **Feature: ktor-content-negotiation, Property 6: Annotation support delegation**

7. **Property 7: Polymorphic type preservation**
   - Generate polymorphic type hierarchies
   - Serialize concrete instances through ContentConverter
   - Deserialize to base type
   - Verify concrete type is preserved
   - **Feature: ktor-content-negotiation, Property 7: Polymorphic type preservation**

8. **Property 8: TypeInfo usage**
   - Generate generic types (List<T>, Map<K,V>, etc.)
   - Serialize through ContentConverter
   - Deserialize with correct TypeInfo
   - Verify correct type is returned
   - **Feature: ktor-content-negotiation, Property 8: TypeInfo usage**

9. **Property 9: Custom Toon instance delegation**
   - Create custom Toon instance with custom serializers
   - Serialize through ContentConverter with custom Toon
   - Serialize through custom Toon.encodeToString directly
   - Verify outputs are identical
   - **Feature: ktor-content-negotiation, Property 9: Custom Toon instance delegation**

### Integration Tests

Integration tests will verify end-to-end functionality with real HTTP clients:

1. **Mock Server Test**: Use Ktor's test client to verify request/response handling
2. **Real HTTP Test**: Test against a real server (if feasible in CI)
3. **Platform Test**: Verify on at least JVM and one other platform (Android or iOS)

### Testing Framework

- **Unit Tests**: kotlin.test
- **Property-Based Tests**: kotest-property or kotlinx-kover (to be determined based on KMP support)
- **Integration Tests**: ktor-client-mock for mock server tests
- **Assertions**: kotlin.test assertions

## Implementation Notes

### Dependencies

**Required:**
- `io.ktor:ktor-client-core` - Core Ktor client functionality
- `io.ktor:ktor-client-content-negotiation` - ContentNegotiation plugin
- `ktoon-core` - TOON format implementation

**Optional (for testing):**
- `io.ktor:ktor-client-mock` - Mock client for testing
- `kotest-property` or `kotlinx-kover` - Property-based testing
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
- Respect charset from Content-Type header for responses

### Performance Considerations

**Serialization:**
- Single-pass O(N) serialization from ktoon-core
- No intermediate buffering beyond Toon's internal StringBuilder
- Direct conversion to ByteReadChannel

**Deserialization:**
- Read entire response into String (required for TOON parsing)
- Single-pass O(N) deserialization from ktoon-core
- No additional allocations beyond Toon's internal structures

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
val client = HttpClient(CIO) {
    install(ContentNegotiation) {
        toon()
    }
}

@Serializable
data class User(val id: Int, val name: String)

// Automatic serialization
val response = client.post("https://api.example.com/users") {
    contentType(ContentType.parse("application/toon"))
    setBody(User(1, "Alice"))
}.body<User>()
```

**Custom Configuration:**
```kotlin
val customToon = Toon(
    serializersModule = SerializersModule {
        contextual(LocalDateTime::class, LocalDateTimeSerializer)
    }
)

val client = HttpClient(CIO) {
    install(ContentNegotiation) {
        toon(toon = customToon)
    }
}
```

**Multiple Content Types:**
```kotlin
val client = HttpClient(CIO) {
    install(ContentNegotiation) {
        json()  // For application/json
        toon()  // For application/toon
    }
}

// Client will use appropriate converter based on Content-Type
```

## Security Considerations

### Input Validation

- Rely on ToonDecoder's validation for TOON format
- No additional validation needed at converter level
- Malformed input will throw SerializationException

### Error Information Disclosure

- Error messages from ToonDecoder include line numbers and context
- This is acceptable for debugging and does not expose sensitive data
- Do not add request/response headers or URLs to error messages

### Charset Handling

- Default to UTF-8 to prevent encoding attacks
- Validate charset from Content-Type header
- Reject unsupported charsets with clear error message

## Future Enhancements

### Potential Improvements

1. **Streaming Support**: Add streaming serialization/deserialization for large payloads
2. **Compression**: Integrate with Ktor's compression features
3. **Validation**: Add schema validation for TOON format
4. **Metrics**: Add serialization/deserialization metrics
5. **Caching**: Cache serialized representations for immutable objects

### Compatibility

- Maintain backward compatibility with ktoon-core
- Follow semantic versioning
- Deprecate features before removal
- Provide migration guides for breaking changes
