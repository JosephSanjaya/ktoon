package io.ktoon.ktor.server

import io.ktoon.Toon
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

class ToonContentConverterDeserializeTest {
    
    @Serializable
    data class User(val id: Int, val name: String)
    
    @Serializable
    data class NullableUser(val id: Int, val name: String?)
    
    private val converter = ToonContentConverter(Toon())
    
    @Test
    fun testDeserializeValidToonFormat() = runTest {
        val toonContent = """
            id: 123
            name: Alice
        """.trimIndent()
        
        val channel = ByteReadChannel(toonContent.toByteArray(Charsets.UTF_8))
        
        val result = converter.deserialize(
            charset = Charsets.UTF_8,
            typeInfo = typeInfo<User>(),
            content = channel
        ) as User
        
        assertEquals(123, result.id)
        assertEquals("Alice", result.name)
    }
    
    @Test
    fun testDeserializeEmptyBodyWithNullableType() = runTest {
        val channel = ByteReadChannel(ByteArray(0))
        
        val result = converter.deserialize(
            charset = Charsets.UTF_8,
            typeInfo = typeInfo<User?>(),
            content = channel
        )
        
        assertNull(result)
    }
    
    @Test
    fun testDeserializeInvalidToonFormatThrowsException() = runTest {
        val invalidToonContent = "invalid: toon: format:"
        val channel = ByteReadChannel(invalidToonContent.toByteArray(Charsets.UTF_8))
        
        assertFailsWith<SerializationException> {
            converter.deserialize(
                charset = Charsets.UTF_8,
                typeInfo = typeInfo<User>(),
                content = channel
            )
        }
    }
    
    @Test
    fun testDeserializeWithDifferentCharset() = runTest {
        val toonContent = """
            id: 456
            name: José
        """.trimIndent()
        
        val channel = ByteReadChannel(toonContent.toByteArray(Charsets.UTF_8))
        
        val result = converter.deserialize(
            charset = Charsets.UTF_8,
            typeInfo = typeInfo<User>(),
            content = channel
        ) as User
        
        assertEquals(456, result.id)
        assertEquals("José", result.name)
    }
    
    @Test
    fun testDeserializeNullFieldInToonFormat() = runTest {
        val toonContent = """
            id: 789
            name: null
        """.trimIndent()
        
        val channel = ByteReadChannel(toonContent.toByteArray(Charsets.UTF_8))
        
        val result = converter.deserialize(
            charset = Charsets.UTF_8,
            typeInfo = typeInfo<NullableUser>(),
            content = channel
        ) as NullableUser
        
        assertEquals(789, result.id)
        assertNull(result.name)
    }
    
    @Test
    fun testDeserializeCollectionInTableMode() = runTest {
        val toonContent = """
            users[2]{id,name}:
              123,Alice
              456,Bob
        """.trimIndent()
        
        val channel = ByteReadChannel(toonContent.toByteArray(Charsets.UTF_8))
        
        val result = converter.deserialize(
            charset = Charsets.UTF_8,
            typeInfo = typeInfo<List<User>>(),
            content = channel
        ) as List<User>
        
        assertEquals(2, result.size)
        assertEquals(123, result[0].id)
        assertEquals("Alice", result[0].name)
        assertEquals(456, result[1].id)
        assertEquals("Bob", result[1].name)
    }
}
