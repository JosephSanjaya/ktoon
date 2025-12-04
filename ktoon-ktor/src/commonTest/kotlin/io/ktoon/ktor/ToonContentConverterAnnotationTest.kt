package io.ktoon.ktor

import io.ktoon.Toon
import io.ktor.http.ContentType
import io.ktor.util.reflect.TypeInfo
import io.ktor.util.reflect.typeInfo
import io.ktor.utils.io.ByteReadChannel
import io.ktor.utils.io.charsets.Charsets
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.Serializable
import kotlinx.serialization.serializer
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class ToonContentConverterAnnotationTest {
    
    @Serializable
    data class SimpleUser(
        val id: Int,
        val name: String
    )
    
    @Serializable
    data class ComplexData(
        val id: Int,
        val name: String,
        val active: Boolean,
        val score: Double
    )
    
    @Serializable
    data class NestedData(
        val user: SimpleUser,
        val timestamp: Long
    )
    
    @Test
    fun testSerializableAnnotationWithSimpleClass() = runTest {
        val converter = ToonContentConverter(Toon())
        val user = SimpleUser(id = 1, name = "Alice")
        
        val serialized = converter.serialize(
            contentType = ContentType.parse("application/toon"),
            charset = Charsets.UTF_8,
            typeInfo = typeInfo<SimpleUser>(),
            value = user
        )
        
        assertNotNull(serialized, "Serialization should produce non-null result")
    }
    
    @Test
    fun testSerializableAnnotationRoundTrip() = runTest {
        val converter = ToonContentConverter(Toon())
        val original = SimpleUser(id = 42, name = "Bob")
        
        val serialized = converter.serialize(
            contentType = ContentType.parse("application/toon"),
            charset = Charsets.UTF_8,
            typeInfo = typeInfo<SimpleUser>(),
            value = original
        )
        
        assertNotNull(serialized)
        
        val content = (serialized as io.ktor.http.content.ByteArrayContent).bytes()
        val channel = ByteReadChannel(content)
        
        val deserialized = converter.deserialize(
            charset = Charsets.UTF_8,
            typeInfo = typeInfo<SimpleUser>(),
            content = channel
        ) as SimpleUser
        
        assertEquals(original.id, deserialized.id)
        assertEquals(original.name, deserialized.name)
    }
    
    @Test
    fun testSerializableWithMultipleTypes() = runTest {
        val converter = ToonContentConverter(Toon())
        val data = ComplexData(
            id = 100,
            name = "Test",
            active = true,
            score = 95.5
        )
        
        val serialized = converter.serialize(
            contentType = ContentType.parse("application/toon"),
            charset = Charsets.UTF_8,
            typeInfo = typeInfo<ComplexData>(),
            value = data
        )
        
        assertNotNull(serialized)
        
        val content = (serialized as io.ktor.http.content.ByteArrayContent).bytes()
        val channel = ByteReadChannel(content)
        
        val deserialized = converter.deserialize(
            charset = Charsets.UTF_8,
            typeInfo = typeInfo<ComplexData>(),
            content = channel
        ) as ComplexData
        
        assertEquals(data.id, deserialized.id)
        assertEquals(data.name, deserialized.name)
        assertEquals(data.active, deserialized.active)
        assertEquals(data.score, deserialized.score)
    }
    
    @Test
    fun testSerializableWithNestedObjects() = runTest {
        val converter = ToonContentConverter(Toon())
        val nested = NestedData(
            user = SimpleUser(id = 5, name = "Charlie"),
            timestamp = 1234567890L
        )
        
        val serialized = converter.serialize(
            contentType = ContentType.parse("application/toon"),
            charset = Charsets.UTF_8,
            typeInfo = typeInfo<NestedData>(),
            value = nested
        )
        
        assertNotNull(serialized)
        
        val content = (serialized as io.ktor.http.content.ByteArrayContent).bytes()
        val channel = ByteReadChannel(content)
        
        val deserialized = converter.deserialize(
            charset = Charsets.UTF_8,
            typeInfo = typeInfo<NestedData>(),
            content = channel
        ) as NestedData
        
        assertEquals(nested.user.id, deserialized.user.id)
        assertEquals(nested.user.name, deserialized.user.name)
        assertEquals(nested.timestamp, deserialized.timestamp)
    }
    
    @Test
    fun testSerializableWithCollections() = runTest {
        val converter = ToonContentConverter(Toon())
        val users = listOf(
            SimpleUser(id = 1, name = "Alice"),
            SimpleUser(id = 2, name = "Bob"),
            SimpleUser(id = 3, name = "Charlie")
        )
        
        val serialized = converter.serialize(
            contentType = ContentType.parse("application/toon"),
            charset = Charsets.UTF_8,
            typeInfo = typeInfo<List<SimpleUser>>(),
            value = users
        )
        
        assertNotNull(serialized)
        
        val content = (serialized as io.ktor.http.content.ByteArrayContent).bytes()
        val channel = ByteReadChannel(content)
        
        val deserialized = converter.deserialize(
            charset = Charsets.UTF_8,
            typeInfo = typeInfo<List<SimpleUser>>(),
            content = channel
        ) as List<SimpleUser>
        
        assertEquals(users.size, deserialized.size)
        assertEquals(users[0].id, deserialized[0].id)
        assertEquals(users[0].name, deserialized[0].name)
        assertEquals(users[2].id, deserialized[2].id)
        assertEquals(users[2].name, deserialized[2].name)
    }
    
    @Test
    fun testDelegationToToonFormat() = runTest {
        val toon = Toon()
        val converter = ToonContentConverter(toon)
        val user = SimpleUser(id = 99, name = "Direct")
        
        val directEncoding = toon.encodeToString(serializer<SimpleUser>(), user)
        
        val serialized = converter.serialize(
            contentType = ContentType.parse("application/toon"),
            charset = Charsets.UTF_8,
            typeInfo = typeInfo<SimpleUser>(),
            value = user
        )
        
        assertNotNull(serialized)
        val content = (serialized as io.ktor.http.content.ByteArrayContent).bytes()
        val converterEncoding = content.decodeToString()
        
        assertEquals(directEncoding, converterEncoding, "ContentConverter should produce same output as direct Toon.encodeToString")
    }
}
