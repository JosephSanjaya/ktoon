# ktoon-ktor API Documentation

Complete API reference for the ktoon-ktor library.

## Package: io.ktoon.ktor

### Extension Functions

#### toon()

```kotlin
fun ContentNegotiationConfig.toon(
    contentType: ContentType = ContentType.parse("application/toon"),
    toon: Toon = Toon()
)
```

Registers TOON format support with Ktor's ContentNegotiation plugin.

This extension function enables automatic serialization and deserialization of Kotlin objects using the token-efficient TOON format for HTTP requests and responses.

**Parameters:**

- `contentType: ContentType` - The content type to register for TOON format.
  - **Default:** `ContentType.parse("application/toon")`
  - **Description:** Specifies which Content-Type header value should trigger TOON serialization/deserialization. The client will use this converter when the request or response has a matching Content-Type.
  - **Example:** `ContentType.parse("application/x-custom-toon")`

- `toon: Toon` - The Toon instance to use for serialization/deserialization.
  - **Default:** `Toon()` (default Toon instance)
  - **Description:** Allows customization of the TOON serialization behavior by providing a custom Toon instance with custom serializers, configuration, or SerializersModule.
  - **Example:** `Toon(serializersModule = customModule)`

**Returns:** `Unit`

**Throws:** None directly, but serialization/deserialization may throw `SerializationException`

**Usage:**

```kotlin
// Basic usage with defaults
val client = HttpClient(CIO) {
    install(ContentNegotiation) {
        toon()
    }
}

// Custom content type
val client = HttpClient(CIO) {
    install(ContentNegotiation) {
        toon(contentType = ContentType.parse("application/x-toon"))
    }
}

// Custom Toon instance
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

**See Also:**
- [ContentNegotiationConfig](https://api.ktor.io/ktor-client/ktor-client-core/io.ktor.client.plugins.contentnegotiation/-content-negotiation-config/index.html)
- [Toon](../ktoon-core/API.md#toon)

---

## Internal Classes

### ToonContentConverter

```kotlin
internal class ToonContentConverter(
    private val toon: Toon = Toon()
) : ContentConverter
```

Internal implementation of Ktor's ContentConverter interface for TOON format.

**Note:** This class is internal and should not be used directly. Use the `toon()` extension function instead.

**Constructor Parameters:**

- `toon: Toon` - The Toon instance to use for serialization/deserialization

**Implements:** `io.ktor.serialization.ContentConverter`

#### Methods

##### serialize()

```kotlin
override suspend fun serialize(
    contentType: ContentType,
    charset: Charset,
    typeInfo: TypeInfo,
    value: Any?
): OutgoingContent?
```

Serializes a Kotlin object to TOON format for HTTP request body.

**Parameters:**

- `contentType: ContentType` - The content type for the request
- `charset: Charset` - The character encoding to use (typically UTF-8)
- `typeInfo: TypeInfo` - Type information for the value to serialize
- `value: Any?` - The object to serialize, or null

**Returns:** `OutgoingContent?` - The serialized content, or null if value is null

**Throws:**
- `SerializationException` - If serialization fails due to unsupported type or encoding error

**Behavior:**

1. Returns `null` immediately if `value` is `null`
2. Obtains the appropriate serializer from the Toon instance's SerializersModule
3. Calls `toon.encodeToString()` to serialize the value to TOON format
4. Converts the TOON string to bytes using the specified charset
5. Wraps the bytes in a `ByteArrayContent` with the correct Content-Type header
6. Propagates any `SerializationException` without modification

**Example TOON Output:**

For a simple object:
```kotlin
@Serializable
data class User(val id: Int, val name: String)
val user = User(123, "Alice")
```

Serializes to:
```
id: 123
name: Alice
```

For a collection:
```kotlin
@Serializable
data class UserList(val users: List<User>)
val userList = UserList(listOf(User(1, "Alice"), User(2, "Bob")))
```

Serializes to:
```
users[2]{id,name}:
  1,Alice
  2,Bob
```

##### deserialize()

```kotlin
override suspend fun deserialize(
    charset: Charset,
    typeInfo: TypeInfo,
    content: ByteReadChannel
): Any?
```

Deserializes TOON format from HTTP response body to a Kotlin object.

**Parameters:**

- `charset: Charset` - The character encoding to use for decoding
- `typeInfo: TypeInfo` - Type information for the target type
- `content: ByteReadChannel` - The response body content stream

**Returns:** `Any?` - The deserialized object, or null for empty nullable types

**Throws:**
- `SerializationException` - If deserialization fails due to malformed TOON, type mismatch, or missing fields

**Behavior:**

1. Reads the entire content stream to a string using the specified charset
2. Checks if content is empty:
   - If empty and type is nullable, returns `null`
   - If empty and type is non-nullable, lets ToonDecoder throw appropriate error
3. Obtains the appropriate deserializer from the Toon instance's SerializersModule
4. Calls `toon.decodeFromString()` to deserialize the TOON string
5. Propagates any `SerializationException` with full context (line numbers, error details)

**Error Messages:**

The deserializer provides detailed error messages with context:

```
Expected field 'name' at line 2:
  1 | id: 123
