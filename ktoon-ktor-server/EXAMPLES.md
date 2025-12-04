# ktoon-ktor-server Examples

This document provides comprehensive usage scenarios for ktoon-ktor-server, demonstrating various features and integration patterns.

## Table of Contents

- [Basic Server Setup](#basic-server-setup)
- [Custom Toon Instance](#custom-toon-instance)
- [Error Handling](#error-handling)
- [Multiple Content Types](#multiple-content-types)
- [Demo Server Usage](#demo-server-usage)
- [Advanced Scenarios](#advanced-scenarios)

## Basic Server Setup

### Minimal Configuration

The simplest way to add TOON format support to your Ktor Server application:

```kotlin
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.routing.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktoon.ktor.server.toon
import kotlinx.serialization.Serializable

@Serializable
data class Message(val text: String, val timestamp: Long)

fun main() {
    embeddedServer(Netty, port = 8080) {
        install(ContentNegotiation) {
            toon()
        }
        
        routing {
            post("/messages") {
                val message = call.receive<Message>()
                println("Received: ${message.text}")
                call.respond(message)
            }
        }
    }.start(wait = true)
}
```

### CRUD API Example

A complete REST API with CRUD operations:

```kotlin
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktoon.ktor.server.toon
import kotlinx.serialization.Serializable

@Serializable
data class Task(
    val id: Int,
    val title: String,
    val completed: Boolean
)

@Serializable
data class CreateTaskRequest(
    val title: String
)

private val tasks = mutableMapOf<Int, Task>()
private var nextId = 1

fun Application.module() {
    install(ContentNegotiation) {
        toon()
    }
    
    routing {
        route("/tasks") {
            // Create
            post {
                val request = call.receive<CreateTaskRequest>()
                val task = Task(
                    id = nextId++,
                    title = request.title,
                    completed = false
                )
                tasks[task.id] = task
                call.respond(HttpStatusCode.Created, task)
            }
            
            // Read all
            get {
                call.respond(tasks.values.toList())
            }
            
            // Read one
            get("/{id}") {
                val id = call.parameters["id"]?.toIntOrNull()
                    ?: return@get call.respond(HttpStatusCode.BadRequest)
                
                val task = tasks[id]
                    ?: return@get call.respond(HttpStatusCode.NotFound)
                
                call.respond(task)
            }
            
            // Update
            put("/{id}") {
                val id = call.parameters["id"]?.toIntOrNull()
                    ?: return@put call.respond(HttpStatusCode.BadRequest)
                
                val existing = tasks[id]
                    ?: return@put call.respond(HttpStatusCode.NotFound)
                
                val updated = call.receive<Task>()
                tasks[id] = updated.copy(id = id)
                call.respond(tasks[id]!!)
            }
            
            // Delete
            delete("/{id}") {
                val id = call.parameters["id"]?.toIntOrNull()
                    ?: return@delete call.respond(HttpStatusCode.BadRequest)
                
                tasks.remove(id)
                    ?: return@delete call.respond(HttpStatusCode.NotFound)
                
                call.respond(HttpStatusCode.NoContent)
            }
        }
    }
}
```

**Example Requests:**

Create a task:
```
POST /tasks
Content-Type: application/toon

createTaskRequest:
  title: Buy groceries
```

List all tasks (response uses table mode):
```
GET /tasks
Accept: application/toon

tasks[2]{id,title,completed}:
  1,Buy groceries,false
  2,Write documentation,true
```

## Custom Toon Instance

### Custom Serializers

Register custom serializers for types not supported by default:

```kotlin
import io.ktoon.Toon
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.serializers.LocalDateTimeIso8601Serializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.contextual

@Serializable
data class Event(
    val name: String,
    @Contextual
    val scheduledAt: LocalDateTime
)

val customToon = Toon(
    serializersModule = SerializersModule {
        contextual(LocalDateTime::class, LocalDateTimeIso8601Serializer)
    }
)

fun Application.module() {
    install(ContentNegotiation) {
        toon(toon = customToon)
    }
    
    routing {
        post("/events") {
            val event = call.receive<Event>()
            call.respond(event)
        }
    }
}
```

### Polymorphic Types

Handle polymorphic type hierarchies:

```kotlin
import kotlinx.serialization.Serializable
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import kotlinx.serialization.modules.subclass

@Serializable
sealed class Shape {
    abstract val color: String
}

@Serializable
data class Circle(
    override val color: String,
    val radius: Double
) : Shape()

@Serializable
data class Rectangle(
    override val color: String,
    val width: Double,
    val height: Double
) : Shape()

val polymorphicToon = Toon(
    serializersModule = SerializersModule {
        polymorphic(Shape::class) {
            subclass(Circle::class)
            subclass(Rectangle::class)
        }
    }
)

fun Application.module() {
    install(ContentNegotiation) {
        toon(toon = polymorphicToon)
    }
    
    routing {
        post("/shapes") {
            val shape = call.receive<Shape>()
            when (shape) {
                is Circle -> println("Circle with radius ${shape.radius}")
                is Rectangle -> println("Rectangle ${shape.width}x${shape.height}")
            }
            call.respond(shape)
        }
    }
}
```

## Error Handling

### Basic Error Handling

Use StatusPages plugin to handle serialization errors:

```kotlin
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.plugins.statuspages.*
import io.ktor.server.response.*
import io.ktoon.ktor.server.toon
import kotlinx.serialization.SerializationException
import kotlinx.serialization.Serializable

@Serializable
data class ErrorResponse(
    val error: String,
    val message: String
)

fun Application.module() {
    install(StatusPages) {
        exception<SerializationException> { call, cause ->
            call.respond(
                HttpStatusCode.BadRequest,
                ErrorResponse(
                    error = "serialization_error",
                    message = cause.message ?: "Invalid TOON format"
                )
            )
        }
    }
    
    install(ContentNegotiation) {
        toon()
    }
    
    routing {
        post("/data") {
            val data = call.receive<Map<String, String>>()
            call.respond(data)
        }
    }
}
```

### Detailed Error Context

TOON format provides detailed error messages with line numbers and context:

```kotlin
// Invalid request body:
// user:
//   id: not-a-number
//   name: Alice

// Error response:
{
  "error": "serialization_error",
  "message": "Expected integer value at line 2, column 7:\n  1 | user:\n> 2 |   id: not-a-number\n  3 |   name: Alice"
}
```

### Custom Error Handlers

Handle different error types with custom responses:

```kotlin
install(StatusPages) {
    exception<SerializationException> { call, cause ->
        call.respond(
            HttpStatusCode.BadRequest,
            ErrorResponse("invalid_format", cause.message ?: "Invalid data")
        )
    }
    
    exception<IllegalArgumentException> { call, cause ->
        call.respond(
            HttpStatusCode.BadRequest,
            ErrorResponse("invalid_argument", cause.message ?: "Invalid argument")
        )
    }
    
    exception<Exception> { call, cause ->
        call.respond(
            HttpStatusCode.InternalServerError,
            ErrorResponse("internal_error", "An unexpected error occurred")
        )
    }
}
```

## Multiple Content Types

### JSON and TOON Support

Support both JSON and TOON formats with automatic content negotiation:

```kotlin
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.routing.*
import io.ktoon.ktor.server.toon
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
data class Product(
    val id: Int,
    val name: String,
    val price: Double
)

fun Application.module() {
    install(ContentNegotiation) {
        json(Json {
            prettyPrint = true
            isLenient = true
        })
        toon()
    }
    
    routing {
        get("/products") {
            val products = listOf(
                Product(1, "Laptop", 999.99),
                Product(2, "Mouse", 29.99)
            )
            call.respond(products)
        }
    }
}
```

**Client requests:**

Request JSON response:
```
GET /products
Accept: application/json

[
  {
    "id": 1,
    "name": "Laptop",
    "price": 999.99
  },
  {
    "id": 2,
    "name": "Mouse",
    "price": 29.99
  }
]
```

Request TOON response:
```
GET /products
Accept: application/toon

products[2]{id,name,price}:
  1,Laptop,999.99
  2,Mouse,29.99
```

### Content Type Priority

Control which format is used by default:

```kotlin
install(ContentNegotiation) {
    // First registered format has higher priority
    toon()  // Default for ambiguous Accept headers
    json()
}
```

## Demo Server Usage

### Running the Demo Server

The ktoon-ktor-server module includes a complete demo server application. To run it:

```bash
# Using Gradle
gradle :ktoon-ktor-server:run

# Or run the main function directly
gradle :ktoon-ktor-server:runJvm
```

The server starts on `http://localhost:8080`.

### Demo Server Endpoints

The demo server provides a User Management API:

**Create User:**
```bash
curl -X POST http://localhost:8080/users \
  -H "Content-Type: application/toon" \
  -d "createUserRequest:
  name: Alice
  email: alice@example.com"
```

**List Users:**
```bash
curl http://localhost:8080/users \
  -H "Accept: application/toon"
```

Response:
```
users[2]{id,name,email,createdAt}:
  1,Alice,alice@example.com,2024-12-04T10:30:00Z
  2,Bob,bob@example.com,2024-12-04T10:31:00Z
```

**Get User by ID:**
```bash
curl http://localhost:8080/users/1 \
  -H "Accept: application/toon"
```

**Update User:**
```bash
curl -X PUT http://localhost:8080/users/1 \
  -H "Content-Type: application/toon" \
  -d "updateUserRequest:
  name: Alice Smith
  email: alice.smith@example.com"
```

**Delete User:**
```bash
curl -X DELETE http://localhost:8080/users/1
```

**Test Error Handling:**
```bash
curl -X POST http://localhost:8080/users/invalid
```

### Demo Server Features

The demo server demonstrates:

1. **ContentNegotiation**: Supports both JSON and TOON formats
2. **CallLogging**: Logs all requests and responses
3. **StatusPages**: Custom error handling for serialization errors
4. **Compression**: GZIP compression for responses
5. **CRUD Operations**: Complete user management API

## Advanced Scenarios

### Nested Objects

Handle complex nested data structures:

```kotlin
@Serializable
data class Address(
    val street: String,
    val city: String,
    val country: String
)

@Serializable
data class Company(
    val name: String,
    val address: Address
)

@Serializable
data class Employee(
    val id: Int,
    val name: String,
    val company: Company
)

routing {
    post("/employees") {
        val employee = call.receive<Employee>()
        call.respond(employee)
    }
}
```

Request:
```
employee:
  id: 1
  name: Alice
  company:
    name: TechCorp
    address:
      street: 123 Main St
      city: San Francisco
      country: USA
```

### Nullable Fields

Handle optional and nullable fields:

```kotlin
@Serializable
data class Profile(
    val username: String,
    val bio: String?,
    val website: String?
)

routing {
    post("/profiles") {
        val profile = call.receive<Profile>()
        call.respond(profile)
    }
}
```

Request with nulls:
```
profile:
  username: alice
  bio: null
  website: null
```

### Collections and Arrays

Work with lists and arrays:

```kotlin
@Serializable
data class Batch(
    val items: List<String>,
    val tags: List<String>
)

routing {
    post("/batch") {
        val batch = call.receive<Batch>()
        println("Received ${batch.items.size} items")
        call.respond(batch)
    }
}
```

### Custom Content Type

Use a custom content type identifier:

```kotlin
install(ContentNegotiation) {
    toon(contentType = ContentType.parse("application/x-toon"))
}
```

Clients must use the custom content type:
```
POST /data
Content-Type: application/x-toon
Accept: application/x-toon
```

### Integration with Other Plugins

Combine TOON format with other Ktor plugins:

```kotlin
import io.ktor.server.plugins.calllogging.*
import io.ktor.server.plugins.compression.*
import io.ktor.server.plugins.cors.routing.*
import io.ktor.server.plugins.defaultheaders.*

fun Application.module() {
    install(DefaultHeaders)
    
    install(CORS) {
        anyHost()
        allowHeader(HttpHeaders.ContentType)
    }
    
    install(CallLogging) {
        level = Level.INFO
    }
    
    install(Compression) {
        gzip()
    }
    
    install(ContentNegotiation) {
        toon()
    }
    
    routing {
        // Your routes here
    }
}
```

## Testing

### Testing with TestApplication

Test your TOON endpoints using Ktor's test utilities:

```kotlin
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.server.testing.*
import kotlin.test.Test
import kotlin.test.assertEquals

class ApiTest {
    @Test
    fun testCreateUser() = testApplication {
        application {
            module()
        }
        
        val response = client.post("/users") {
            contentType(ContentType.parse("application/toon"))
            setBody("""
                createUserRequest:
                  name: Alice
                  email: alice@example.com
            """.trimIndent())
        }
        
        assertEquals(HttpStatusCode.Created, response.status)
    }
}
```

## Performance Tips

1. **Reuse Toon Instance**: Create a single Toon instance and reuse it across requests
2. **Table Mode**: TOON automatically uses table mode for collections, reducing token usage by 30-60%
3. **Compression**: Enable GZIP compression for additional bandwidth savings
4. **Caching**: Cache serialized responses for frequently accessed data

## Troubleshooting

### Common Issues

**Issue**: "No suitable converter found"
- **Solution**: Ensure ContentNegotiation plugin is installed before routing
- **Solution**: Verify Content-Type header matches registered content type

**Issue**: "SerializationException: Expected X but found Y"
- **Solution**: Check that request body matches the expected data structure
- **Solution**: Verify all required fields are present in the request

**Issue**: "Charset encoding error"
- **Solution**: Ensure client sends UTF-8 encoded data
- **Solution**: Specify charset in Content-Type header: `application/toon; charset=utf-8`

## Additional Resources

- [README.md](README.md) - Installation and basic usage
- [API.md](API.md) - Complete API reference
- [DEMO.md](DEMO.md) - Demo server documentation
- [ktoon-core](../ktoon-core) - Core TOON format specification
