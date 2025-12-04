# Implementation Plan

- [x] 1. Set up ktoon-ktor module structure and dependencies





  - Remove unnecessary `buildlogic.multiplatform.cmp` plugin (Compose not needed)
  - Remove `composeHotReload` plugin from build.gradle.kts
  - Remove all Compose-related dependencies (compose.desktop, kotlinx.coroutinesSwing)
  - Remove web-specific and JVM-specific source set dependencies
  - Add ktoon-core project dependency: `implementation(project(":ktoon-core"))`
  - Add Ktor client core dependency: `implementation(sjy.ktor.client.core)`
  - Add Ktor ContentNegotiation dependency: `implementation(sjy.ktor.client.contentNegotiation)`
  - Add Ktor mock client for testing: `implementation(sjy.ktor.client.mock)` in commonTest
  - Update Android namespace to "io.ktoon.ktor"
  - Create package structure `io.ktoon.ktor` in `commonMain/kotlin`
  - _Requirements: 5.1, 5.2, 5.3, 5.4, 5.5_

- [x] 2. Implement ToonContentConverter








- [x] 2.1 Create ToonContentConverter class implementing ContentConverter interface


  - Define class with Toon instance parameter
  - Implement serialize() method for request serialization
  - Implement deserialize() method for response deserialization
  - Handle charset encoding/decoding
  - _Requirements: 2.1, 2.2, 2.3, 3.1, 3.2, 3.5_

- [x] 2.2 Implement request body serialization logic


  - Handle null values by returning null
  - Call Toon.encodeToString() for serialization
  - Convert string to ByteReadChannel with correct charset
  - Set Content-Type header to "application/toon; charset=UTF-8"
  - Propagate SerializationException on failure
  - _Requirements: 2.1, 2.2, 2.3, 2.4, 2.5_

- [x] 2.3 Implement response body deserialization logic


  - Read ByteReadChannel to string using specified charset
  - Handle empty response bodies based on type nullability
  - Call Toon.decodeFromString() for deserialization
  - Use TypeInfo to determine target type
  - Propagate SerializationException with original context
  - _Requirements: 3.1, 3.2, 3.3, 3.4, 3.5, 6.5_

- [ ]* 2.4 Write property test for round-trip serialization
  - **Property 1: Round-trip serialization through HTTP**
  - **Validates: Requirements 2.1, 3.1, 3.2, 6.1**

- [ ]* 2.5 Write property test for Content-Type header correctness
  - **Property 2: Content-Type header correctness**
  - **Validates: Requirements 1.2, 1.3, 2.2, 2.3**

- [ ]* 2.6 Write property test for charset handling
  - **Property 4: Charset handling**
  - **Validates: Requirements 3.5**

- [ ]* 2.7 Write property test for error preservation
  - **Property 5: Error preservation**
  - **Validates: Requirements 2.5, 3.4, 4.5**

- [x] 3. Implement ContentNegotiation extension function



- [x] 3.1 Create extension function for ContentNegotiation.Configuration



  - Define toon() extension function
  - Accept optional ContentType parameter (default: "application/toon")
  - Accept optional Toon instance parameter (default: Toon.Default)
  - Register ToonContentConverter with ContentNegotiation
  - _Requirements: 1.1, 1.2, 7.1, 7.2_

- [x] 3.2 Implement Content-Type registration logic


  - Register converter for "application/toon" content type
  - Support custom content types if provided
  - Ensure UTF-8 charset is default
  - _Requirements: 1.2, 1.3, 1.4_

- [ ]* 3.3 Write property test for Content-Type selection
  - **Property 3: Content-Type selection**
  - **Validates: Requirements 1.4**

- [ ]* 3.4 Write unit test for registration API
  - Verify toon() extension function can be called
  - Verify default parameters work correctly
  - Verify custom parameters are accepted
  - _Requirements: 1.1, 7.1, 7.2_

- [x] 4. Implement annotation and type support




- [x] 4.1 Verify @Serializable annotation support


  - Test with various @Serializable classes
  - Ensure delegation to Toon format works correctly
  - _Requirements: 6.1_

- [x] 4.2 Verify @SerialName and @Transient annotation support


  - Test with classes using @SerialName
  - Test with classes using @Transient
  - Ensure output matches direct Toon.encodeToString() calls
  - _Requirements: 6.2, 6.3_

- [x] 4.3 Verify polymorphic type support


  - Test with polymorphic type hierarchies
  - Ensure concrete types are preserved through round-trip
  - _Requirements: 6.4_

