package io.ktoon.ktor.server.demo

import kotlinx.serialization.Serializable

@Serializable
data class User(
    val id: Int,
    val name: String,
    val email: String,
    val createdAt: String
)

@Serializable
data class CreateUserRequest(
    val name: String,
    val email: String
)

@Serializable
data class UpdateUserRequest(
    val name: String?,
    val email: String?
)

@Serializable
data class ErrorResponse(
    val error: String,
    val message: String
)