>>> 2 | email: alice@example.com
  3 |
```

---

## Content Type

### Default Content Type

The default content type for TOON format is:

```
application/toon; charset=UTF-8
```

**Components:**
- **Type:** `application`
- **Subtype:** `toon`
- **Charset:** `UTF-8` (default, but respects server-specified charset)

### Custom Content Types

You can register TOON format with custom content types:

```kotlin
// Custom subtype
toon(contentType = ContentType.parse("application/x-custom-toon"))

// Custom type and subtype
toon(contentType = ContentType.parse("text/toon"))

// With explicit charset
toon(contentType = ContentType.parse("application/toon; charset=ISO-8859-1"))
```

---

## Serialization Behavior

### Request Serialization Flow

```
Kotlin Object
    ↓
TypeInfo extraction
    ↓
Serializer lookup (from SerializersModule)
    ↓
Toon.encodeToString(serializer, value)
    ↓
TOON String
    ↓
String.toByteArray(charset)
    ↓
ByteArrayContent
    ↓
HTTP Request Body
```

### Response Deserialization Flow

```
HTTP Response Body
    ↓
ByteReadChannel
    ↓
readRemaining().readText(charset)
    ↓
TOON String
    ↓
Empty check (nullable handling)
    ↓
Deserializer lookup (from SerializersModule)
    ↓
Toon.decodeFromString(deserializer, string)
    ↓
Kotlin Object
```

---

## Error Handling

### SerializationException

All serialization and deserialization errors are reported as `kotlinx.serialization.SerializationException`.

**Common Error Scenarios:**

#### 1. Unsupported Type

```kotlin
// Attempting to serialize a type without @Serializable
class MyClass(val value: String)  // Missing @Serializable

// Throws: SerializationException: Serializer for class 'MyClass' is not found
```

#### 2. Malformed TOON Format

```kotlin
// Server returns invalid TOON
"""
id: 123
name Alice  // Missing colon
"""

// Throws: SerializationException: Expected ':' after field name at line 2
```

#### 3. Type Mismatch

```kotlin
// Server returns wrong type
"""
id: not-a-number
name: Alice
"""

// Throws: SerializationException: Expected Int for field 'id', got String at line 1
```

#### 4. Missing Required Field

```kotlin
@Serializable
data class User(val id: Int, val name: String)

// Server returns incomplete data
"""
id: 123
"""

// Throws: SerializationException: Missing required field 'name'
```

#### 5. Empty Response for Non-Nullable Type

```kotlin
// Server returns empty body
""

// For non-nullable type:
// Throws: SerializationException: Cannot decode empty content to non-nullable type
```

### Error Context

All deserialization errors include context with line numbers:

```
Error message at line X:
  X-2 | previous line
  X-1 | previous line
>>> X   | line with error
  X+1 | next line
  X+2 | next line
```

---

## Null Handling

### Null Request Bodies

```kotlin
val user: User? = null
client.post("https://api.example.com/users") {
    setBody(user)  // serialize() returns null
}
// Results in empty request body
```

### Empty Response Bodies

```kotlin
// Nullable type - returns null
val user: User? = client.get("https://api.example.com/users/123")
    .body<User?>()  // Returns null for empty response

// Non-nullable type - throws error
val user: User = client.get("https://api.example.com/users/123")
    .body<User>()  // Throws SerializationException for empty response
```

### Null Values in TOON Format

**Indentation Mode:**
```
user:
  id: 123
  name: null
  email: alice@example.com
```

**Table Mode:**
```
users[2]{id,name,email}:
  1,Alice,alice@example.com
  2,Bob,
```
(Empty CSV value represents null)

---

## Charset Support

### Default Charset

UTF-8 is the default charset for TOON format:

```kotlin
contentType(ContentType.parse("application/toon; charset=UTF-8"))
```

### Custom Charsets

Other charsets are supported:

```kotlin
// ISO-8859-1
contentType(ContentType.parse("application/toon; charset=ISO-8859-1"))

// UTF-16
contentType(ContentType.parse("application/toon; charset=UTF-16"))
```

**Note:** The charset specified in the Content-Type header is used for both encoding (requests) and decoding (responses).

---

## Annotation Support

### @Serializable

Required for all types that will be serialized/deserialized:

```kotlin
@Serializable
data class User(val id: Int, val name: String)
```

### @SerialName

Customize field names in TOON format:

```kotlin
@Serializable
data class User(
    @SerialName("user_id") val id: Int,
    @SerialName("full_name") val name: String
)

// Serializes as:
// user_id: 123
// full_name: Alice
```

### @Transient

Exclude fields from serialization:

```kotlin
@Serializable
data class User(
    val id: Int,
    val name: String,
    @Transient val password: String = ""
)

// Serializes as:
// id: 123
// name: Alice
// (password is excluded)
```

### @Contextual

Use custom serializers:

```kotlin
@Serializable
data class Event(
    val id: Int,
    @Contextual val timestamp: LocalDateTime
)