- [ ]* 4.4 Write property test for annotation support delegation
  - **Property 6: Annotation support delegation**
  - **Validates: Requirements 6.2, 6.3**

- [ ]* 4.5 Write property test for polymorphic type preservation
  - **Property 7: Polymorphic type preservation**
  - **Validates: Requirements 6.4**

- [ ]* 4.6 Write property test for TypeInfo usage
  - **Property 8: TypeInfo usage**
  - **Validates: Requirements 6.5**

- [x] 5. Implement custom configuration support




- [x] 5.1 Support custom Toon instances


  - Verify custom Toon instance can be passed to toon() function
  - Verify custom instance is used by ToonContentConverter
  - Test with custom SerializersModule
  - _Requirements: 7.1, 7.3_

- [x] 5.2 Verify custom serializer delegation


  - Create custom serializers in SerializersModule
  - Register with custom Toon instance
  - Verify ContentConverter uses custom serializers
  - _Requirements: 7.3_

- [ ]* 5.3 Write property test for custom Toon instance delegation
  - **Property 9: Custom Toon instance delegation**
  - **Validates: Requirements 7.3**

- [x] 6. Implement error handling





- [x] 6.1 Handle serialization errors


  - Catch and propagate SerializationException from Toon.encodeToString()
  - Preserve original error message and stack trace
  - Test with unsupported types
  - _Requirements: 2.5, 4.1_


- [x] 6.2 Handle deserialization errors



  - Catch and propagate SerializationException from Toon.decodeFromString()
  - Preserve line numbers and context from ToonDecoder
  - Test with malformed TOON format
  - Test with type mismatches
  - _Requirements: 3.4, 4.2, 4.3_



- [x] 6.3 Handle charset errors




  - Test with invalid charsets
  - Ensure clear error messages
  - _Requirements: 4.4_

- [ ]* 6.4 Write unit tests for error scenarios
  - Test unsupported type serialization error
  - Test malformed TOON deserialization error
  - Test type mismatch deserialization error
  - Test charset decoding error
  - Verify error messages contain appropriate context
  - _Requirements: 4.1, 4.2, 4.3, 4.4, 4.5_

- [x] 7. Implement null handling




- [x] 7.1 Handle null request bodies


  - Return null from serialize() for null input
  - Verify Ktor handles null body correctly
  - _Requirements: 2.4_

- [x] 7.2 Handle empty response bodies


  - Handle empty responses for nullable types
  - Let ToonDecoder handle empty responses for non-nullable types
  - _Requirements: 3.3_

- [ ]* 7.3 Write unit tests for null handling
  - Test null request body
  - Test empty response body with nullable type
  - Test empty response body with non-nullable type
  - _Requirements: 2.4, 3.3_

- [x] 8. Checkpoint - Ensure all tests pass




  - Ensure all tests pass, ask the user if questions arise.

- [x] 9. Create integration tests





- [ ]* 9.1 Write mock server integration test
  - Use ktor-client-mock to create mock server
  - Test full request/response cycle
  - Verify Content-Type headers
  - Verify serialization/deserialization
  - _Requirements: 1.2, 2.1, 2.2, 3.1, 3.2_

- [ ]* 9.2 Write multi-content-type integration test
  - Register multiple converters (JSON, TOON)
  - Verify correct converter is selected based on Content-Type
  - _Requirements: 1.4_

- [ ]* 9.3 Write platform-specific tests
  - Test on JVM platform
  - Test on at least one other platform (Android or iOS)
  - Verify identical behavior across platforms
  - _Requirements: 5.1, 5.2, 5.3, 5.4, 5.5_

- [x] 10. Final checkpoint - Ensure all tests pass





  - Ensure all tests pass, ask the user if questions arise.

- [x] 11. Create documentation and examples





- [x] 11.1 Write README for ktoon-ktor module

  - Document installation instructions
  - Provide basic usage examples
  - Show custom configuration examples
  - Document error handling
  - _Requirements: 1.1, 7.1, 7.2_

- [x] 11.2 Create usage examples


  - Basic client setup example
  - Custom Toon instance example
  - Multiple content types example
  - Error handling example
  - _Requirements: 1.1, 2.1, 3.1, 7.1_

- [x] 11.3 Document API surface


  - Document toon() extension function
  - Document ToonContentConverter (if public)
  - Document supported features and limitations
  - _Requirements: 1.1, 7.1, 7.2_
