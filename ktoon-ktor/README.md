# ktoon-ktor

Ktor ContentNegotiation integration for the TOON (Token-Oriented Object Notation) serialization format.

## Overview

`ktoon-ktor` provides seamless integration between the TOON serialization format and Ktor's HttpClient through the ContentNegotiation plugin. This enables automatic serialization and deserialization of Kotlin objects using the token-efficient TOON format, reducing API costs when communicating with LLMs and other services.

## Features

- **Zero-Configuration Setup**: Works out-of-the-box with sensible defaults
- **Automatic Serialization**: Request bodies are automatically serialized to TOON format
- **Automatic Deserialization**: Response bodies are automatically deserialized from TOON format
- **Custom Configuration**: Support for custom Toon instances with custom serializers
- **Type-Safe**: Full kotlinx.serialization annotation support (@Serializable, @SerialName, @Transient, @Polymorphic)
- **Cross-Platform**: Works on all Kotlin Multiplatform targets (JVM, Android, iOS, JS, Wasm)
- **Error Handling**: Preserves detailed error messages with line numbers and context

## Installation

Add the dependency to your `build.gradle.kts`:

```kotlin
dependencies {
    implementation(project(":ktoon-ktor"))
    // Also requires ktoon-core and Ktor dependencies
    implementation(project(":ktoon-core"))
    implementation("io.ktor:ktor-client-core:3.3.3")
    implementation("io.ktor:ktor-client-content-negotiation:3.3.3")
}
```

## Basic Usage

### Simple Setup

```kotlin
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktoon.ktor.toon
import kotlinx.serialization.Serializable

// Create HTTP client with TOON support
val client = HttpClient(CIO) {
    install(ContentNegotiation) {
        toon()  // Register TOON format with defaults
    }
}

// Define your data models
@Serializable
data class User(val id: Int, val name: String, val email: String)

// Make requests - serialization happens automatically
val response = client.post("https://api.example.com/users") {
    contentType(ContentType.parse("application/toon"))
    setBody(User(1, "Alice", "alice@example.com"))
}.body<User>()

println("Created user: ${response.name}")
```

### Sending Requests

When you set a body with `setBody()`, the ContentNegotiation plugin automatically serializes it to TOON format:

```kotlin
@Serializable
data class CreateUserRequest(
    val name: String,
    val email: String,
    val age: Int
)

val newUser = CreateUserRequest(
    name = "Bob",
    email = "bob@example.com",
    age = 30
)

val response = client.post("https://api.example.com/users") {
    contentType(ContentType.parse("application/toon"))
    setBody(newUser)
}
```

The request body will be serialized as:
```
name: Bob
email: bob@example.com
age: 30
```

### Receiving Responses

Responses with `Content-Type: application/toon` are automatically deserialized:

```kotlin
@Serializable
data class UserList(val users: List<User>)

val response = client.get("https://api.example.com/users") {
    accept(ContentType.parse("application/toon"))
}.body<UserList>()

println("Received ${response.users.size} users")
```

## Custom Configuration

### Custom Toon Instance

You can provide a custom Toon instance with custom serializers:

```kotlin
import io.ktoon.Toon
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.contextual
import kotlinx.datetime.LocalDateTime

// Create custom Toon instance with custom serializers
val customToon = Toon(
    serializersModule = SerializersModule {
        contextual(LocalDateTime::class, LocalDateTimeSerializer)
    }
)

// Use custom Toon instance
val client = HttpClient(CIO) {
    install(ContentNegotiation) {
        toon(toon = customToon)
    }
}
```

### Multiple Content Types

You can register multiple content converters for different formats:

```kotlin
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.serialization.kotlinx.json.*
import io.ktoon.ktor.toon

val client = HttpClient(CIO) {
    install(ContentNegotiation) {
        json()  // For application/json
        toon()  // For application/toon
    }
}

// The client will automatically select the appropriate converter
// based on the Content-Type header
```

### Custom Content Type

You can register TOON format with a custom content type:

```kotlin
val client = HttpClient(CIO) {
    install(ContentNegotiation) {
        toon(contentType = ContentType.parse("application/x-custom-toon"))
    }
}
```

## Error Handling

The library preserves detailed error messages from the TOON decoder:

