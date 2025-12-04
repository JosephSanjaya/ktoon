# Technology Stack

## Build System
- **Gradle** with Kotlin DSL (`.gradle.kts`)
- Custom build logic in `sjy-build-logic` composite build
- Version catalogs: `sjy` (from build-logic) and `libs` (project-level)

## Core Technologies
- **Kotlin**: 2.2.21
- **Compose Multiplatform**: 1.10.0-beta02
- **Kotlin Compose Compiler**: 1.9.3
- **JVM Target**: 21
- **Android Gradle Plugin**: 8.13.1

## Key Libraries

### Dependency Injection
- Koin 4.1.1 (with annotations 2.3.1)
- KSP for annotation processing

### Networking
- Ktor 3.3.3
  - **Client**: CIO, Darwin, OkHttp engines
    - ktoon-ktor module: TOON format ContentNegotiation integration for Ktor HttpClient
    - Automatic serialization/deserialization of request/response bodies
    - Custom Toon instance support for advanced configuration
  - **Server**: Netty engine
    - ktoon-ktor-server module: TOON format ContentNegotiation integration for Ktor Server
    - Automatic request deserialization and response serialization
    - Accept header-based content negotiation
    - Verified 67.4% token savings vs JSON
- Ktorfit 2.6.5
- OkHttp 5.3.2

### State Management & Architecture
- Orbit MVI 11.0.0
- AndroidX Lifecycle 2.9.4
- Kotlinx Coroutines 1.10.2

### Data & Persistence
- Store5 5.1.0-alpha07 (caching)
- Multiplatform Settings 1.3.0
- Room 2.8.4 (with SQLCipher 4.11.0)
- DataStore 1.2.0
- Kotlinx Serialization 1.9.0
  - TOON format implementation in ktoon-core module
  - Custom StringFormat using AbstractEncoder/AbstractDecoder
  - Single-pass O(N) serialization architecture
  - Comprehensive error handling with line numbers and context snippets
  - State machine for mode switching (Indentation vs Table)
  - ToonLexer for tokenization with indentation awareness
  - Specialized encoders/decoders for CSV-style collection rows
  - Full annotation support (@SerialName, @Transient, @Polymorphic)
  - Custom serializer delegation
  - Pure Kotlin Multiplatform (no platform-specific code)
  - Verified on JVM, Android, iOS, JS, and Wasm

### UI Components
- Material3 1.4.0
- Compose Navigation 2.9.1
- Coil 3.3.0 (image loading)
- Compose Destinations 2.3.0

### Utilities
- Kotlinx DateTime 0.7.1
- Kotlinx Collections Immutable 0.4.0
- Napier 2.7.1 (logging)

### Code Quality
- Detekt 1.23.8
- Jacoco 0.8.14

### Testing
- JUnit 4.13.2 / JUnit 5 6.0.1
- MockK 1.14.6
- Turbine 1.2.1
- Kotlin Test
- Property-Based Testing for serialization correctness
  - Round-trip identity properties
  - Format validation properties
  - Cross-platform consistency tests

## Common Commands

### Build & Run
```bash
# Build the project
gradle build

# Run Android app
gradle :composeApp:installDebug

# Run Desktop app
gradle :composeApp:run

# Run JS app
gradle :composeApp:jsBrowserDevelopmentRun

# Run WASM app
gradle :composeApp:wasmJsBrowserDevelopmentRun

# Run backend server
gradle :backend:run
```

### Code Quality
```bash
# Run Detekt
gradle detekt

# Run tests
gradle test

# Run tests for specific module
gradle :ktoon-core:test
gradle :ktoon-ktor:test
gradle :ktoon-ktor-server:test

# Generate coverage report
gradle jacocoTestReport
```

### Clean & Sync
```bash
# Clean build
gradle clean

# Sync dependencies
gradle --refresh-dependencies
```

### Backend Server Testing
```bash
# Start backend server
gradle :backend:run

# Test JSON endpoint
curl -H "Accept: application/json" http://localhost:8080/users

# Test TOON endpoint
curl -H "Accept: application/toon" http://localhost:8080/users

# Test root endpoint
curl http://localhost:8080/
```

## Platform-Specific Notes
- **Android**: Uses edge-to-edge display, Material3 theming
- **iOS**: Xcode project in `iosApp/` directory
- **Desktop**: Distributable formats: DMG (macOS), MSI (Windows), DEB (Linux)
- **Web**: Supports both JS and WASM targets with browser binaries
