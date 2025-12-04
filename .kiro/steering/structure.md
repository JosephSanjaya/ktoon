# Project Structure

## Module Organization

### Root Project
- `settings.gradle.kts` - Project configuration and module includes
- `build.gradle.kts` - Root build configuration with plugin declarations
- `gradle/libs.versions.toml` - Project-level version catalog
- `sjy-build-logic/` - Custom Gradle convention plugins and shared build logic

### Modules

#### `composeApp`
Main application module containing platform-specific entry points and shared UI.

**Source Sets:**
- `commonMain/` - Shared Compose UI code
  - `kotlin/io/ktoon/` - Main application code
  - `composeResources/` - Shared resources (drawables, strings, etc.)
- `androidMain/` - Android-specific code
  - `AndroidManifest.xml`
  - `MainActivity.kt` - Android entry point
  - `res/` - Android resources
- `iosMain/` - iOS-specific code
  - `MainViewController.kt` - iOS entry point
- `jvmMain/` - Desktop-specific code
  - `main.kt` - Desktop entry point
- `jsMain/` - JavaScript web-specific code
- `wasmJsMain/` - WebAssembly-specific code
- `webMain/` - Shared web code (JS + WASM)
  - `resources/` - Web resources (index.html, styles.css)

#### `ktoon-core`
Shared library module for TOON serialization format implementation.

**Implementation Status:**
- ✅ Core encoder/decoder with state machine
- ✅ Lexer with tokenization and indentation tracking
- ✅ Collection encoding with table mode (CSV-style)
- ✅ Null handling in both indentation and table modes
- ✅ Comprehensive error handling with line numbers and context
- ✅ kotlinx.serialization annotation support (@SerialName, @Transient, @Polymorphic)
- ✅ Platform-specific implementations (NONE REQUIRED - pure Kotlin implementation)
- ✅ Cross-platform compilation verified (JVM, Android, iOS, JS, Wasm)
- ⏳ Property-based testing for correctness
- ⏳ Documentation and examples

**Source Sets:**
- `commonMain/` - Platform-agnostic serialization logic (ALL implementation code)
  - `kotlin/io/ktoon/` - TOON format implementation
    - `Toon.kt` - Main StringFormat object (public API)
    - `ToonEncoder.kt` - Serialization encoder with state machine
    - `ToonDecoder.kt` - Deserialization decoder with error handling
    - `ToonLexer.kt` - Tokenization with indentation awareness
    - Internal classes:
      - `ToonCollectionEncoder` - Table header and collection management
      - `ToonRowEncoder` - CSV-style row encoding
      - `ToonCollectionDecoder` - Collection size and row iteration
      - `ToonRowDecoder` - CSV-style row decoding
- `commonTest/` - Cross-platform tests
  - `ToonTest.kt` - Basic round-trip tests
  - `ToonEncoderTest.kt` - Encoder-specific tests
  - `ToonLexerTest.kt` - Lexer and tokenization tests
  - `ToonErrorHandlingTest.kt` - Error handling validation
  - `ToonAnnotationTest.kt` - kotlinx.serialization annotation tests
  - `ToonPolymorphicTest.kt` - Polymorphic serialization tests
  - `ToonCustomSerializerTest.kt` - Custom serializer tests
  - Unit tests for format validation
  - Property-based tests for correctness (planned)
  - Round-trip serialization tests
- `androidMain/` - Empty (no platform-specific code needed)
- `iosMain/` - Empty (no platform-specific code needed)
- `jvmMain/` - Empty (no platform-specific code needed)
- `webMain/` - Empty (no platform-specific code needed)

**Platform Support:**
- See `ktoon-core/PLATFORM_SUPPORT.md` for detailed platform compatibility documentation
- All platforms compile successfully without platform-specific dependencies
- Pure Kotlin implementation using only stdlib and kotlinx.serialization
- Deterministic serialization produces identical output across all platforms

#### `ktoon-ktor`
Ktor ContentNegotiation integration library for TOON format (client-side only).

