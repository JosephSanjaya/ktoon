package io.ktoon.ktor.server

import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.plugins.compression.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.plugins.statuspages.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.testing.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertContains
import kotlin.test.assertTrue

class ToonServerPipelineIntegrationTest {
    
    @Serializable
    data class User(val id: Int, val name: String)
    
    @Serializable
    data class ErrorResponse(val error: String, val message: String)
    
    @Test
    fun testContentTypeNegotiationWithMultipleConverters() = testApplication {
        application {
            install(ContentNegotiation) {
                json(Json { prettyPrint = true })
                toon()
            }
            
            routing {
                post("/users") {
                    val user = call.receive<User>()
                    call.respond(user)
                }
            }
        }
        
        // Test TOON format request and response
        val toonResponse = client.post("/users") {
            contentType(ContentType.parse("application/toon"))
            accept(ContentType.parse("application/toon"))
            setBody("""
                id: 123
                name: Alice
            """.trimIndent())
        }
        
        assertEquals(HttpStatusCode.OK, toonResponse.status)
        assertEquals("application/toon; charset=UTF-8", toonResponse.contentType()?.toString())
        val toonBody = toonResponse.bodyAsText()
        assertContains(toonBody, "id: 123")
        assertContains(toonBody, "name: Alice")
        
        // Test JSON format request and response
        val jsonResponse = client.post("/users") {
            contentType(ContentType.Application.Json)
            accept(ContentType.Application.Json)
            setBody("""{"id":456,"name":"Bob"}""")
        }
        
        assertEquals(HttpStatusCode.OK, jsonResponse.status)
        assertTrue(jsonResponse.contentType()?.match(ContentType.Application.Json) == true)
        val jsonBody = jsonResponse.bodyAsText()
        assertContains(jsonBody, "456")
        assertContains(jsonBody, "Bob")
    }
    
    @Test
    fun testStatusPagesPluginHandlesSerializationErrors() = testApplication {
        application {
            install(StatusPages) {
                exception<SerializationException> { call, cause ->
                    call.respond(
                        HttpStatusCode.BadRequest,
                        ErrorResponse("serialization_error", cause.message ?: "Unknown error")
                    )
                }
            }
            
            install(ContentNegotiation) {
                json()
                toon()
            }
            
            routing {
                post("/users") {
                    val user = call.receive<User>()
                    call.respond(user)
                }
            }
        }
        
        // Send invalid TOON format
        val response = client.post("/users") {
            contentType(ContentType.parse("application/toon"))
            setBody("invalid: toon: format:")
        }
        
        assertEquals(HttpStatusCode.BadRequest, response.status)
    }
    
    @Test
    fun testStatusPagesWithToonResponseFormat() = testApplication {
        application {
            install(StatusPages) {
                exception<IllegalArgumentException> { call, cause ->
                    call.respond(
                        HttpStatusCode.BadRequest,
                        ErrorResponse("validation_error", cause.message ?: "Invalid input")
                    )
                }
            }
            
            install(ContentNegotiation) {
                toon()
            }
            
            routing {
                post("/users") {
                    val user = call.receive<User>()
                    if (user.id <= 0) {
                        throw IllegalArgumentException("User ID must be positive")
                    }
                    call.respond(user)
                }
            }
        }
        
        val response = client.post("/users") {
            contentType(ContentType.parse("application/toon"))
            accept(ContentType.parse("application/toon"))
            setBody("""
                id: -1
                name: Invalid
            """.trimIndent())
        }
        
        assertEquals(HttpStatusCode.BadRequest, response.status)
        val body = response.bodyAsText()
        assertContains(body, "error: validation_error")
        assertContains(body, "message: User ID must be positive")
    }
    
    @Test
    fun testCompressionPluginWithToonResponses() = testApplication {
        application {
            install(Compression) {
                gzip {
                    priority = 1.0
                }
            }
            
            install(ContentNegotiation) {
                toon()
            }
            
            routing {
                get("/users") {
                    val users = List(10) { User(it, "User$it") }
                    call.respond(users)
                }
            }
        }
        
        // Test without compression first to verify TOON format works
        val uncompressedResponse = client.get("/users") {
            accept(ContentType.parse("application/toon"))
        }
        
        assertEquals(HttpStatusCode.OK, uncompressedResponse.status)
        assertEquals("application/toon; charset=UTF-8", uncompressedResponse.contentType()?.toString())
        val uncompressedBody = uncompressedResponse.bodyAsText()
        // Check for table mode format: [size]{fields}:
        assertContains(uncompressedBody, "[10]{id,name}:")
        
        // Test with compression - Ktor test client handles decompression automatically
        val compressedResponse = client.get("/users") {
            accept(ContentType.parse("application/toon"))
            header(HttpHeaders.AcceptEncoding, "gzip")
        }
        
        assertEquals(HttpStatusCode.OK, compressedResponse.status)
        assertEquals("application/toon; charset=UTF-8", compressedResponse.contentType()?.toString())
        
        // Verify compression was applied (header may or may not be present depending on size)
        // The important thing is that the response is still valid TOON format
        val compressedBody = compressedResponse.bodyAsText()
        assertContains(compressedBody, "[10]{id,name}:")
    }
    
