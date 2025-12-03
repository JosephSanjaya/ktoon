# Requirements Document

## Introduction

The TOON (Token-Oriented Object Notation) Serialization Library core module (ktoon-core) is a Kotlin Multiplatform serialization format designed to reduce token usage for AI/LLM interactions by 30-60% compared to standard JSON. This module implements the core encoder and decoder for converting between Kotlin objects and TOON format strings, using a compact tabular representation for collections while maintaining full compatibility with kotlinx.serialization.

## Glossary

- **TOON Format**: Token-Oriented Object Notation, a compact serialization format that uses header-row notation for collections
- **ToonEncoder**: The serialization component that converts Kotlin objects to TOON format strings
- **ToonDecoder**: The deserialization component that parses TOON format strings back to Kotlin objects
- **Indentation Mode**: Serialization mode for nested objects using indentation-based hierarchy
- **Table Mode**: Serialization mode for collections using CSV-like header-row format
- **StringFormat**: kotlinx.serialization interface for text-based serialization formats


## Requirements

### Requirement 1

**User Story:** As a developer integrating with LLM APIs, I want to serialize Kotlin data classes to TOON format, so that I can reduce token usage and API costs by 30-60%.

#### Acceptance Criteria

1. WHEN a developer calls Toon.encodeToString with a serializable object THEN the ToonEncoder SHALL produce a valid TOON format string
2. WHEN the ToonEncoder encounters a primitive field THEN the ToonEncoder SHALL write it in key-value format with proper indentation
3. WHEN the ToonEncoder encounters a nested object THEN the ToonEncoder SHALL increase indentation level and serialize child fields
4. WHEN the ToonEncoder encounters a collection of objects THEN the ToonEncoder SHALL switch to Table Mode and write the header-row format
5. WHEN the ToonEncoder writes a collection header THEN the ToonEncoder SHALL include the collection name, size, and field names in the format `name[N]{field1,field2}:`

### Requirement 2

**User Story:** As a developer receiving TOON-formatted data, I want to deserialize TOON strings back to Kotlin objects, so that I can process structured data from LLM responses or APIs.

#### Acceptance Criteria

1. WHEN a developer calls Toon.decodeFromString with a valid TOON string THEN the ToonDecoder SHALL reconstruct the original object structure
2. WHEN the ToonDecoder encounters indented key-value pairs THEN the ToonDecoder SHALL parse them as object fields
3. WHEN the ToonDecoder encounters a collection header line ending with colon THEN the ToonDecoder SHALL switch to CSV parsing mode for the specified number of rows
4. WHEN the ToonDecoder parses CSV rows THEN the ToonDecoder SHALL map comma-separated values to the fields declared in the header
5. WHEN the ToonDecoder encounters an empty value between commas THEN the ToonDecoder SHALL interpret it as null

### Requirement 3

**User Story:** As a library maintainer, I want the serialization to be O(N) single-pass, so that the library performs efficiently even with large datasets.

#### Acceptance Criteria

1. WHEN the ToonEncoder serializes an object THEN the encoder SHALL traverse the object graph exactly once
2. WHEN the ToonEncoder detects a collection of objects THEN the encoder SHALL write the complete header immediately without buffering all elements
3. WHEN the ToonEncoder switches between Indentation Mode and Table Mode THEN the encoder SHALL maintain state without re-traversing previous data
4. WHEN the ToonEncoder uses kotlinx.serialization metadata THEN the encoder SHALL access field information at compile-time to avoid runtime reflection
5. WHEN the ToonEncoder processes nested collections THEN the encoder SHALL handle them in a single forward pass

### Requirement 6

**User Story:** As a Kotlin Multiplatform developer, I want TOON serialization to work across all KMP targets, so that I can use the same serialization logic on Android, iOS, JVM, JS, and Wasm.

#### Acceptance Criteria

1. WHEN the ktoon-core module is compiled for any KMP target THEN the compilation SHALL succeed without platform-specific dependencies
2. WHEN TOON serialization runs on Android THEN the encoder and decoder SHALL produce identical results to other platforms
3. WHEN TOON serialization runs on iOS THEN the encoder and decoder SHALL produce identical results to other platforms
4. WHEN TOON serialization runs on JVM THEN the encoder and decoder SHALL produce identical results to other platforms
5. WHEN TOON serialization runs on JS or Wasm THEN the encoder and decoder SHALL produce identical results to other platforms

### Requirement 7

**User Story:** As a developer debugging serialization issues, I want clear error messages when TOON parsing fails, so that I can quickly identify and fix format problems.

#### Acceptance Criteria

1. WHEN the ToonDecoder encounters invalid syntax THEN the decoder SHALL throw a SerializationException with the line number and error description
2. WHEN the ToonDecoder encounters mismatched collection size THEN the decoder SHALL throw an exception indicating expected versus actual row count
3. WHEN the ToonDecoder encounters unexpected indentation THEN the decoder SHALL throw an exception with context about the indentation error
4. WHEN the ToonEncoder encounters an unsupported type THEN the encoder SHALL throw a SerializationException with the type information
5. WHEN serialization or deserialization fails THEN the error message SHALL include sufficient context for debugging without exposing sensitive data

### Requirement 8

**User Story:** As a developer working with nullable fields, I want TOON format to handle null values correctly, so that data integrity is maintained during serialization round-trips.

#### Acceptance Criteria

1. WHEN a serializable object contains a null primitive field THEN the ToonEncoder SHALL represent it appropriately in the output
2. WHEN a serializable object contains a null nested object THEN the ToonEncoder SHALL handle it without breaking the format structure
3. WHEN a collection contains objects with null fields THEN the ToonEncoder SHALL write empty values in the CSV rows
4. WHEN the ToonDecoder parses an empty CSV value THEN the decoder SHALL reconstruct it as null in the target object
5. WHEN the ToonDecoder parses a null representation THEN the decoder SHALL correctly assign null to nullable fields

### Requirement 9

**User Story:** As a developer integrating TOON with existing codebases, I want the library to work seamlessly with kotlinx.serialization annotations, so that I don't need to modify my data classes.

#### Acceptance Criteria

1. WHEN a data class is annotated with @Serializable THEN the TOON format SHALL serialize it without additional annotations
2. WHEN a field uses @SerialName annotation THEN the ToonEncoder SHALL use the custom name in the output format
3. WHEN a field is marked with @Transient THEN the ToonEncoder SHALL exclude it from serialization
4. WHEN a class uses @Polymorphic serialization THEN the TOON format SHALL handle type discrimination correctly
5. WHEN custom serializers are registered THEN the TOON format SHALL delegate to them appropriately
