# Implementation Plan

- [x] 1. Set up ktoon-ktor-server module structure




  - Create module directory and build.gradle.kts file
  - Configure Kotlin Multiplatform with commonMain source set
  - Add dependencies: ktor-server-core, ktor-server-content-negotiation, ktoon-core
  - Add test dependencies: ktor-server-test-host, kotlin-test, kotest-property
  - Configure module in settings.gradle.kts
  - Update libs.versions.toml with any new version entries
  - _Requirements: 1.1, 1.2_

- [x] 2. Implement ToonContentConverter for Ktor Server




  - [x] 2.1 Create ToonContentConverter class implementing ContentConverter interface


    - Write class with custom Toon instance parameter
    - Implement serialize() method for response bodies
    - Implement deserialize() method for request bodies
    - Handle charset encoding/decoding
    - _Requirements: 2.1, 2.2, 3.1, 3.2, 4.1, 4.2, 4.3_
  
  - [ ]* 2.2 Write property test for round-trip serialization
    - **Property 1: Round-trip serialization through HTTP server**
    - **Validates: Requirements 2.2, 3.2**
  
  - [ ]* 2.3 Write property test for Content-Type header correctness
    - **Property 2: Content-Type header correctness**
    - **Validates: Requirements 3.2, 4.3**

- [x] 3. Implement ContentNegotiation extension function




  - [x] 3.1 Create ContentNegotiationExt.kt with toon() extension function


    - Write extension function for ContentNegotiation.Configuration
    - Accept contentType parameter with default "application/toon"
    - Accept custom Toon instance parameter with default Toon.Default
    - Register ToonContentConverter with provided parameters
    - _Requirements: 1.1, 1.2, 1.3, 1.4, 1.5_
  
  - [ ]* 3.2 Write property test for request Content-Type recognition
    - **Property 3: Request Content-Type recognition**
    - **Validates: Requirements 2.1**

- [x] 4. Implement request body deserialization




  - [x] 4.1 Implement deserialize() method in ToonContentConverter

    - Read ByteReadChannel to string using specified charset
    - Call Toon.decodeFromString() with appropriate deserializer
    - Handle null/empty request bodies
    - Preserve SerializationException with full context
    - _Requirements: 2.1, 2.2, 2.3, 2.4, 2.5, 4.2_
  
  - [ ]* 4.2 Write property test for charset handling in requests
    - **Property 4: Charset handling in requests**
    - **Validates: Requirements 2.2, 4.2**
  
  - [ ]* 4.3 Write property test for error preservation
    - **Property 5: Error preservation**
    - **Validates: Requirements 2.3, 3.3, 5.1, 5.2, 5.3, 5.4, 5.5**

- [x] 5. Implement response body serialization



  - [x] 5.1 Implement serialize() method in ToonContentConverter


    - Call Toon.encodeToString() with appropriate serializer
    - Convert string to ByteArray using UTF-8 charset
    - Create OutgoingContent.ByteArrayContent with correct Content-Type
    - Handle null values appropriately
    - Preserve SerializationException with full context
    - _Requirements: 3.1, 3.2, 3.3, 3.4, 3.5, 4.3_
  
  - [ ]* 5.2 Write property test for annotation support delegation
    - **Property 6: Annotation support delegation**
    - **Validates: Requirements 6.1, 6.2**
  
  - [ ]* 5.3 Write property test for polymorphic type preservation
    - **Property 7: Polymorphic type preservation**
    - **Validates: Requirements 2.5, 6.3**
  
  - [ ]* 5.4 Write property test for collection table mode usage
    - **Property 8: Collection table mode usage**
    - **Validates: Requirements 3.5**

- [x] 6. Checkpoint - Ensure all tests pass




  - Ensure all tests pass, ask the user if questions arise.

- [x] 7. Implement custom Toon instance support



  - [x] 7.1 Add custom Toon instance parameter to extension function

    - Update toon() extension to accept custom Toon instance
    - Pass custom instance to ToonContentConverter constructor
    - Document usage in code comments
    - _Requirements: 1.3, 1.5, 6.4, 6.5_
  
  - [ ]* 7.2 Write property test for custom Toon instance delegation
    - **Property 9: Custom Toon instance delegation**
    - **Validates: Requirements 1.3, 6.4**

- [x] 8. Implement pipeline integration



  - [x] 8.1 Ensure ToonContentConverter integrates with Ktor Server pipeline


    - Verify content type negotiation works with multiple converters
    - Test interaction with StatusPages plugin for error handling
    - Test interaction with CallLogging plugin
    - Test interaction with Compression plugin
    - _Requirements: 7.1, 7.2, 7.3, 7.4, 7.5_
  
  - [ ]* 8.2 Write property test for pipeline integration
    - **Property 10: Pipeline integration**
    - **Validates: Requirements 7.1, 7.2**
  
  - [ ]* 8.3 Write integration tests for plugin interactions
    - Test with StatusPages for error responses
    - Test with CallLogging for request/response logging
    - Test with Compression for compressed responses
    - Test with multiple content converters (JSON + TOON)
    - _Requirements: 7.1, 7.2, 7.3, 7.4, 7.5_

- [x] 9. Create demo server application



  - [x] 9.1 Set up demo server structure in jvmMain


    - Create DemoServer.kt with main function
    - Create DemoModels.kt with sample data classes
    - Create DemoRoutes.kt with route definitions
    - _Requirements: 8.1, 8.2, 8.3_
  
  - [x] 9.2 Implement User Management API routes

    - POST /users - Create user endpoint
    - GET /users - List users endpoint
    - GET /users/{id} - Get user by ID endpoint
    - PUT /users/{id} - Update user endpoint
    - DELETE /users/{id} - Delete user endpoint
    - _Requirements: 8.1, 8.2, 8.3_
  
  - [x] 9.3 Add error handling demonstration

    - Install StatusPages plugin
    - Add error handler for SerializationException
    - Create /users/invalid endpoint to trigger errors
    - _Requirements: 8.4_
  
  - [x] 9.4 Add logging and debugging features

    - Install CallLogging plugin
    - Log request/response bodies for debugging
    - Add console output showing TOON format in action
    - _Requirements: 8.5_
  
  - [x] 9.5 Add multiple content type support

    - Install JSON content negotiation alongside TOON
    - Demonstrate content negotiation based on Accept header
    - Add documentation comments explaining behavior
    - _Requirements: 8.1, 8.2_

- [x] 10. Final Checkpoint - Ensure all tests pass




  - Ensure all tests pass, ask the user if questions arise.

- [x] 11. Create documentation



  - [x] 11.1 Create README.md for ktoon-ktor-server


    - Add installation instructions
    - Add basic usage examples
    - Add configuration options
    - Add link to API documentation
    - _Requirements: 1.1, 1.2, 1.3_
  
  - [x] 11.2 Create EXAMPLES.md with comprehensive usage scenarios


    - Basic server setup example
    - Custom Toon instance example
    - Error handling example
    - Multiple content types example
    - Demo server usage instructions
    - _Requirements: 8.1, 8.2, 8.3, 8.4, 8.5_
  
  - [x] 11.3 Create API.md with complete API reference


    - Document toon() extension function
    - Document ToonContentConverter class (internal)
    - Document supported content types
    - Document error handling behavior
    - Document charset handling
    - _Requirements: 1.1, 1.2, 1.3, 1.4, 1.5, 4.1, 4.2, 4.3_