    @Test
    fun testMultiplePluginsWorkTogether() = testApplication {
        application {
            install(StatusPages) {
                exception<SerializationException> { call, cause ->
                    call.respond(
                        HttpStatusCode.BadRequest,
                        ErrorResponse("serialization_error", cause.message ?: "Unknown error")
                    )
                }
            }
            
            install(Compression) {
                gzip()
            }
            
            install(ContentNegotiation) {
                json()
                toon()
            }
            
            routing {
                post("/users") {
                    val user = call.receive<User>()
                    call.respond(user)
                }
                
                get("/users") {
                    val users = List(10) { User(it, "User$it") }
                    call.respond(users)
                }
            }
        }
        
        // Test POST with TOON format
        val postResponse = client.post("/users") {
            contentType(ContentType.parse("application/toon"))
            accept(ContentType.parse("application/toon"))
            setBody("""
                id: 999
                name: TestUser
            """.trimIndent())
        }
        
        assertEquals(HttpStatusCode.OK, postResponse.status)
        assertContains(postResponse.bodyAsText(), "id: 999")
        
        // Test GET with TOON format (compression may or may not be applied depending on size)
        val getResponse = client.get("/users") {
            accept(ContentType.parse("application/toon"))
        }
        
        assertEquals(HttpStatusCode.OK, getResponse.status)
        assertContains(getResponse.bodyAsText(), "[10]{id,name}:")
    }
    
    @Test
    fun testContentNegotiationBasedOnAcceptHeader() = testApplication {
        application {
            install(ContentNegotiation) {
                json()
                toon()
            }
            
            routing {
                get("/user") {
                    call.respond(User(123, "Alice"))
                }
            }
        }
        
        // Request with Accept: application/toon
        val toonResponse = client.get("/user") {
            accept(ContentType.parse("application/toon"))
        }
        
        assertEquals(HttpStatusCode.OK, toonResponse.status)
        assertEquals("application/toon; charset=UTF-8", toonResponse.contentType()?.toString())
        val toonBody = toonResponse.bodyAsText()
        assertContains(toonBody, "id: 123")
        assertContains(toonBody, "name: Alice")
        
        // Request with Accept: application/json
        val jsonResponse = client.get("/user") {
            accept(ContentType.Application.Json)
        }
        
        assertEquals(HttpStatusCode.OK, jsonResponse.status)
        assertTrue(jsonResponse.contentType()?.match(ContentType.Application.Json) == true)
        val jsonBody = jsonResponse.bodyAsText()
        assertContains(jsonBody, "123")
        assertContains(jsonBody, "Alice")
    }
    
    @Test
    fun testToonConverterSelectedForToonContentType() = testApplication {
        application {
            install(ContentNegotiation) {
                json()
                toon()
            }
            
            routing {
                post("/echo") {
                    val user = call.receive<User>()
                    call.respond(user)
                }
            }
        }
        
        // Send TOON request, expect TOON response
        val response = client.post("/echo") {
            contentType(ContentType.parse("application/toon"))
            accept(ContentType.parse("application/toon"))
            setBody("""
                id: 777
                name: ToonUser
            """.trimIndent())
        }
        
        assertEquals(HttpStatusCode.OK, response.status)
        assertEquals("application/toon; charset=UTF-8", response.contentType()?.toString())
        
        val body = response.bodyAsText()
        assertContains(body, "id: 777")
        assertContains(body, "name: ToonUser")
    }
    
    @Test
    fun testErrorHandlingPreservesContext() = testApplication {
        application {
            install(StatusPages) {
                exception<SerializationException> { call, cause ->
                    call.respondText(
                        text = "SerializationError: ${cause.message}",
                        status = HttpStatusCode.BadRequest
                    )
                }
            }
            
            install(ContentNegotiation) {
                toon()
            }
            
            routing {
                post("/users") {
                    val user = call.receive<User>()
                    call.respond(user)
                }
            }
        }
        
        // Send truly malformed TOON (invalid syntax)
        val response = client.post("/users") {
            contentType(ContentType.parse("application/toon"))
            setBody("this is not valid toon format at all")
        }
        
        // Verify that we get a BadRequest status (error was caught and handled)
        assertEquals(HttpStatusCode.BadRequest, response.status)
        val errorMessage = response.bodyAsText()
        // The error message should not be empty and should contain our custom prefix
        assertTrue(errorMessage.isNotEmpty(), "Expected non-empty error message but got empty string")
        assertTrue(errorMessage.contains("SerializationError:"), "Expected error message to contain 'SerializationError:' but got: $errorMessage")
    }
}
