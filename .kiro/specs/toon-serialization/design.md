# Design Document

## Overview

The ktoon-core module implements the TOON (Token-Oriented Object Notation) serialization format for Kotlin Multiplatform. The design leverages kotlinx.serialization's encoder/decoder architecture to provide a StringFormat implementation that converts between Kotlin objects and TOON format strings. The format uses two distinct modes: Indentation Mode for nested objects and Table Mode for collections, achieving 30-60% token reduction compared to JSON.

## Architecture

### High-Level Components

```
┌─────────────────────────────────────────────────────────┐
│                    Toon (StringFormat)                   │
│  - encodeToString<T>(serializer, value): String         │
│  - decodeFromString<T>(deserializer, string): T         │
└─────────────────────────────────────────────────────────┘
                    │                    │
        ┌───────────┘                    └───────────┐
        ▼                                            ▼
┌──────────────────┐                      ┌──────────────────┐
│   ToonEncoder    │                      │   ToonDecoder    │
│  (AbstractEncoder)│                      │ (AbstractDecoder)│
└──────────────────┘                      └──────────────────┘
        │                                            │
        ├─────────────────┐                         ├─────────────────┐
        ▼                 ▼                         ▼                 ▼
┌──────────────┐  ┌──────────────┐      ┌──────────────┐  ┌──────────────┐
│ Indentation  │  │ ToonRow      │      │ Indentation  │  │ ToonRow      │
│ Mode         │  │ Encoder      │      │ Parser       │  │ Decoder      │
└──────────────┘  └──────────────┘      └──────────────┘  └──────────────┘
```

### Core Classes

1. **Toon**: The main StringFormat implementation providing public API
2. **ToonEncoder**: State machine managing serialization modes
3. **ToonRowEncoder**: Specialized encoder for CSV-style collection rows
4. **ToonDecoder**: Line-aware parser managing deserialization modes
5. **ToonRowDecoder**: Specialized decoder for CSV-style collection rows
6. **ToonLexer**: Tokenizer for parsing TOON format with indentation awareness

## Components and Interfaces

### Toon (StringFormat)

```kotlin
@OptIn(ExperimentalSerializationApi::class)
object Toon : StringFormat {
    override val serializersModule: SerializersModule = EmptySerializersModule()
    
    override fun <T> encodeToString(serializer: SerializationStrategy<T>, value: T): String {
        val encoder = ToonEncoder()
        encoder.encodeSerializableValue(serializer, value)
        return encoder.toString()
    }
    
    override fun <T> decodeFromString(deserializer: DeserializationStrategy<T>, string: String): String {
        val decoder = ToonDecoder(string)
        return decoder.decodeSerializableValue(deserializer)
    }
}
```

### ToonEncoder

The encoder maintains a StringBuilder for output and tracks current indentation level. It implements a state machine that switches between Indentation Mode and Table Mode based on the structure being serialized.

**Key Methods:**
- `encodeValue(value: Any)`: Writes primitive values with proper formatting
- `beginStructure(descriptor: SerialDescriptor)`: Detects structure type and switches modes
- `beginCollection(descriptor: SerialDescriptor, collectionSize: Int)`: Writes table header and switches to ToonRowEncoder
- `encodeElement(descriptor: SerialDescriptor, index: Int)`: Writes field names in indentation mode

**State Management:**
- Current indentation level (Int)
- Current mode (Indentation or Table)
- StringBuilder for output accumulation

### ToonRowEncoder

Specialized encoder for writing CSV-style rows in Table Mode. Maintains a list of values for the current row and writes them comma-separated when the row is complete.

**Key Methods:**
- `encodeValue(value: Any)`: Accumulates values in current row
- `endStructure(descriptor: SerialDescriptor)`: Writes accumulated row with commas
- `encodeNull()`: Writes empty value for null fields

### ToonDecoder

The decoder parses TOON format line-by-line, tracking indentation levels to reconstruct object hierarchy. It uses ToonLexer for tokenization and switches to ToonRowDecoder when encountering table headers.

**Key Methods:**
- `decodeValue()`: Reads and parses next value from current line
- `decodeElementIndex(descriptor: SerialDescriptor)`: Determines next field to decode based on indentation
- `beginStructure(descriptor: SerialDescriptor)`: Detects table headers and switches to ToonRowDecoder
- `decodeCollectionSize(descriptor: SerialDescriptor)`: Extracts collection size from table header

**State Management:**
- Current line index
- Current indentation level
- Lines array (split input)
- ToonLexer instance

### ToonLexer

Tokenizes TOON format strings with indentation awareness. Provides methods to peek and consume tokens while tracking position.

**Key Methods:**
- `peekToken()`: Returns next token without consuming
- `consumeToken()`: Returns and consumes next token
- `currentIndentation()`: Returns indentation level of current line
- `isTableHeader()`: Detects table header pattern `name[N]{fields}:`

### ToonRowDecoder