```kotlin
try {
    val response = client.get("https://api.example.com/users/123")
        .body<User>()
} catch (e: SerializationException) {
    // Error messages include line numbers and context
    println("Deserialization failed: ${e.message}")
    // Example: "Expected field 'name' at line 2:
    //           1 | id: 123
    //        >>> 2 | email: alice@example.com
    //           3 |"
}
```

### Common Error Scenarios

**Malformed TOON Format:**
```kotlin
// Server returns invalid TOON
// Error: "Expected ':' after field name at line 1"
```

**Type Mismatch:**
```kotlin
// Server returns wrong type for field
// Error: "Expected Int for field 'age', got String at line 3"
```

**Missing Required Fields:**
```kotlin
// Server omits non-nullable field
// Error: "Missing required field 'name'"
```

**Unsupported Type:**
```kotlin
// Attempting to serialize unsupported type
// Error: "Serializer for class 'MyClass' is not found"
```

## Null Handling

### Null Request Bodies

Null request bodies are handled gracefully:

```kotlin
val user: User? = null
client.post("https://api.example.com/users") {
    setBody(user)  // Results in empty request body
}
```

### Empty Response Bodies

Empty responses are handled based on type nullability:

```kotlin
// Nullable type - returns null for empty response
val user: User? = client.get("https://api.example.com/users/123")
    .body<User?>()

// Non-nullable type - throws error for empty response
val user: User = client.get("https://api.example.com/users/123")
    .body<User>()  // Throws if response is empty
```

## Charset Support

The library respects charset specifications from Content-Type headers:

```kotlin
// Default charset is UTF-8
contentType(ContentType.parse("application/toon; charset=UTF-8"))

// Other charsets are supported
contentType(ContentType.parse("application/toon; charset=ISO-8859-1"))
```

## Annotation Support

Full support for kotlinx.serialization annotations:

### @SerialName

```kotlin
@Serializable
data class User(
    @SerialName("user_id") val id: Int,
    @SerialName("full_name") val name: String
)

// Serializes as:
// user_id: 1
// full_name: Alice
```

### @Transient

```kotlin
@Serializable
data class User(
    val id: Int,
    val name: String,
    @Transient val password: String = ""
)

// Serializes as:
// id: 1
// name: Alice
// (password field is excluded)
```

### @Polymorphic

```kotlin
@Serializable
sealed class Message {
    @Serializable
    @SerialName("text")
    data class Text(val content: String) : Message()
    
    @Serializable
    @SerialName("image")
    data class Image(val url: String) : Message()
}

// Serializes with type information preserved
```

## Platform Support

`ktoon-ktor` works on all Kotlin Multiplatform targets:

- **JVM** (Desktop)
- **Android** (minSdk 24)
- **iOS** (Kotlin/Native)
- **JavaScript** (Browser and Node.js)
- **WebAssembly** (WasmJs)

## Performance

- **Single-pass serialization**: O(N) time complexity
- **Minimal allocations**: Efficient memory usage
- **Token efficiency**: 30-60% reduction in token usage compared to JSON for tabular data

## Limitations

- **In-memory processing**: Request and response bodies are held in memory as strings
- **Not optimized for streaming**: Not suitable for very large payloads (> 10MB)
- **Content-Type matching**: Requires exact Content-Type match for automatic conversion

## API Reference

### Extension Function

```kotlin
fun ContentNegotiationConfig.toon(
    contentType: ContentType = ContentType.parse("application/toon"),
    toon: Toon = Toon()
)
```

Registers TOON format support with Ktor's ContentNegotiation plugin.

**Parameters:**
- `contentType`: The content type to register for TOON format (default: "application/toon")
- `toon`: The Toon instance to use for serialization/deserialization (default: Toon())

## Examples

See the test files for more examples:
- `ToonContentConverterAnnotationTest.kt` - Annotation support examples
- `ToonContentConverterCustomConfigTest.kt` - Custom configuration examples
- `ToonContentConverterErrorHandlingTest.kt` - Error handling examples
- `ToonContentConverterNullHandlingTest.kt` - Null handling examples
- `ToonContentConverterPolymorphicTest.kt` - Polymorphic type examples

## License

[Your License Here]

## Contributing

[Your Contributing Guidelines Here]
