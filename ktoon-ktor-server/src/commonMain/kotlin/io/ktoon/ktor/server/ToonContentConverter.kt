package io.ktoon.ktor.server

import io.ktoon.Toon
import io.ktor.http.ContentType
import io.ktor.http.content.OutgoingContent
import io.ktor.http.withCharset
import io.ktor.serialization.ContentConverter
import io.ktor.util.reflect.TypeInfo
import io.ktor.utils.io.ByteReadChannel
import io.ktor.utils.io.charsets.Charset
import io.ktor.utils.io.core.readText
import io.ktor.utils.io.readRemaining
import kotlinx.serialization.SerializationException
import kotlinx.serialization.serializer

internal class ToonContentConverter(
    private val toon: Toon = Toon()
) : ContentConverter {
    
    override suspend fun serialize(
        contentType: ContentType,
        charset: Charset,
        typeInfo: TypeInfo,
        value: Any?
    ): OutgoingContent? {
        if (value == null) return null
        
        return try {
            val serializer = toon.serializersModule.serializer(typeInfo.kotlinType!!)
            val toonString = toon.encodeToString(serializer, value)
            val bytes = toonString.toByteArray(Charsets.UTF_8)
            
            io.ktor.http.content.ByteArrayContent(
                bytes,
                contentType.withCharset(Charsets.UTF_8)
            )
        } catch (e: SerializationException) {
            throw e
        }
    }
    
    override suspend fun deserialize(
        charset: Charset,
        typeInfo: TypeInfo,
        content: ByteReadChannel
    ): Any? {
        return try {
            val contentString = content.readRemaining().readText(charset = charset)
            
            if (contentString.isEmpty()) {
                if (typeInfo.kotlinType?.isMarkedNullable == true) {
                    return null
                }
            }
            
            val deserializer = toon.serializersModule.serializer(typeInfo.kotlinType!!)
            toon.decodeFromString(deserializer, contentString)
        } catch (e: SerializationException) {
            throw e
        }
    }
}