Specialized decoder for parsing CSV-style rows in Table Mode. Splits row by commas and provides values sequentially.

**Key Methods:**
- `decodeValue()`: Returns next comma-separated value
- `decodeElementIndex(descriptor: SerialDescriptor)`: Tracks position in row
- `decodeNotNullMark()`: Checks if current value is empty (null)

## Data Models

### TOON Format Structure

**Indentation Mode Example:**
```
user:
  id: 123
  name: Alice
  profile:
    bio: Developer
    location: NYC
```

**Table Mode Example:**
```
users[2]{id,name,email}:
  1,Alice,alice@example.com
  2,Bob,bob@example.com
```

**Mixed Example:**
```
response:
  status: success
  users[2]{id,name}:
    1,Alice
    2,Bob
  metadata:
    count: 2
    timestamp: 1234567890
```

### Internal Data Structures

**EncoderState:**
```kotlin
private sealed class EncoderState {
    data class Indentation(val level: Int) : EncoderState()
    data class Table(val level: Int, val fieldNames: List<String>) : EncoderState()
}
```

**Token:**
```kotlin
private sealed class Token {
    data class Identifier(val value: String) : Token()
    data class Number(val value: String) : Token()
    data class StringLiteral(val value: String) : Token()
    object Colon : Token()
    object Comma : Token()
    data class TableHeader(val name: String, val size: Int, val fields: List<String>) : Token()
}
```

## Correctness Properties


*A property is a characteristic or behavior that should hold true across all valid executions of a system—essentially, a formal statement about what the system should do. Properties serve as the bridge between human-readable specifications and machine-verifiable correctness guarantees.*

### Core Serialization Properties

**Property 1: Round-trip identity**
*For any* serializable object, encoding it to TOON format and then decoding the result should produce an object equivalent to the original.
**Validates: Requirements 2.1**

**Property 2: Primitive field formatting**
*For any* object with primitive fields at any nesting level, each primitive field should be written in "key: value" format with indentation matching its nesting depth.
**Validates: Requirements 1.2**

**Property 3: Nested object indentation**
*For any* object with nested objects, the indentation level should increase by exactly one for each level of nesting.
**Validates: Requirements 1.3**

**Property 4: Collection table mode**
*For any* collection of objects, the encoder should produce output in table format with a header line followed by CSV rows.
**Validates: Requirements 1.4**

**Property 5: Table header format**
*For any* collection, the table header should match the pattern `name[N]{field1,field2,...}:` where N is the collection size and fields are comma-separated field names.
**Validates: Requirements 1.5**

### Parsing Properties

**Property 6: Indented key-value parsing**
*For any* valid TOON string with indented key-value pairs, the decoder should correctly reconstruct the nested object structure.
**Validates: Requirements 2.2**

**Property 7: Table mode row count**
*For any* table header declaring size N, the decoder should parse exactly N subsequent rows as collection elements.
**Validates: Requirements 2.3**

**Property 8: CSV field mapping**
*For any* table row with M comma-separated values, the decoder should map each value to the corresponding field declared in the table header.
**Validates: Requirements 2.4**

**Property 9: Empty value null interpretation**
*For any* CSV row containing empty values (consecutive commas or trailing comma), the decoder should interpret empty values as null.
**Validates: Requirements 2.5**

### Performance Properties

**Property 10: Single-pass traversal**
*For any* object graph, the encoder should visit each node exactly once during serialization.
**Validates: Requirements 3.1**

**Property 11: Streaming table headers**
*For any* collection, the encoder should write the complete table header before serializing any collection elements, without buffering the entire collection.
**Validates: Requirements 3.2**

**Property 12: Stateful mode switching**
*For any* object containing both nested objects and collections, mode switches between Indentation Mode and Table Mode should not cause re-traversal of previously serialized data.
**Validates: Requirements 3.3**

**Property 13: Forward-only nested collections**
*For any* object with nested collections, the encoder should process them in document order without backtracking.
**Validates: Requirements 3.5**

### Cross-Platform Properties

**Property 14: Platform output consistency**
*For any* serializable object, encoding on any KMP target (Android, iOS, JVM, JS, Wasm) should produce identical TOON format strings.
**Validates: Requirements 4.2, 4.3, 4.4, 4.5**

### Error Handling Properties

**Property 15: Invalid syntax errors**
*For any* invalid TOON string, the decoder should throw a SerializationException containing the line number and error description.
**Validates: Requirements 5.1**

**Property 16: Indentation errors**
*For any* TOON string with invalid indentation, the decoder should throw a SerializationException with context about the indentation error.
**Validates: Requirements 5.3**

**Property 17: Unsupported type errors**
*For any* attempt to serialize an unsupported type, the encoder should throw a SerializationException including the type information.
**Validates: Requirements 5.4**

### Null Handling Properties

**Property 18: Null primitive fields**
*For any* object with null primitive fields, the encoder should represent them in a way that preserves null during round-trip.
**Validates: Requirements 6.1**

