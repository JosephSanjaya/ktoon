---
inclusion: always
---

# General Development Rules

## Platform Requirements

- Windows operating system - ensure all terminal commands support powershell only
- Use `gradle` from environment instead of `gradlew` for all Gradle tasks

## Code Style

- Write verbose, self-documenting code without comments
- Code should be expressive enough that comments are unnecessary
- Prioritize readability through descriptive variable and method names

## Development Approach

- Optimize for efficiency and effectiveness in all actions
- Research the most efficient approach before implementation
- Avoid redundant or unnecessary operations
- If using any sdk always check context7 mcp first for latest documentation
- for complex use sequentialthinking mcp
- if needed to store memory use memory MCP

## Serialization Implementation Guidelines

### TOON Format Architecture
- Use kotlinx.serialization's AbstractEncoder/AbstractDecoder
- Implement state machine for mode switching (Indentation vs Table vs Value)
- Maintain single-pass O(N) traversal without buffering
- Use compile-time metadata via @Serializable annotation
- Separate concerns: main encoder/decoder, collection handlers, row handlers, lexer

### Current Implementation Structure
- **ToonEncoder**: Main encoder with EncodingState enum (IDLE, ENCODING_STRUCTURE, ENCODING_COLLECTION, ENCODING_VALUE)
- **ToonCollectionEncoder**: Handles table headers and delegates to ToonRowEncoder
- **ToonRowEncoder**: Accumulates CSV values and writes complete rows
- **ToonDecoder**: Main decoder with error context tracking and helper methods
- **ToonCollectionDecoder**: Manages collection size validation and row iteration
- **ToonRowDecoder**: Parses CSV rows with field count validation
- **ToonLexer**: Tokenizes input with indentation awareness and table header detection

### Testing Strategy for Serialization
- Write property-based tests for universal correctness properties
- Test round-trip identity: encode then decode should equal original
- Validate format structure matches specification exactly
- Test cross-platform consistency (same output on all targets)
- Use minimal unit tests for specific edge cases
- Property tests should run minimum 100 iterations
- Test files: ToonTest.kt, ToonEncoderTest.kt, ToonLexerTest.kt, ToonErrorHandlingTest.kt

### Error Handling (Implemented)
- Throw SerializationException with line numbers for decoder errors
- Include context snippets showing 2 lines before/after with >>> marker
- Provide clear type information for encoder errors
- Never expose sensitive data in error messages
- Validate state transitions in encoder (IllegalStateException for invalid sequences)
- Detect unsupported types (Map, unknown structure kinds)
- Validate indentation consistency
- Check table size matches actual row count
- Verify CSV field count matches header declaration
- Provide available field names when unknown field encountered

### Null Handling (Implemented)
- Table mode: empty CSV values represent null
- Indentation mode: "null" keyword for null values
- Both modes: decodeNotNullMark() checks for null before decoding
- Row decoder: getCurrentValue() throws clear error for empty non-nullable fields

### Platform-Specific Implementation (Completed)
- **No platform-specific code required** for TOON serialization
- All implementation resides in `commonMain` source set
- Platform-specific source sets (androidMain, iosMain, jvmMain, webMain) are empty
- Uses only standard Kotlin stdlib and kotlinx.serialization APIs
- No expect/actual declarations needed
- Compilation verified on all targets:
  - JVM: `gradle :ktoon-core:compileKotlinJvm`
  - Android: `gradle :ktoon-core:compileDebugKotlinAndroid`
  - iOS: `gradle :ktoon-core:compileKotlinIosArm64`
  - JavaScript: `gradle :ktoon-core:compileKotlinJs`
  - WebAssembly: `gradle :ktoon-core:compileKotlinWasmJs`
- Deterministic serialization produces identical output across all platforms
- See `ktoon-core/PLATFORM_SUPPORT.md` for comprehensive documentation
