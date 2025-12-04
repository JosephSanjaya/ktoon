package io.ktoon.ktor

import io.ktor.http.ContentType
import io.ktoon.Toon
import io.ktor.client.plugins.contentnegotiation.ContentNegotiationConfig

/**
 * Registers TOON format support with Ktor's ContentNegotiation plugin.
 * 
 * This extension function enables automatic serialization and deserialization
 * of Kotlin objects using the token-efficient TOON format for HTTP requests
 * and responses.
 * 
 * @param contentType The content type to register for TOON format.
 *                    Defaults to "application/toon".
 * @param toon The Toon instance to use for serialization/deserialization.
 *             Defaults to Toon.Default for zero-configuration usage.
 * 
 * Example usage:
 * ```kotlin
 * val client = HttpClient(CIO) {
 *     install(ContentNegotiation) {
 *         toon()  // Use defaults
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
 * val client = HttpClient(CIO) {
 *     install(ContentNegotiation) {
 *         toon(toon = customToon)
 *     }
 * }
 * ```
 */
fun ContentNegotiationConfig.toon(
    contentType: ContentType = ContentType.parse("application/toon"),
    toon: Toon = Toon()
) {
    val converter = ToonContentConverter(toon)
    register(contentType, converter)
}