**Property 19: Null nested objects**
*For any* object with null nested objects, the encoder should handle them without breaking the TOON format structure.
**Validates: Requirements 6.2**

**Property 20: Null fields in collections**
*For any* collection containing objects with null fields, the encoder should write empty values in CSV rows, and the decoder should reconstruct them as null.
**Validates: Requirements 6.3, 6.4**

**Property 21: Null parsing**
*For any* TOON string with null representations, the decoder should correctly assign null to nullable fields.
**Validates: Requirements 6.5**

### kotlinx.serialization Integration Properties

**Property 22: @Serializable compatibility**
*For any* data class annotated with @Serializable, the TOON format should serialize and deserialize it correctly without additional annotations.
**Validates: Requirements 7.1**

**Property 23: @SerialName support**
*For any* field with @SerialName annotation, the encoder should use the custom name in the TOON output.
**Validates: Requirements 7.2**

**Property 24: @Transient exclusion**
*For any* field marked with @Transient, the encoder should exclude it from the TOON output.
**Validates: Requirements 7.3**

**Property 25: Polymorphic serialization**
*For any* polymorphic class hierarchy with @Polymorphic annotation, the TOON format should correctly serialize and deserialize with type discrimination.
**Validates: Requirements 7.4**

**Property 26: Custom serializer delegation**
*For any* type with a registered custom serializer, the TOON format should delegate serialization and deserialization to that serializer.
**Validates: Requirements 7.5**

## Error Handling

### Encoder Errors

1. **Unsupported Types**: Throw SerializationException with type name when encountering types that cannot be serialized
2. **Circular References**: Detect and throw SerializationException for circular object graphs
3. **Invalid State**: Throw IllegalStateException if encoder methods are called in invalid sequence

### Decoder Errors

1. **Syntax Errors**: Throw SerializationException with line number and column for invalid TOON syntax
2. **Type Mismatches**: Throw SerializationException when parsed value type doesn't match expected type
3. **Missing Fields**: Throw SerializationException when required fields are missing from TOON input
4. **Indentation Errors**: Throw SerializationException when indentation is inconsistent or invalid
5. **Table Size Mismatches**: Throw SerializationException when actual row count doesn't match header size
6. **Unexpected EOF**: Throw SerializationException when input ends unexpectedly

### Error Message Format

All SerializationExceptions should include:
- Line number (for decoder errors)
- Column number (when applicable)
- Context snippet showing the problematic section
- Clear description of what went wrong
- Suggestion for how to fix (when possible)

Example: `SerializationException: Invalid indentation at line 5: expected 2 spaces but found 3`

## Testing Strategy

### Unit Testing

Unit tests will cover specific scenarios and edge cases:

1. **Format Validation Tests**
   - Verify TOON output matches expected format for known inputs
   - Test edge cases like empty collections, single-element collections
   - Verify indentation is correct for deeply nested structures

2. **Error Handling Tests**
   - Verify appropriate exceptions for invalid inputs
   - Test error messages contain required context
   - Verify graceful handling of malformed TOON strings

3. **Integration Tests**
   - Test with real-world data structures
   - Verify compatibility with kotlinx.serialization annotations
   - Test custom serializers work correctly



## Implementation Notes

### Phase 1: Encoder Implementation

1. Implement Toon object as StringFormat
2. Implement ToonEncoder extending AbstractEncoder
3. Implement state machine for mode switching
4. Implement ToonRowEncoder for table mode
5. Add null handling support
6. Implement error handling with descriptive messages

### Phase 2: Decoder Implementation

1. Implement ToonLexer for tokenization
2. Implement ToonDecoder extending AbstractDecoder
3. Implement line-by-line parsing with indentation tracking
4. Implement ToonRowDecoder for table mode
5. Add null parsing support
6. Implement comprehensive error handling

### Phase 3: Testing and Documentation

1. Write unit tests for specific scenarios
2. Test on all KMP targets
3. Create comprehensive README with usage examples
4. Add inline documentation and KDoc comments

### Key Design Decisions

1. **Single-Pass Architecture**: Use state machine to avoid buffering entire object graph
2. **Line-Based Parsing**: Simplifies indentation tracking and error reporting
3. **Separate Row Encoders/Decoders**: Clean separation between indentation and table modes
4. **Compile-Time Metadata**: Leverage kotlinx.serialization's compile-time code generation
5. **Explicit Null Handling**: Use empty values in CSV for nulls, special handling in indentation mode

### Performance Considerations

1. **StringBuilder Usage**: Minimize allocations by reusing StringBuilder
2. **Streaming**: Write output incrementally without buffering entire result
3. **Lazy Parsing**: Parse tokens on-demand rather than tokenizing entire input upfront
4. **Zero-Copy**: Avoid copying strings when possible, use substring views
5. **Inline Functions**: Use inline functions for hot paths to reduce call overhead
