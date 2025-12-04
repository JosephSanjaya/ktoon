# TOON Format Demo Server

This demo server showcases the TOON format integration with Ktor Server through the ContentNegotiation plugin.

## Running the Demo Server

### Using IntelliJ IDEA (Recommended)

1. Open the project in IntelliJ IDEA
2. Navigate to `ktoon-ktor-server/src/jvmMain/kotlin/io/ktoon/ktor/server/demo/DemoServer.kt`
3. Click the green play button next to the `main()` function
4. The server will start on `http://localhost:8080`

### Using Gradle

```bash
gradle :ktoon-ktor-server:jvmRun
```

The server will start on `http://localhost:8080`

### Using Command Line (after building)

```bash
# Build the project first
gradle :ktoon-ktor-server:jvmJar

# Run the JAR
java -cp ktoon-ktor-server/build/libs/ktoon-ktor-server-jvm.jar io.ktoon.ktor.server.demo.DemoServerKt
```

## API Endpoints

### Root Endpoint

```
GET /
```

Returns information about available endpoints and supported content types.

### User Management API

#### Create User

```
POST /users
Content-Type: application/toon

name: Alice
email: alice@example.com
```

Response (201 Created):
```
id: 1
name: Alice
email: alice@example.com
createdAt: 2024-12-04T10:30:00Z
```

#### List All Users

```
GET /users
Accept: application/toon
```

Response (200 OK) - TOON Table Mode:
```
users[2]{id,name,email,createdAt}:
  1,Alice,alice@example.com,2024-12-04T10:30:00Z
  2,Bob,bob@example.com,2024-12-04T10:31:00Z
```

#### Get User by ID

```
GET /users/1
Accept: application/toon
```

Response (200 OK):
```
id: 1
name: Alice
email: alice@example.com
createdAt: 2024-12-04T10:30:00Z
```

#### Update User

```
PUT /users/1
Content-Type: application/toon

name: Alice Smith
email: alice.smith@example.com
```

Response (200 OK):
```
id: 1
name: Alice Smith
email: alice.smith@example.com
createdAt: 2024-12-04T10:30:00Z
```

#### Delete User

```
DELETE /users/1
```

Response (204 No Content)

#### Trigger Error (for testing)

```
POST /users/invalid
```

Response (500 Internal Server Error):
```
Server error: This endpoint intentionally triggers an error
```

## Content Negotiation

The demo server supports both TOON and JSON formats. Use the `Content-Type` header for requests and the `Accept` header for responses.

### TOON Format

```bash
curl -X POST http://localhost:8080/users \
  -H "Content-Type: application/toon" \
  -H "Accept: application/toon" \
  -d "name: Alice
email: alice@example.com"
```

### JSON Format

```bash
curl -X POST http://localhost:8080/users \
  -H "Content-Type: application/json" \
  -H "Accept: application/json" \
  -d '{"name":"Alice","email":"alice@example.com"}'
```

## Features Demonstrated

### 1. Automatic Serialization/Deserialization

The server automatically converts between Kotlin objects and TOON format based on the `Content-Type` and `Accept` headers.

### 2. Table Mode for Collections

When returning lists of users, the TOON format uses table mode for efficient serialization:

```
users[2]{id,name,email,createdAt}:
  1,Alice,alice@example.com,2024-12-04T10:30:00Z
  2,Bob,bob@example.com,2024-12-04T10:31:00Z
```

This is 30-60% more token-efficient than JSON for AI interactions.

### 3. Error Handling

The server demonstrates proper error handling with the StatusPages plugin:

- **SerializationException**: Returns 400 Bad Request with error details
- **IllegalStateException**: Returns 500 Internal Server Error with error message

### 4. Request/Response Logging

The CallLogging plugin logs all requests and responses:

```
INFO  - POST /users - 201 Created
INFO  - GET /users - 200 OK
INFO  - GET /users/1 - 200 OK
```

### 5. Compression

The Compression plugin automatically compresses responses when the client supports it.

### 6. Multiple Content Types

The server supports both TOON and JSON formats, demonstrating content negotiation:

- Use `Accept: application/toon` for TOON responses
- Use `Accept: application/json` for JSON responses
- Use `Content-Type: application/toon` for TOON requests
- Use `Content-Type: application/json` for JSON requests

## Testing with cURL

### Create a user with TOON format

```bash
curl -X POST http://localhost:8080/users \
  -H "Content-Type: application/toon" \
  -H "Accept: application/toon" \
  -d "name: Alice
email: alice@example.com"
```

### List all users with TOON format

```bash
curl -X GET http://localhost:8080/users \
  -H "Accept: application/toon"
```

### Get a specific user with JSON format

```bash
curl -X GET http://localhost:8080/users/1 \
  -H "Accept: application/json"
```

### Update a user with TOON format

```bash
curl -X PUT http://localhost:8080/users/1 \
  -H "Content-Type: application/toon" \
  -H "Accept: application/toon" \
  -d "name: Alice Smith
email: alice.smith@example.com"
```

### Delete a user

```bash
curl -X DELETE http://localhost:8080/users/1
```

### Trigger an error

```bash
curl -X POST http://localhost:8080/users/invalid
```

## Implementation Details

### Data Models

The demo uses simple data models defined in `DemoModels.kt`:

- `User`: Represents a user with id, name, email, and createdAt
- `CreateUserRequest`: Request body for creating a user
- `UpdateUserRequest`: Request body for updating a user
- `ErrorResponse`: Error response format

### Routes

All user management routes are defined in `DemoRoutes.kt` using Ktor's routing DSL.

### Server Configuration

The server is configured in `DemoServer.kt` with:

- **Netty Engine**: Embedded server running on port 8080
- **CallLogging**: Logs all requests and responses
- **StatusPages**: Handles exceptions and converts them to HTTP responses
- **Compression**: Compresses responses with gzip
- **ContentNegotiation**: Supports both TOON and JSON formats

## Next Steps

1. Try creating users with different content types
2. Compare the TOON and JSON response sizes
3. Test error handling by sending invalid TOON format
4. Experiment with the compression feature
5. Integrate the TOON format into your own Ktor Server application

## Requirements Validated

This demo validates the following requirements from the specification:

- **Requirement 8.1**: Demo server exposes REST endpoints accepting and returning TOON format
- **Requirement 8.2**: Demo deserializes TOON requests and processes the data
- **Requirement 8.3**: Demo returns TOON-formatted responses
- **Requirement 8.4**: Demo demonstrates proper error handling
- **Requirement 8.5**: Demo logs serialization operations for debugging
