package io.ktoon.ktor.server

import io.ktor.http.ContentType
import io.ktor.serialization.Configuration
import io.ktoon.Toon

/**
 * Registers TOON format support with Ktor Server's ContentNegotiation plugin.
 * 
 * This extension function enables automatic serialization and deserialization
 * of Kotlin objects using the token-efficient TOON format for HTTP request
 * and response bodies in server applications.
 * 
 * @param contentType The content type to register for TOON format.
 *                    Defaults to "application/toon".
 * @param toon The Toon instance to use for serialization/deserialization.
 *             Defaults to Toon() for zero-configuration usage.
 * 
 * Example usage:
 * ```kotlin
 * fun Application.module() {
 *     install(ContentNegotiation) {
 *         toon()  // Use defaults
 *     }
 *     
 *     routing {
 *         post("/users") {
 *             val user = call.receive<User>()
 *             call.respond(user)
 *         }
 *     }
 * }
 * ```
 * 
 * Example with custom configuration:
 * ```kotlin
 * val customToon = Toon(
 *     serializersModule = SerializersModule {
 *         contextual(LocalDateTime::class, LocalDateTimeSerializer)
 *     }
 * )
 * 
 * fun Application.module() {
 *     install(ContentNegotiation) {
 *         toon(toon = customToon)
 *     }
 * }
 * ```
 */
fun Configuration.toon(
    contentType: ContentType = ContentType.parse("application/toon"),
    toon: Toon = Toon()
) {
    val converter = ToonContentConverter(toon)
    register(contentType, converter)
}
