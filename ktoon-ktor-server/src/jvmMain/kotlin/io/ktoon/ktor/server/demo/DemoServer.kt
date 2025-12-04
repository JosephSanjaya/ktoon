package io.ktoon.ktor.server.demo

import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.Application
import io.ktor.server.application.call
import io.ktor.server.application.install
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.plugins.calllogging.CallLogging
import io.ktor.server.plugins.compression.Compression
import io.ktor.server.plugins.compression.gzip
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.plugins.statuspages.StatusPages
import io.ktor.server.request.httpMethod
import io.ktor.server.request.path
import io.ktor.server.response.respondText
import io.ktor.server.routing.get
import io.ktor.server.routing.routing
import io.ktoon.ktor.server.toon
import kotlinx.serialization.SerializationException
import org.slf4j.event.Level
import io.ktor.http.HttpStatusCode

fun main() {
    embeddedServer(Netty, port = 8080, module = Application::demoModule)
        .start(wait = true)
}

fun Application.demoModule() {
    install(CallLogging) {
        level = Level.INFO
        format { call ->
            val status = call.response.status()
            val method = call.request.httpMethod.value
            val path = call.request.path()
            "$method $path - $status"
        }
    }
    
    install(StatusPages) {
        exception<SerializationException> { call, cause ->
            call.respondText(
                text = "Invalid TOON format: ${cause.message}",
                status = HttpStatusCode.BadRequest
            )
        }
        exception<IllegalStateException> { call, cause ->
            call.respondText(
                text = "Server error: ${cause.message}",
                status = HttpStatusCode.InternalServerError
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
        get("/") {
            call.respondText("""
                TOON Format Demo Server
                =======================
                
                Available endpoints:
                - POST   /users          Create a new user
                - GET    /users          List all users
                - GET    /users/{id}     Get user by ID
                - PUT    /users/{id}     Update user
                - DELETE /users/{id}     Delete user
                - POST   /users/invalid  Trigger error (for testing)
                
                Content Types:
                - application/toon (TOON format)
                - application/json (JSON format)
                
                Use Accept header to specify response format.
                Use Content-Type header to specify request format.
            """.trimIndent())
        }
        
        userRoutes()
    }
}
