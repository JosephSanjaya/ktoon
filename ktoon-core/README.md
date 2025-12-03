# Platform Support Documentation

## Overview

The ktoon-core module implements the TOON (Token-Oriented Object Notation) serialization format as a pure Kotlin Multiplatform library. The implementation is designed to work identically across all supported platforms without requiring platform-specific code.

## Supported Platforms

The following platforms are fully supported and tested:

### ✅ JVM (Desktop)
- **Target**: JVM 21
- **Status**: Fully supported
- **Compilation**: Verified with `compileKotlinJvm`
- **Notes**: Uses standard Kotlin stdlib and kotlinx.serialization

### ✅ Android
- **Min SDK**: 24
- **Target SDK**: 36
- **Status**: Fully supported
- **Compilation**: Verified with `compileDebugKotlinAndroid`
- **Notes**: No Android-specific dependencies required

### ✅ iOS (Kotlin/Native)
- **Targets**: iosArm64, iosX64, iosSimulatorArm64
- **Status**: Fully supported
- **Compilation**: Verified with `compileKotlinIosArm64`
- **Notes**: Uses Kotlin/Native stdlib

### ✅ JavaScript
- **Target**: Browser and Node.js
- **Status**: Fully supported
- **Compilation**: Verified with `compileKotlinJs`
- **Notes**: Uses Kotlin/JS stdlib

### ✅ WebAssembly
- **Target**: WasmJs
- **Status**: Fully supported
- **Compilation**: Verified with `compileKotlinWasmJs`
- **Notes**: Uses Kotlin/Wasm stdlib

## Implementation Architecture

### No Platform-Specific Code Required

The TOON serialization format implementation is entirely platform-agnostic because:

1. **Standard Kotlin APIs Only**: Uses only standard Kotlin language features and stdlib
2. **kotlinx.serialization**: Leverages the multiplatform kotlinx.serialization library
3. **No Platform Dependencies**: No file I/O, networking, or platform-specific APIs
4. **Pure String Processing**: All operations work on strings and data structures

### Source Set Structure

```
ktoon-core/src/
├── commonMain/          # All implementation code
│   └── kotlin/io/ktoon/
│       ├── Toon.kt              # Public API
│       ├── ToonEncoder.kt       # Serialization
│       ├── ToonDecoder.kt       # Deserialization
│       └── ToonLexer.kt         # Tokenization
├── commonTest/          # All test code
│   └── kotlin/io/ktoon/
│       ├── ToonTest.kt
│       ├── ToonEncoderTest.kt
│       ├── ToonLexerTest.kt
│       └── ToonErrorHandlingTest.kt
├── androidMain/         # Empty (no platform-specific code needed)
├── iosMain/             # Empty (no platform-specific code needed)
├── jvmMain/             # Empty (no platform-specific code needed)
└── webMain/             # Empty (no platform-specific code needed)
```

### Why No expect/actual Declarations?

The TOON format implementation does not require expect/actual declarations because:

- **String Operations**: All string operations (split, trim, substring) are available in Kotlin stdlib across all platforms
- **Collections**: Standard collections (List, Map, Set) work identically on all platforms
- **Serialization**: kotlinx.serialization provides consistent behavior across platforms
- **No I/O**: The library only converts between objects and strings; file/network I/O is the caller's responsibility

## Compilation Verification

All platforms have been verified to compile successfully:

```powershell
# JVM
gradle :ktoon-core:compileKotlinJvm

# Android
gradle :ktoon-core:compileDebugKotlinAndroid

# iOS
gradle :ktoon-core:compileKotlinIosArm64

# JavaScript
gradle :ktoon-core:compileKotlinJs

# WebAssembly
gradle :ktoon-core:compileKotlinWasmJs
```

## Cross-Platform Consistency

### Guaranteed Identical Behavior

The TOON format produces identical output across all platforms for the same input because:

1. **Deterministic Serialization**: The encoder follows a fixed algorithm
2. **Standard String Representation**: Primitive types (Int, Float, Boolean) use Kotlin's standard toString()
3. **No Platform-Specific Formatting**: No locale-dependent or platform-specific formatting
4. **Consistent Line Endings**: Uses `\n` for line breaks on all platforms

### Testing Strategy

The test suite in `commonTest/` runs on all platforms to verify:

- Round-trip serialization (encode then decode equals original)
- Format structure matches specification
- Error handling produces consistent exceptions
- Null handling works identically

## Dependencies

### Common Dependencies

All platforms use the same dependencies from `commonMain`:

```kotlin
commonMain.dependencies {
    implementation(sjy.kotlin.serialization)  // kotlinx.serialization-core
}
```

### No Platform-Specific Dependencies

The platform-specific source sets (androidMain, iosMain, jvmMain, webMain) have no additional dependencies beyond what's provided by the Kotlin stdlib for each platform.

## Future Considerations

### When Platform-Specific Code Might Be Needed

Platform-specific implementations would only be required if future features include:

1. **File I/O Helpers**: Reading/writing TOON files directly
2. **Streaming**: Large file processing with platform-specific streams
3. **Performance Optimizations**: Platform-specific string builders or buffers
4. **Native Interop**: Direct integration with platform-specific serialization systems

Currently, none of these features are planned, keeping the library pure and portable.

## Validation

### Requirement 4.1 Compliance

✅ **"WHEN the ktoon-core module is compiled for any KMP target THEN the compilation SHALL succeed without platform-specific dependencies"**

- All five platforms (JVM, Android, iOS, JS, Wasm) compile successfully
- No platform-specific dependencies in any source set
- All code resides in commonMain
- Tests run from commonTest on all platforms

### Requirement 4.2-4.5 Compliance

✅ **"WHEN TOON serialization runs on [platform] THEN the encoder and decoder SHALL produce identical results to other platforms"**

- Deterministic serialization algorithm
- Standard Kotlin string operations
- No platform-specific formatting
- Consistent test suite across all platforms

## Conclusion

The ktoon-core module successfully implements a pure Kotlin Multiplatform library with zero platform-specific code. All functionality is implemented in commonMain using only standard Kotlin and kotlinx.serialization APIs, ensuring consistent behavior across all supported platforms.
