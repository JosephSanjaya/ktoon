# ktoon-ktor Usage Examples

This document provides comprehensive examples for using ktoon-ktor with Ktor's HttpClient.

## Table of Contents

1. [Basic Client Setup](#basic-client-setup)
2. [Custom Toon Instance](#custom-toon-instance)
3. [Multiple Content Types](#multiple-content-types)
4. [Error Handling](#error-handling)
5. [Advanced Scenarios](#advanced-scenarios)

## Basic Client Setup

### Simple GET Request

```kotlin
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktoon.ktor.toon
import kotlinx.serialization.Serializable

// Define your data model
@Serializable
data class User(
    val id: Int,
    val name: String,
    val email: String
)

// Create HTTP client with TOON support
val client = HttpClient(CIO) {
    install(ContentNegotiation) {
        toon()
    }
}

// Make a GET request
suspend fun getUser(userId: Int): User {
    return client.get("https://api.example.com/users/$userId") {
        accept(ContentType.parse("application/toon"))
    }.body<User>()
}

// Usage
val user = getUser(123)
println("User: ${user.name} (${user.email})")
```

### Simple POST Request

```kotlin
@Serializable
data class CreateUserRequest(
    val name: String,
    val email: String,
    val age: Int
)

@Serializable
data class CreateUserResponse(
    val id: Int,
    val name: String,
    val email: String,
    val createdAt: String
)

suspend fun createUser(request: CreateUserRequest): CreateUserResponse {
    return client.post("https://api.example.com/users") {
        contentType(ContentType.parse("application/toon"))
        setBody(request)
    }.body<CreateUserResponse>()
}

// Usage
val newUser = CreateUserRequest(
    name = "Alice",
    email = "alice@example.com",
    age = 28
)
val response = createUser(newUser)
println("Created user with ID: ${response.id}")
```

### Working with Collections

```kotlin
@Serializable
data class UserList(
    val users: List<User>,
    val total: Int,
    val page: Int
)

suspend fun listUsers(page: Int = 1): UserList {
    return client.get("https://api.example.com/users") {
        accept(ContentType.parse("application/toon"))
        parameter("page", page)
    }.body<UserList>()
}

// Usage
val userList = listUsers(page = 1)
println("Found ${userList.total} users")
userList.users.forEach { user ->
    println("- ${user.name}")
}
```

## Custom Toon Instance

### Custom Serializers

```kotlin
import io.ktoon.Toon
import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.contextual
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.toLocalDateTime

// Custom serializer for LocalDateTime
object LocalDateTimeSerializer : KSerializer<LocalDateTime> {
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("LocalDateTime", PrimitiveKind.STRING)
    
    override fun serialize(encoder: Encoder, value: LocalDateTime) {
        encoder.encodeString(value.toString())
    }
    
    override fun deserialize(decoder: Decoder): LocalDateTime {
        return decoder.decodeString().toLocalDateTime()
    }
}

// Create custom Toon instance
val customToon = Toon(
    serializersModule = SerializersModule {
        contextual(LocalDateTime::class, LocalDateTimeSerializer)
    }
)

// Use custom Toon instance with Ktor
val client = HttpClient(CIO) {
    install(ContentNegotiation) {
        toon(toon = customToon)
    }
}

// Now you can use LocalDateTime in your models
@Serializable
data class Event(
    val id: Int,
    val name: String,
    @Contextual val startTime: LocalDateTime,
    @Contextual val endTime: LocalDateTime
)

suspend fun createEvent(event: Event): Event {
    return client.post("https://api.example.com/events") {
        contentType(ContentType.parse("application/toon"))
        setBody(event)
    }.body<Event>()
}
```

### Custom Configuration for Specific Use Cases

```kotlin
import kotlinx.serialization.modules.polymorphic
import kotlinx.serialization.modules.subclass

// Define polymorphic hierarchy
@Serializable
sealed class Notification {
    abstract val id: Int
    abstract val timestamp: String
}

@Serializable
@SerialName("email")
data class EmailNotification(
    override val id: Int,
    override val timestamp: String,
    val recipient: String,
    val subject: String,
    val body: String
) : Notification()

@Serializable
@SerialName("sms")
data class SmsNotification(
    override val id: Int,
    override val timestamp: String,
    val phoneNumber: String,
    val message: String
) : Notification()

// Create Toon instance with polymorphic support
val polymorphicToon = Toon(
    serializersModule = SerializersModule {
        polymorphic(Notification::class) {
            subclass(EmailNotification::class)
            subclass(SmsNotification::class)
        }
    }
)

val client = HttpClient(CIO) {
    install(ContentNegotiation) {
        toon(toon = polymorphicToon)
    }
}

// Use polymorphic types
suspend fun sendNotification(notification: Notification): Boolean {
    val response = client.post("https://api.example.com/notifications") {
        contentType(ContentType.parse("application/toon"))
        setBody(notification)
    }.body<Map<String, Boolean>>()
    return response["success"] ?: false
}

// Usage
val emailNotif = EmailNotification(
    id = 1,
    timestamp = "2024-01-01T10:00:00",
    recipient = "user@example.com",
    subject = "Welcome",
    body = "Welcome to our service!"
)
sendNotification(emailNotif)
```

## Multiple Content Types

### Registering Multiple Formats

```kotlin
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json

val client = HttpClient(CIO) {
    install(ContentNegotiation) {
        // Register JSON for application/json
        json(Json {
            prettyPrint = true
            isLenient = true
        })
        
        // Register TOON for application/toon
        toon()
    }
}

// The client automatically selects the right converter based on Content-Type
suspend fun getUserJson(userId: Int): User {
    return client.get("https://api.example.com/users/$userId") {
        accept(ContentType.Application.Json)
    }.body<User>()
}

suspend fun getUserToon(userId: Int): User {
    return client.get("https://api.example.com/users/$userId") {
        accept(ContentType.parse("application/toon"))
    }.body<User>()
}
```

### Content Negotiation Based on Server Response

```kotlin
// Server decides which format to return based on Accept header
suspend fun getUser(userId: Int, preferToon: Boolean = true): User {
    return client.get("https://api.example.com/users/$userId") {
        if (preferToon) {
            accept(ContentType.parse("application/toon"))
        } else {
            accept(ContentType.Application.Json)
        }
    }.body<User>()
}

// Server will respond with the preferred format if available
val user1 = getUser(123, preferToon = true)   // Gets TOON response
val user2 = getUser(123, preferToon = false)  // Gets JSON response
```

## Error Handling

### Handling Serialization Errors

```kotlin
import kotlinx.serialization.SerializationException

suspend fun createUserSafe(request: CreateUserRequest): Result<CreateUserResponse> {
    return try {
        val response = client.post("https://api.example.com/users") {
            contentType(ContentType.parse("application/toon"))
            setBody(request)
        }.body<CreateUserResponse>()
        Result.success(response)
    } catch (e: SerializationException) {
        println("Serialization failed: ${e.message}")
        Result.failure(e)
    } catch (e: Exception) {
        println("Request failed: ${e.message}")
        Result.failure(e)
    }
}

// Usage
val result = createUserSafe(CreateUserRequest("Bob", "bob@example.com", 30))
result.onSuccess { response ->
    println("User created: ${response.id}")
}.onFailure { error ->
    println("Failed to create user: ${error.message}")
}
```

### Handling Deserialization Errors

```kotlin
@Serializable
data class ApiResponse<T>(
    val success: Boolean,
    val data: T? = null,
    val error: String? = null
)

suspend fun getUserWithErrorHandling(userId: Int): User? {
    return try {
        val response = client.get("https://api.example.com/users/$userId") {
            accept(ContentType.parse("application/toon"))
        }.body<ApiResponse<User>>()
        
        if (response.success && response.data != null) {
            response.data
        } else {
            println("API error: ${response.error}")
            null
        }
    } catch (e: SerializationException) {
        // Detailed error with line numbers and context
        println("Failed to parse response: ${e.message}")
        null
    }
}
```

### Retry Logic with Error Handling

```kotlin
suspend fun <T> retryRequest(
    maxRetries: Int = 3,
    block: suspend () -> T
): Result<T> {
    var lastException: Exception? = null
    
    repeat(maxRetries) { attempt ->
        try {
            return Result.success(block())
        } catch (e: SerializationException) {
            println("Attempt ${attempt + 1} failed: ${e.message}")
            lastException = e
            if (attempt < maxRetries - 1) {
                kotlinx.coroutines.delay(1000L * (attempt + 1))
            }
        }
    }
    
    return Result.failure(lastException ?: Exception("Unknown error"))
}

// Usage
val result = retryRequest {
    client.get("https://api.example.com/users/123") {
        accept(ContentType.parse("application/toon"))
    }.body<User>()
}
```

## Advanced Scenarios

### Streaming Large Collections

```kotlin
@Serializable
data class PagedResponse<T>(
    val items: List<T>,
    val page: Int,
    val totalPages: Int,
    val hasMore: Boolean
)

suspend fun getAllUsers(): List<User> {
    val allUsers = mutableListOf<User>()
    var currentPage = 1
    var hasMore = true
    
    while (hasMore) {
        val response = client.get("https://api.example.com/users") {
            accept(ContentType.parse("application/toon"))
            parameter("page", currentPage)
            parameter("pageSize", 100)
        }.body<PagedResponse<User>>()
        
        allUsers.addAll(response.items)
        hasMore = response.hasMore
        currentPage++
    }
    
    return allUsers
}
```

### Request/Response Interceptors

```kotlin
import io.ktor.client.plugins.*
import io.ktor.client.request.*

val client = HttpClient(CIO) {
    install(ContentNegotiation) {
        toon()
    }
    
    // Add request interceptor
    install(DefaultRequest) {
        header("X-API-Version", "1.0")
        header("X-Client", "ktoon-ktor")
    }
    
    // Add response validation
    HttpResponseValidator {
        validateResponse { response ->
            if (response.status.value >= 400) {
                println("Request failed with status: ${response.status}")
            }
        }
    }
}
```

### Authentication with TOON

```kotlin
import io.ktor.client.plugins.auth.*
import io.ktor.client.plugins.auth.providers.*

@Serializable
data class LoginRequest(val username: String, val password: String)

@Serializable
data class LoginResponse(val token: String, val expiresIn: Int)

val client = HttpClient(CIO) {
    install(ContentNegotiation) {
        toon()
    }
    
    install(Auth) {
        bearer {
            loadTokens {
                // Load tokens from storage
                BearerTokens(accessToken = "your-token", refreshToken = "")
            }
            
            refreshTokens {
                // Refresh tokens when needed
                val response = client.post("https://api.example.com/auth/refresh") {
                    contentType(ContentType.parse("application/toon"))
                    markAsRefreshTokenRequest()
                }.body<LoginResponse>()
                
                BearerTokens(accessToken = response.token, refreshToken = "")
            }
        }
    }
}

suspend fun login(username: String, password: String): LoginResponse {
    return client.post("https://api.example.com/auth/login") {
        contentType(ContentType.parse("application/toon"))
        setBody(LoginRequest(username, password))
    }.body<LoginResponse>()
}
```

### Batch Operations

```kotlin
@Serializable
data class BatchRequest<T>(
    val operations: List<T>
)

@Serializable
data class BatchResponse<T>(
    val results: List<T>,
    val errors: List<String>
)

suspend fun batchCreateUsers(users: List<CreateUserRequest>): BatchResponse<CreateUserResponse> {
    return client.post("https://api.example.com/users/batch") {
        contentType(ContentType.parse("application/toon"))
        setBody(BatchRequest(users))
    }.body<BatchResponse<CreateUserResponse>>()
}

// Usage
val usersToCreate = listOf(
    CreateUserRequest("Alice", "alice@example.com", 28),
    CreateUserRequest("Bob", "bob@example.com", 30),
    CreateUserRequest("Charlie", "charlie@example.com", 25)
)

val batchResult = batchCreateUsers(usersToCreate)
println("Created ${batchResult.results.size} users")
if (batchResult.errors.isNotEmpty()) {
    println("Errors: ${batchResult.errors.joinToString()}")
}
```

### WebSocket with TOON (if supported)

```kotlin
import io.ktor.client.plugins.websocket.*
import io.ktor.websocket.*

@Serializable
data class ChatMessage(
    val userId: Int,
    val message: String,
    val timestamp: String
)

val client = HttpClient(CIO) {
    install(ContentNegotiation) {
        toon()
    }
    install(WebSockets)
}

suspend fun chatSession() {
    client.webSocket("wss://api.example.com/chat") {
        // Send message
        val message = ChatMessage(
            userId = 123,
            message = "Hello!",
            timestamp = "2024-01-01T10:00:00"
        )
        val toonString = Toon().encodeToString(ChatMessage.serializer(), message)
        send(Frame.Text(toonString))
        
        // Receive messages
        for (frame in incoming) {
            if (frame is Frame.Text) {
                val receivedMessage = Toon().decodeFromString(
                    ChatMessage.serializer(),
                    frame.readText()
                )
                println("Received: ${receivedMessage.message}")
            }
        }
    }
}
```

## Testing Examples

### Mock Server Testing

```kotlin
import io.ktor.client.engine.mock.*
import io.ktor.http.*
import io.ktor.utils.io.*
import kotlin.test.Test
import kotlin.test.assertEquals

class UserApiTest {
    @Test
    fun testGetUser() = runTest {
        val mockEngine = MockEngine { request ->
            when (request.url.encodedPath) {
                "/users/123" -> {
                    respond(
                        content = ByteReadChannel("id: 123\nname: Alice\nemail: alice@example.com"),
                        status = HttpStatusCode.OK,
                        headers = headersOf(
                            HttpHeaders.ContentType,
                            "application/toon; charset=UTF-8"
                        )
                    )
                }
                else -> error("Unhandled ${request.url.encodedPath}")
            }
        }
        
        val client = HttpClient(mockEngine) {
            install(ContentNegotiation) {
                toon()
            }
        }
        
        val user = client.get("http://localhost/users/123").body<User>()
        assertEquals(123, user.id)
        assertEquals("Alice", user.name)
    }
}
```

## Best Practices

1. **Reuse HttpClient instances**: Create one client and reuse it throughout your application
2. **Use custom Toon instances for specific needs**: Don't modify the default Toon instance
3. **Handle errors gracefully**: Always wrap requests in try-catch blocks
4. **Set appropriate timeouts**: Configure request timeouts for your use case
5. **Use type-safe models**: Always use @Serializable data classes
6. **Test with mock servers**: Use MockEngine for unit testing
7. **Monitor token usage**: Track the token savings from using TOON format

## Additional Resources

- [ktoon-core Documentation](../ktoon-core/README.md)
- [Ktor Client Documentation](https://ktor.io/docs/client.html)
- [kotlinx.serialization Guide](https://github.com/Kotlin/kotlinx.serialization/blob/master/docs/serialization-guide.md)
