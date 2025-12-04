package io.ktoon.backend

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.serialization.kotlinx.json.*
import io.ktoon.backend.models.User
import kotlinx.serialization.json.Json

fun main() {
    embeddedServer(Netty, port = 8080, host = "0.0.0.0") {
        install(ContentNegotiation) {
            json(Json {
                prettyPrint = true
            })
        }
        
        routing {
            get("/users") {
                val users = listOf(
                    User(1, "Alice", "alice@example.com", 28),
                    User(2, "Bob", "bob@example.com", 35),
                    User(3, "Charlie", "charlie@example.com", 42),
                    User(4, "Diana", "diana@example.com", 31),
                    User(5, "Eve", "eve@example.com", 29)
                )
                
                // Check Accept header for format preference
                val acceptHeader = call.request.headers["Accept"] ?: "application/json"
                
                when {
                    acceptHeader.contains("application/toon") -> {
                        // Manually format as TOON table mode
                        val toonResponse = buildString {
                            appendLine("users[${users.size}]{id,name,email,age}:")
                            users.forEach { user ->
                                appendLine("  ${user.id},${user.name},${user.email},${user.age}")
                            }
                        }.trimEnd()
                        call.respondText(toonResponse, ContentType.parse("application/toon"))
                    }
                    else -> {
                        // Default to JSON
                        call.respond(users)
                    }
                }
            }
            
            get("/") {
                call.respondText("""
                    KToon Backend Demo
                    
                    Available endpoints:
                    - GET /users (supports JSON and TOON formats)
                    
                    Try:
                    curl -H "Accept: application/json" http://localhost:8080/users
                    curl -H "Accept: application/toon" http://localhost:8080/users
                """.trimIndent())
            }
        }
        
        println("Server started at http://localhost:8080")
    }.start(wait = true)
}
