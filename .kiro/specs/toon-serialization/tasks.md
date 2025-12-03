# Implementation Plan

- [x] 1. Set up ktoon-core module structure






  - Create source set directories for commonMain, androidMain, iosMain, jvmMain, webMain
  - Configure build.gradle.kts with kotlinx.serialization plugin
  - Add kotlinx.serialization dependencies to commonMain
  - _Requirements: 4.1_

- [x] 2. Implement core Toon StringFormat





- [x] 2.1 Create Toon object implementing StringFormat




  - Define Toon as object extending StringFormat
  - Implement encodeToString method delegating to ToonEncoder
  - Implement decodeFromString method delegating to ToonDecoder
  - Add serializersModule property with EmptySerializersModule
  - _Requirements: 1.1, 2.1_

- [x] 2.2 Write unit tests for Toon StringFormat



  - Test encodeToString with simple data class
  - Test decodeFromString with valid TOON string
  - Test round-trip with basic object
  - _Requirements: 1.1, 2.1_

- [ ] 3. Implement ToonEncoder for serialization




- [x] 3.1 Create ToonEncoder extending AbstractEncoder



  - Implement encodeValue for primitive types
  - Add StringBuilder for output accumulation
  - Implement indentation tracking with level counter
  - Override serializersModule property
  - _Requirements: 1.1, 1.2_

- [x] 3.2 Implement structure encoding in ToonEncoder

  - Override beginStructure to detect structure types
  - Implement encodeElement to write field names with indentation
  - Override endStructure to handle structure completion
  - Add logic to increase/decrease indentation levels
  - _Requirements: 1.2, 1.3_

- [x] 3.3 Implement collection encoding with table mode

  - Override beginCollection to detect collections of objects
  - Write table header in format `name[N]{fields}:`
  - Extract field names from element descriptor
  - Switch to ToonRowEncoder for collection elements
  - _Requirements: 1.4, 1.5_

- [x] 3.4 Create ToonRowEncoder for CSV rows

  - Extend AbstractEncoder for row-level encoding
  - Accumulate values in a list during encoding
  - Override endStructure to write comma-separated row
  - Implement encodeNull to write empty value
  - _Requirements: 1.4, 1.5, 6.3_

- [x] 3.5 Add null handling to ToonEncoder

  - Override encodeNull for indentation mode
  - Handle null nested objects gracefully
  - Ensure null doesn't break format structure
  - _Requirements: 6.1, 6.2_

- [ ]* 3.6 Write unit tests for ToonEncoder
  - Test primitive field encoding with correct indentation
  - Test nested object encoding with increased indentation
  - Test collection encoding produces table format
  - Test table header format matches specification
  - Test null handling in both modes
  - _Requirements: 1.2, 1.3, 1.4, 1.5, 6.1, 6.2, 6.3_

- [x] 4. Implement ToonLexer for tokenization





- [x] 4.1 Create ToonLexer class


  - Split input into lines array
  - Implement currentIndentation to count leading spaces
  - Implement peekToken without consuming
  - Implement consumeToken to advance position
  - Add isTableHeader to detect pattern `name[N]{fields}:`
  - _Requirements: 2.2, 2.3_

- [x] 4.2 Implement token parsing methods



  - Add parseIdentifier for field names
  - Add parseNumber for numeric values
  - Add parseString for string literals
  - Add parseTableHeader to extract name, size, and fields
  - _Requirements: 2.2, 2.3, 2.4_

- [x] 4.3 Write unit tests for ToonLexer





  - Test indentation counting
  - Test token peeking and consuming
  - Test table header detection and parsing
  - Test various token types
  - _Requirements: 2.2, 2.3, 2.4_

- [x] 5. Implement ToonDecoder for deserialization



- [x] 5.1 Create ToonDecoder extending AbstractDecoder


  - Initialize with ToonLexer
  - Implement decodeValue for primitive types
  - Track current indentation level
  - Override serializersModule property
  - _Requirements: 2.1, 2.2_

- [x] 5.2 Implement structure decoding in ToonDecoder


  - Override beginStructure to detect table headers
  - Implement decodeElementIndex based on indentation
  - Handle nested object parsing with indentation tracking
  - Switch to ToonRowDecoder when table header detected
  - _Requirements: 2.2, 2.3_

- [x] 5.3 Implement collection decoding


  - Override decodeCollectionSize to extract size from header
  - Parse exactly N rows after table header
  - Create ToonRowDecoder instances for each row
  - _Requirements: 2.3, 2.4_

- [x] 5.4 Create ToonRowDecoder for CSV parsing


  - Split row by commas
  - Implement decodeValue to return next value
  - Implement decodeElementIndex to track position
  - Override decodeNotNullMark to check for empty values
  - _Requirements: 2.4, 2.5_