// Requires custom serializer registered in SerializersModule
```

### @Polymorphic

Support polymorphic type hierarchies:

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

// Type information is preserved in TOON format
```

---

## Platform Support

ktoon-ktor works on all Kotlin Multiplatform targets:

- **JVM** (Desktop) - Java 21+
- **Android** - minSdk 24, targetSdk 36
- **iOS** - Kotlin/Native (iosArm64, iosX64, iosSimulatorArm64)
- **JavaScript** - Browser and Node.js
- **WebAssembly** - WasmJs

**Implementation:** Pure Kotlin with no platform-specific code required.

---

## Performance Characteristics

### Time Complexity

- **Serialization:** O(N) where N is the size of the object graph
- **Deserialization:** O(N) where N is the size of the TOON string

### Space Complexity

- **Serialization:** O(N) for the output string
- **Deserialization:** O(N) for the input string and output object

### Memory Usage

- Request and response bodies are held in memory as strings
- Suitable for typical API payloads (< 10MB)
- Not optimized for streaming very large files

### Token Efficiency

Compared to JSON:
- **Simple objects:** ~10-20% reduction
- **Tabular data:** ~30-60% reduction
- **Nested structures:** ~15-30% reduction

---

## Limitations

1. **In-Memory Processing**
   - Entire request/response body is loaded into memory
   - Not suitable for very large payloads (> 10MB)

2. **Content-Type Matching**
   - Requires exact Content-Type match for automatic conversion
   - Case-sensitive content type comparison

3. **No Streaming Support**
   - Cannot stream serialization/deserialization
   - All data must fit in memory

4. **Charset Limitations**
   - Charset must be supported by the platform
   - Invalid charset specifications will cause errors

5. **Type Information Required**
   - TypeInfo must be available at runtime
   - Reified type parameters are used for type safety

---

## Thread Safety

- `ToonContentConverter` is thread-safe
- `Toon` instances are immutable and thread-safe
- Multiple concurrent requests can safely use the same HttpClient instance

---

## Dependencies

### Required Dependencies

```kotlin
// Ktor client core
implementation("io.ktor:ktor-client-core:3.3.3")

// Ktor ContentNegotiation plugin
implementation("io.ktor:ktor-client-content-negotiation:3.3.3")

// ktoon-core for TOON format
implementation(project(":ktoon-core"))

// kotlinx.serialization
implementation("org.jetbrains.kotlinx:kotlinx-serialization-core:1.9.0")
```

### Optional Dependencies

```kotlin
// For testing
testImplementation("io.ktor:ktor-client-mock:3.3.3")
testImplementation("org.jetbrains.kotlin:kotlin-test")
```

---

## Migration Guide

### From JSON to TOON

**Before (JSON):**
```kotlin
val client = HttpClient(CIO) {
    install(ContentNegotiation) {
        json()
    }
}
```

**After (TOON):**
```kotlin
val client = HttpClient(CIO) {
    install(ContentNegotiation) {
        toon()
    }
}

// Change Content-Type in requests
client.post("https://api.example.com/users") {
    contentType(ContentType.parse("application/toon"))  // Changed from Application.Json
    setBody(user)
}
```

### Supporting Both Formats

```kotlin
val client = HttpClient(CIO) {
    install(ContentNegotiation) {
        json()  // Keep JSON support
        toon()  // Add TOON support
    }
}

// Client automatically selects based on Content-Type
```

---

## Best Practices

1. **Reuse HttpClient Instances**
   ```kotlin
   // Good: Single client instance
   object ApiClient {
       val client = HttpClient(CIO) {
           install(ContentNegotiation) { toon() }
       }
   }
   
   // Bad: Creating new client for each request
   fun makeRequest() {
       val client = HttpClient(CIO) { ... }  // Don't do this
   }
   ```

2. **Use Type-Safe Models**
   ```kotlin
   // Good: @Serializable data class
   @Serializable
   data class User(val id: Int, val name: String)
   
   // Bad: Dynamic types
   val user: Map<String, Any> = ...  // Avoid
   ```

3. **Handle Errors Gracefully**
   ```kotlin
   try {
       val user = client.get("...").body<User>()
   } catch (e: SerializationException) {
       logger.error("Deserialization failed", e)
       // Handle error appropriately
   }
   ```

4. **Configure Timeouts**
   ```kotlin
   val client = HttpClient(CIO) {
       install(ContentNegotiation) { toon() }
       install(HttpTimeout) {
           requestTimeoutMillis = 30_000
           connectTimeoutMillis = 10_000
       }
   }
   ```

5. **Use Custom Toon for Specific Needs**
   ```kotlin
   // Don't modify global Toon instance
   // Create custom instance instead
   val customToon = Toon(serializersModule = customModule)
   ```

---

## See Also

- [ktoon-core API Documentation](../ktoon-core/API.md)
- [TOON Format Specification](../ktoon-core/TOON_SPEC.md)
- [Ktor Client Documentation](https://ktor.io/docs/client.html)
- [kotlinx.serialization Guide](https://github.com/Kotlin/kotlinx.serialization/blob/master/docs/serialization-guide.md)
