# Product Overview

**KToon: The Token-Miser Serialization Library for AI-First Kotlin Apps**

## The Problem: The "JSON Tax"
Every time developers send structured data (search results, product catalogs, user history) to LLMs (OpenAI/Anthropic), they pay a "tax." Standard JSON is verbose—repeating field names like "id", "name", "timestamp" for every item in a list. This wastes context window space, increases API costs, and slows down latency.

## The Solution
KToon is a Kotlin Multiplatform serialization library implementing the **TOON (Token-Oriented Object Notation)** format. It's a drop-in replacement for JSON, optimized to slash token usage for AI interactions without changing domain logic.

## Key Value Propositions
- **30-60% Cost Reduction**: Tabular data compressed into header-row format (`users[2]{id,name}: 1,Alice`)
- **Zero-Friction Adoption**: Integrates with Ktor and Retrofit—no API rewrites needed
- **Cross-Platform by Default**: KMP support (Android, iOS, Desktop, Web)
- **Performance First (O(N))**: Compile-time metadata via kotlinx.serialization for single-pass, allocation-free serialization

## Target Use Cases
- **RAG Applications**: Fit ~40% more documents into the same context window
- **Data Analysis Agents**: Upload large CSV-like datasets to LLMs efficiently
- **Low-Bandwidth IoT**: Send complex structured configs over slow networks

## Target Platforms
- Android (minSdk 24, compileSdk 36)
- iOS (via Kotlin/Native)
- Desktop (JVM)
- Web (JavaScript and WebAssembly)

## Package Structure
- Base package: `io.ktoon`
- Core library: `ktoon-core` module (serialization engine)
- Ktor integration: `ktoon-ktor` module (Ktor HttpClient ContentNegotiation)
- Demo app: `composeApp` module (showcases KToon integration)

## Project Type
Multi-module Kotlin Multiplatform library with shared serialization logic and platform-specific implementations using the expect/actual pattern.

## TOON Format Specification

### Format Modes
1. **Indentation Mode**: For nested objects using key-value pairs with indentation
   ```
   user:
     id: 123
     name: Alice
   ```

2. **Table Mode**: For collections using CSV-like header-row format
   ```
   users[2]{id,name}:
     1,Alice
     2,Bob
   ```

### Implementation Status

**Completed Features:**
- ✅ Core StringFormat implementation (`Toon` object)
- ✅ Indentation mode encoder/decoder for nested objects
- ✅ Table mode encoder/decoder for collections
- ✅ ToonLexer with tokenization and indentation tracking
- ✅ Null value handling in both modes
- ✅ Comprehensive error handling:
  - Line numbers and context snippets in all error messages
  - Type mismatch detection with clear descriptions
  - Indentation error validation
  - Table size mismatch detection
  - Row field count validation
  - Unknown field detection with suggestions
- ✅ State machine for mode switching
- ✅ Single-pass O(N) serialization
- ✅ kotlinx.serialization annotation support:
  - @SerialName for custom field names
  - @Transient for excluded fields
  - @Polymorphic for type discrimination
- ✅ Custom serializer delegation
- ✅ Cross-platform compilation verified:
  - JVM (Desktop)
  - Android (minSdk 24, targetSdk 36)
  - iOS (Kotlin/Native - iosArm64, iosX64, iosSimulatorArm64)
  - JavaScript (Browser and Node.js)
  - WebAssembly (WasmJs)
- ✅ Platform-agnostic implementation (no expect/actual needed)
- ✅ Comprehensive test suite:
  - Unit tests for core functionality
  - Round-trip serialization tests
  - Error handling validation tests
  - Annotation support tests
  - Polymorphic serialization tests
  - Custom serializer tests

**In Progress:**
- ⏳ Property-based testing for correctness properties

**ktoon-core Documentation:**
- ⏳ README with installation and usage guide
- ⏳ API documentation
- ⏳ Format specification document

### Implementation Architecture
- **Public API**: `Toon` object implementing `StringFormat`
- **Encoder Components**:
  - `ToonEncoder` - Main encoder with state machine (Indentation/Table/Value states)
  - `ToonCollectionEncoder` - Manages table headers and collection serialization
  - `ToonRowEncoder` - Handles CSV-style row encoding
- **Decoder Components**:
  - `ToonDecoder` - Main decoder with error handling and context tracking
  - `ToonCollectionDecoder` - Manages collection size and row iteration
  - `ToonRowDecoder` - Handles CSV-style row decoding with field validation
- **Lexer**: `ToonLexer` - Tokenization with indentation awareness
- **Error Handling**: Context snippets showing 2 lines before/after errors with line markers
- **Null Handling**: Empty CSV values for null in table mode, "null" keyword in indentation mode
- **Platform Support**: Pure Kotlin implementation with zero platform-specific code
  - All code in `commonMain` source set
  - Uses only standard Kotlin stdlib and kotlinx.serialization APIs
  - No expect/actual declarations required
  - Deterministic output across all platforms
  - See `ktoon-core/PLATFORM_SUPPORT.md` for details