- [x] 5.5 Add null parsing to ToonDecoder


  - Handle empty CSV values as null
  - Parse null representations in indentation mode
  - Correctly assign null to nullable fields
  - _Requirements: 2.5, 6.4, 6.5_

- [ ]* 5.6 Write unit tests for ToonDecoder
  - Test indented key-value parsing
  - Test table mode row count matches header
  - Test CSV field mapping to object fields
  - Test empty value interpreted as null
  - Test null parsing in various contexts
  - _Requirements: 2.2, 2.3, 2.4, 2.5, 6.4, 6.5_

- [x] 6. Implement error handling



- [x] 6.1 Add encoder error handling


  - Throw SerializationException for unsupported types with type info
  - Detect and throw for circular references
  - Throw IllegalStateException for invalid method sequences
  - _Requirements: 5.4_

- [x] 6.2 Add decoder error handling


  - Throw SerializationException with line number for syntax errors
  - Add context snippets to error messages
  - Throw for type mismatches with clear descriptions
  - Throw for missing required fields
  - Throw for indentation errors with context
  - Throw for table size mismatches showing expected vs actual
  - _Requirements: 5.1, 5.3_

- [ ] 6.3 Write unit tests for error handling

  - Test invalid syntax throws with line number
  - Test mismatched collection size throws appropriate error
  - Test unexpected indentation throws with context
  - Test unsupported type throws with type information
  - _Requirements: 5.1, 5.3, 5.4_

- [ ] 7. Add kotlinx.serialization annotation support






- [x] 7.1 Implement @SerialName support


  - Use descriptor.getElementName to get custom names
  - Write custom names in TOON output
  - Parse custom names during decoding
  - _Requirements: 7.2_

- [x] 7.2 Implement @Transient support


  - Check descriptor for transient fields
  - Skip transient fields during encoding
  - Handle missing transient fields during decoding
  - _Requirements: 7.3_

- [x] 7.3 Implement @Polymorphic support


  - Handle type discriminator in TOON format
  - Encode type information for polymorphic classes
  - Decode and reconstruct correct subtype
  - _Requirements: 7.4_



- [x] 7.4 Implement custom serializer delegation




  - Check for custom serializers in serializersModule
  - Delegate to custom serializers when present
  - Ensure custom serializers work in both modes
  - _Requirements: 7.5_

- [ ]* 7.5 Write unit tests for annotation support
  - Test @SerialName produces custom names in output
  - Test @Transient fields excluded from output
  - Test @Polymorphic serialization with type discrimination
  - Test custom serializers are invoked correctly
  - _Requirements: 7.2, 7.3, 7.4, 7.5_

- [x] 8. Implement platform-specific code



- [x] 8.1 Add platform-specific implementations


  - Create expect/actual declarations if needed
  - Ensure no platform-specific dependencies in commonMain
  - Test compilation on all targets (Android, iOS, JVM, JS, Wasm)
  - _Requirements: 4.1_

- [ ]* 8.2 Write cross-platform tests
  - Run same test suite on all platforms
  - Verify outputs are identical across platforms
  - Test with platform-specific types if any
  - _Requirements: 4.2, 4.3, 4.4, 4.5_

- [ ]* 9. Create comprehensive test suite
- [ ]* 9.1 Write round-trip tests
  - Test simple data classes
  - Test nested objects
  - Test collections of various sizes
  - Test mixed structures (objects + collections)
  - Test with null values in various contexts
  - _Requirements: 2.1, 6.1, 6.2, 6.3, 6.4, 6.5_

- [ ]* 9.2 Write format validation tests
  - Verify indentation is correct for nested structures
  - Verify table headers match exact pattern
  - Test edge cases (empty collections, single elements)
  - Test deeply nested structures
  - _Requirements: 1.2, 1.3, 1.4, 1.5_

- [ ]* 9.3 Write integration tests
  - Test with real-world data structures
  - Test with all kotlinx.serialization annotations
  - Test with custom serializers
  - Test error scenarios
  - _Requirements: 7.1, 7.2, 7.3, 7.4, 7.5_

- [ ] 10. Checkpoint - Ensure all tests pass
  - Ensure all tests pass, ask the user if questions arise.

- [ ]* 11. Create documentation
- [ ]* 11.1 Write comprehensive README
  - Add project overview and motivation
  - Include installation instructions
  - Add basic usage examples
  - Show indentation mode examples
  - Show table mode examples
  - Document error handling
  - Add comparison with JSON (token savings)
  - Include API reference
  - _Requirements: All_

- [ ]* 11.2 Add inline documentation
  - Add KDoc comments to all public APIs
  - Document Toon object methods
  - Document encoder and decoder classes
  - Add usage examples in KDoc
  - _Requirements: All_

- [ ]* 12. Final checkpoint - Ensure all tests pass
  - Ensure all tests pass, ask the user if questions arise.
