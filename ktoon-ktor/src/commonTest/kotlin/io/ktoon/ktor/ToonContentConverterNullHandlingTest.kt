package io.ktoon.ktor

import io.ktoon.Toon
import io.ktor.http.ContentType
import io.ktor.util.reflect.TypeInfo
import io.ktor.util.reflect.typeInfo
import io.ktor.utils.io.ByteReadChannel
import io.ktor.utils.io.charsets.Charsets
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

class ToonContentConverterNullHandlingTest {
    
    @Serializable
    data class User(val id: Int, val name: String)
    
    private val converter = ToonContentConverter(Toon())
    
    @Test
    fun testNullRequestBodyReturnsNull() = runTest {
        val result = converter.serialize(
            contentType = ContentType.parse("application/toon"),
            charset = Charsets.UTF_8,
            typeInfo = typeInfo<User>(),
            value = null
        )
        
        assertNull(result, "serialize() should return null for null input")
    }
    
    @Test
    fun testEmptyResponseWithNullableTypeReturnsNull() = runTest {
        val emptyContent = ByteReadChannel("")
        
        val result = converter.deserialize(
            charset = Charsets.UTF_8,
            typeInfo = typeInfo<User?>(),
            content = emptyContent
        )
        
        assertNull(result, "deserialize() should return null for empty response with nullable type")
    }
    
    @Test
    fun testEmptyResponseWithNonNullableTypeThrowsException() = runTest {
        val emptyContent = ByteReadChannel("")
        
        assertFailsWith<SerializationException> {
            converter.deserialize(
                charset = Charsets.UTF_8,
                typeInfo = typeInfo<User>(),
                content = emptyContent
            )
        }
    }
    
    @Test
    fun testNullKeywordInToonFormatDeserializesToNull() = runTest {
        val toonContent = ByteReadChannel("null")
        
        val result = converter.deserialize(
            charset = Charsets.UTF_8,
            typeInfo = typeInfo<User?>(),
            content = toonContent
        )
        
        assertNull(result, "deserialize() should handle 'null' keyword in TOON format")
    }
    
    @Test
    fun testNullableFieldInObject() = runTest {
        @Serializable
        data class UserWithNullableField(val id: Int, val name: String?)
        
        val user = UserWithNullableField(1, null)
        
        val serialized = converter.serialize(
            contentType = ContentType.parse("application/toon"),
            charset = Charsets.UTF_8,
            typeInfo = typeInfo<UserWithNullableField>(),
            value = user
        )
        
        assertNotNull(serialized, "serialize() should handle objects with null fields")
    }
}

private fun assertNotNull(value: Any?, message: String) {
    if (value == null) {
        throw AssertionError(message)
    }
}
