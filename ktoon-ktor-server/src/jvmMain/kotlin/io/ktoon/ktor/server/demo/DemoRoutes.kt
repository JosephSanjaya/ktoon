package io.ktoon.ktor.server.demo

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.delete
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.put
import io.ktor.server.routing.route
import java.time.Instant
import java.time.ZoneOffset

private val users = mutableMapOf<Int, User>()
private var nextId = 1

fun Route.userRoutes() {
    route("/users") {
        post {
            val request = call.receive<CreateUserRequest>()
            val now = Instant.now().atOffset(ZoneOffset.UTC).toString()
            val user = User(
                id = nextId++,
                name = request.name,
                email = request.email,
                createdAt = now
            )
            users[user.id] = user
            call.respond(HttpStatusCode.Created, user)
        }
        
        get {
            val userList = users.values.toList()
            call.respond(userList)
        }
        
        get("/{id}") {
            val id = call.parameters["id"]?.toIntOrNull()
            if (id == null) {
                call.respond(
                    HttpStatusCode.BadRequest,
                    ErrorResponse("invalid_id", "User ID must be a valid integer")
                )
                return@get
            }
            
            val user = users[id]
            if (user == null) {
                call.respond(
                    HttpStatusCode.NotFound,
                    ErrorResponse("user_not_found", "User with ID $id not found")
                )
                return@get
            }
            
            call.respond(user)
        }
        
        put("/{id}") {
            val id = call.parameters["id"]?.toIntOrNull()
            if (id == null) {
                call.respond(
                    HttpStatusCode.BadRequest,
                    ErrorResponse("invalid_id", "User ID must be a valid integer")
                )
                return@put
            }
            
            val existingUser = users[id]
            if (existingUser == null) {
                call.respond(
                    HttpStatusCode.NotFound,
                    ErrorResponse("user_not_found", "User with ID $id not found")
                )
                return@put
            }
            
            val request = call.receive<UpdateUserRequest>()
            val updatedUser = existingUser.copy(
                name = request.name ?: existingUser.name,
                email = request.email ?: existingUser.email
            )
            users[id] = updatedUser
            call.respond(updatedUser)
        }
        
        delete("/{id}") {
            val id = call.parameters["id"]?.toIntOrNull()
            if (id == null) {
                call.respond(
                    HttpStatusCode.BadRequest,
                    ErrorResponse("invalid_id", "User ID must be a valid integer")
                )
                return@delete
            }
            
            val user = users.remove(id)
            if (user == null) {
                call.respond(
                    HttpStatusCode.NotFound,
                    ErrorResponse("user_not_found", "User with ID $id not found")
                )
                return@delete
            }
            
            call.respond(HttpStatusCode.NoContent)
        }
        
        post("/invalid") {
            throw IllegalStateException("This endpoint intentionally triggers an error")
        }
    }
}
