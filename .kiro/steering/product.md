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
- ⏳ Documentation and usage examples
- ⏳ README with installation and usage guide

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