**Implementation Status:**
- ✅ ToonContentConverter implementing Ktor's ContentConverter interface
- ✅ Client-side ContentNegotiation extension function
- ✅ Request body serialization to TOON format
- ✅ Response body deserialization from TOON format
- ✅ Custom Toon instance support
- ✅ Charset handling (UTF-8 default)
- ✅ Null handling for requests and responses
- ✅ Error preservation with detailed context
- ✅ Comprehensive test suite:
  - Annotation support tests (@SerialName, @Transient)
  - Custom configuration tests
  - Error handling tests
  - Null handling tests
  - Polymorphic type tests
  - SerialName tests
- ✅ Complete documentation:
  - README with installation and usage
  - EXAMPLES with comprehensive scenarios
  - API documentation with full reference

**Limitations:**
- Client-side only (Ktor HttpClient)
- Does not support Ktor Server (would require separate extension function)

**Source Sets:**
- `commonMain/` - Platform-agnostic integration code
  - `kotlin/io/ktoon/ktor/` - Ktor integration
    - `ToonContentConverter.kt` - ContentConverter implementation (internal)
    - `ContentNegotiationExt.kt` - Extension function for registration (public API)
- `commonTest/` - Cross-platform tests
  - `ToonContentConverterAnnotationTest.kt` - Annotation support tests
  - `ToonContentConverterCustomConfigTest.kt` - Custom configuration tests
  - `ToonContentConverterErrorHandlingTest.kt` - Error handling tests
  - `ToonContentConverterNullHandlingTest.kt` - Null handling tests
  - `ToonContentConverterPolymorphicTest.kt` - Polymorphic type tests
  - `ToonContentConverterSerialNameTest.kt` - SerialName tests
- `androidMain/` - Empty (no platform-specific code needed)
- `iosMain/` - Empty (no platform-specific code needed)
- `jvmMain/` - Empty (no platform-specific code needed)
- `webMain/` - Empty (no platform-specific code needed)

**Platform Support:**
- Pure Kotlin implementation using only Ktor and ktoon-core APIs
- Works on all Kotlin Multiplatform targets
- No platform-specific code required

#### `ktoon-ktor-server`
Ktor Server ContentNegotiation integration library for TOON format (server-side).

**Implementation Status:**
- ✅ ToonContentConverter implementing Ktor's ContentConverter interface for server
- ✅ Server-side ContentNegotiation extension function
- ✅ Request body deserialization from TOON format
- ✅ Response body serialization to TOON format
- ✅ Custom Toon instance support
- ✅ Charset handling (UTF-8 default)
- ✅ Null handling for requests and responses
- ✅ Error preservation with detailed context
- ✅ Complete documentation:
  - README with installation and usage
  - EXAMPLES with comprehensive scenarios
  - API documentation with full reference
  - DEMO with example server application

**Source Sets:**
- `commonMain/` - Platform-agnostic integration code
  - `kotlin/io/ktoon/ktor/server/` - Ktor Server integration
    - `ToonContentConverter.kt` - ContentConverter implementation (internal)
    - `ContentNegotiationExt.kt` - Extension function for registration (public API)
- `commonTest/` - Cross-platform tests
- `androidMain/` - Empty (no platform-specific code needed)
- `iosMain/` - Empty (no platform-specific code needed)
- `jvmMain/` - Empty (no platform-specific code needed)
- `webMain/` - Empty (no platform-specific code needed)

**Platform Support:**
- Pure Kotlin implementation using only Ktor Server and ktoon-core APIs
- Works on all Kotlin Multiplatform targets that support Ktor Server
- No platform-specific code required

#### `backend`
Standalone JVM backend server module demonstrating TOON format serialization with Ktor Server.

**Purpose:**
- Demonstrates ktoon-ktor-server integration
- Provides working example of TOON format in server responses
- Shows token savings comparison between JSON and TOON formats

**Implementation:**
- ✅ Ktor Server with Netty engine
- ✅ ContentNegotiation with JSON and TOON support
- ✅ GET /users endpoint returning sample data
- ✅ Accept header-based format selection
- ✅ Manual TOON table mode formatting (temporary workaround)
- ✅ Comprehensive test results documentation

**Source Structure:**
- `src/main/kotlin/io/ktoon/backend/`
  - `Application.kt` - Main server entry point with routing
  - `models/User.kt` - Sample data model
- `src/main/resources/`
  - `logback.xml` - Logging configuration
