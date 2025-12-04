package io.ktoon.api

import io.ktoon.models.User
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import kotlinx.serialization.json.Json

class ApiClient(private val baseUrl: String = "http://localhost:8080") {
    
    private val jsonParser = Json {
        prettyPrint = true
        isLenient = true
        ignoreUnknownKeys = true
    }
    
    private val client = HttpClient()
    
    suspend fun getUsersToon(): Pair<List<User>, String> {
        return try {
            val response: HttpResponse = client.get("$baseUrl/users") {
                header("Accept", "application/toon")
            }
            val rawText = response.bodyAsText()
            val users = io.ktoon.Toon.decodeFromString(kotlinx.serialization.builtins.ListSerializer(User.serializer()), rawText)
            users to rawText
        } catch (e: Exception) {
            throw Exception("Failed to fetch users in TOON format: ${e.message}", e)
        }
    }
    
    suspend fun getUsersJson(): Pair<List<User>, String> {
        return try {
            val response: HttpResponse = client.get("$baseUrl/users") {
                header("Accept", "application/json")
            }
            val rawText = response.bodyAsText()
            val users = jsonParser.decodeFromString(kotlinx.serialization.builtins.ListSerializer(User.serializer()), rawText)
            users to rawText
        } catch (e: Exception) {
            throw Exception("Failed to fetch users in JSON format: ${e.message}", e)
        }
    }
    
    fun close() {
        client.close()
    }
}