## Ktor Integration

### ktoon-ktor (Client-Side)

**Implementation Status:**

**Completed Features:**
- ✅ Ktor HttpClient ContentNegotiation integration
- ✅ ToonContentConverter implementing ContentConverter interface
- ✅ Automatic request body serialization to TOON format
- ✅ Automatic response body deserialization from TOON format
- ✅ Custom Toon instance support with custom serializers
- ✅ Content-Type registration ("application/toon" default)
- ✅ Charset handling (UTF-8 default, respects server charset)
- ✅ Null handling for request/response bodies
- ✅ Error preservation with detailed context from ToonDecoder
- ✅ Full kotlinx.serialization annotation support
- ✅ Comprehensive test suite covering all features
- ✅ Complete documentation:
  - README.md - Installation, usage, configuration
  - EXAMPLES.md - Comprehensive usage scenarios
  - API.md - Complete API reference

**Limitations:**
- Client-side only (Ktor HttpClient)
- In-memory processing (not suitable for very large payloads > 10MB)
- No streaming support

**Future Enhancements:**
- Streaming serialization/deserialization for large payloads
- Compression integration

### ktoon-ktor-server (Server-Side)

**Implementation Status:**

**Completed Features:**
- ✅ Ktor Server ContentNegotiation integration
- ✅ ToonContentConverter implementing ContentConverter interface for server
- ✅ Automatic request body deserialization from TOON format
- ✅ Automatic response body serialization to TOON format
- ✅ Custom Toon instance support with custom serializers
- ✅ Content-Type registration ("application/toon" default)
- ✅ Charset handling (UTF-8 default, respects client charset)
- ✅ Null handling for request/response bodies
- ✅ Error preservation with detailed context
- ✅ Full kotlinx.serialization annotation support
- ✅ Complete documentation:
  - README.md - Installation, usage, configuration
  - EXAMPLES.md - Comprehensive usage scenarios
  - API.md - Complete API reference
  - DEMO.md - Demo server application guide

**Verified Features:**
- ✅ Accept header-based content negotiation (application/json vs application/toon)
- ✅ Table mode for collection responses
- ✅ Token savings: 67.4% reduction compared to JSON (verified with 5-user dataset)
- ✅ Proper Content-Type headers in responses

**Known Issues:**
- ktoon-core has unnecessary Compose dependencies causing transitive dependency issues
- Workaround: Manual TOON formatting or dependency exclusions in consuming projects

**Future Enhancements:**
- Streaming serialization/deserialization for large payloads
- Compression integration
- Clean up ktoon-core dependencies

### Usage Example

```kotlin
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktoon.ktor.toon

val client = HttpClient(CIO) {
    install(ContentNegotiation) {
        toon()  // Register TOON format
    }
}

@Serializable
data class User(val id: Int, val name: String)

// Automatic serialization/deserialization
val user = client.post("https://api.example.com/users") {
    contentType(ContentType.parse("application/toon"))
    setBody(User(1, "Alice"))
}.body<User>()
```

### Integration Architecture

**Client-Side (ktoon-ktor):**
```
Ktor HttpClient
    ↓
ContentNegotiation Plugin
    ↓
ToonContentConverter (implements ContentConverter)
    ↓
ktoon-core (Toon.encodeToString / Toon.decodeFromString)
```

**Server-Side (ktoon-ktor-server):**
```
Ktor Server
    ↓
ContentNegotiation Plugin
    ↓
ToonContentConverter (implements ContentConverter)
    ↓
ktoon-core (Toon.encodeToString / Toon.decodeFromString)
```

### Platform Support
- Pure Kotlin implementation
- Works on all Kotlin Multiplatform targets
- No platform-specific code required
- Same behavior across JVM, Android, iOS, JS, and Wasm

### Demo Application (backend module)

A standalone JVM backend server demonstrating TOON format serialization:

**Features:**
- Ktor Server with Netty engine on port 8080
- GET /users endpoint with sample user data
- Accept header-based format selection (application/json or application/toon)
- Demonstrates 67.4% token savings with TOON format

**Verified Results:**
- JSON response: 553 bytes (~138 tokens)
- TOON response: 179 bytes (~45 tokens)
- Token savings: 67.4% reduction

**Usage:**
```bash
# Start server
gradle :backend:run

# Test JSON format
curl -H "Accept: application/json" http://localhost:8080/users

# Test TOON format
curl -H "Accept: application/toon" http://localhost:8080/users
```

See `backend/TEST_RESULTS.md` for comprehensive test validation.