- `README.md` - Usage instructions and examples
- `TEST_RESULTS.md` - Comprehensive test validation results

**Build Configuration:**
- JVM-only module (not multiplatform)
- Uses Ktor Server 3.3.3
- Application plugin for executable JAR
- Main class: `io.ktoon.backend.ApplicationKt`

**Known Issues:**
- ktoon-core has unnecessary Compose dependencies that cause build issues
- Temporary workaround: Manual TOON formatting instead of ktoon-ktor-server
- Future: Clean up ktoon-core dependencies or create separate serialization-only module

#### `iosApp`
Native iOS application wrapper (Xcode project).
- `iosApp.xcodeproj/` - Xcode project configuration
- `iosApp/` - Swift UI wrapper and iOS assets
- `Configuration/` - Build configuration files

## Code Organization Patterns

### Package Structure

### composeApp Module
```
io.ktoon/
├── App.kt              # Main Compose application
├── Platform.kt         # Platform interface (expect)
├── Greeting.kt         # Example shared logic
└── [platform]/
    └── Platform.[platform].kt  # Platform implementations (actual)
```

### ktoon-core Module
```
io.ktoon/
├── Toon.kt                    # Main StringFormat object (public API)
├── ToonEncoder.kt             # Serialization encoder with state machine (internal)
│   ├── ToonEncoder            # Main encoder for indentation mode
│   ├── ToonCollectionEncoder  # Table header and collection management
│   └── ToonRowEncoder         # CSV-style row encoding
├── ToonDecoder.kt             # Deserialization decoder with error handling (internal)
│   ├── ToonDecoder            # Main decoder for indentation mode
│   ├── ToonCollectionDecoder  # Collection size and row iteration
│   └── ToonRowDecoder         # CSV-style row decoding
└── ToonLexer.kt               # Tokenization with indentation awareness (internal)
    ├── Token                  # Sealed class for token types
    └── TableHeaderInfo        # Data class for table header parsing
```

### ktoon-ktor Module
```
io.ktoon.ktor/
├── ToonContentConverter.kt    # ContentConverter implementation (internal)
└── ContentNegotiationExt.kt   # Extension function for registration (public API)
```

### ktoon-ktor-server Module
```
io.ktoon.ktor.server/
├── ToonContentConverter.kt    # Server ContentConverter implementation (internal)
└── ContentNegotiationExt.kt   # Server extension function for registration (public API)
```

### backend Module
```
io.ktoon.backend/
├── Application.kt             # Main server entry point with routing
└── models/
    └── User.kt                # Sample data model
```

### Platform-Specific Code
Use Kotlin's `expect`/`actual` mechanism for platform-specific implementations:
- Define `expect` declarations in `commonMain`
- Provide `actual` implementations in platform source sets

### Resource Management
- Compose resources go in `composeResources/` (accessible via generated `Res` object)
- Android resources in `androidMain/res/`
- Web resources in `webMain/resources/`

## Build Configuration Conventions

### Plugin Application
Use custom build-logic plugins:
- `buildlogic.multiplatform.app` - For application modules
- `buildlogic.multiplatform.lib` - For library modules
- `buildlogic.multiplatform.cmp` - For Compose Multiplatform setup

### Dependency Declaration
- Use version catalog references: `sjy.plugins.*`, `sjy.*`, `libs.*`
- Platform-specific dependencies in respective source set blocks
- Common dependencies in `commonMain.dependencies`

### Source Set Dependencies
```kotlin
sourceSets {
    commonMain.dependencies {
        // Shared dependencies
    }
    androidMain.dependencies {
        // Android-specific
    }
    iosMain.dependencies {
        // iOS-specific
    }
    jvmMain.dependencies {
        // Desktop-specific
    }
    webMain.dependencies {
        // Web-specific (JS + WASM)
    }
}
```

## Naming Conventions
- Modules: kebab-case (`ktoon-core`)
- Packages: lowercase (`io.ktoon`)
- Classes: PascalCase (`MainActivity`)
- Functions: camelCase (`getPlatform()`)
- Composables: PascalCase (`App()`)
- Platform files: `FileName.[platform].kt` (e.g., `Platform.android.kt`)
