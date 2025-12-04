package io.ktoon.ktor

import io.ktoon.Toon
import io.ktor.http.ContentType
import io.ktor.util.reflect.typeInfo
import io.ktor.utils.io.ByteReadChannel
import io.ktor.utils.io.charsets.Charsets
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import kotlinx.serialization.serializer
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class ToonContentConverterSerialNameTest {
    
    @Serializable
    data class UserWithSerialName(
        @SerialName("user_id")
        val userId: Int,
        @SerialName("user_name")
        val userName: String
    )
    
    @Serializable
    data class DataWithTransient(
        val id: Int,
        val name: String,
        @Transient
        val tempValue: String = "default"
    )
    
    @Test
    fun testSerialNameAnnotationInSerialization() = runTest {
        val converter = ToonContentConverter(Toon())
        val user = UserWithSerialName(userId = 123, userName = "Alice")
        
        val serialized = converter.serialize(
            contentType = ContentType.parse("application/toon"),
            charset = Charsets.UTF_8,
            typeInfo = typeInfo<UserWithSerialName>(),
            value = user
        )
        
        assertNotNull(serialized)
        val content = (serialized as io.ktor.http.content.ByteArrayContent).bytes()
        val toonString = content.decodeToString()
        
        assertTrue(toonString.contains("user_id"), "Should contain custom field name 'user_id'")
        assertTrue(toonString.contains("user_name"), "Should contain custom field name 'user_name'")
        assertFalse(toonString.contains("userId"), "Should not contain original field name 'userId'")
        assertFalse(toonString.contains("userName"), "Should not contain original field name 'userName'")
    }
    
    @Test
    fun testSerialNameAnnotationRoundTrip() = runTest {
        val converter = ToonContentConverter(Toon())
        val original = UserWithSerialName(userId = 456, userName = "Bob")
        
        val serialized = converter.serialize(
            contentType = ContentType.parse("application/toon"),
            charset = Charsets.UTF_8,
            typeInfo = typeInfo<UserWithSerialName>(),
            value = original
        )
        
        assertNotNull(serialized)
        val content = (serialized as io.ktor.http.content.ByteArrayContent).bytes()
        val channel = ByteReadChannel(content)
        
        val deserialized = converter.deserialize(
            charset = Charsets.UTF_8,
            typeInfo = typeInfo<UserWithSerialName>(),
            content = channel
        ) as UserWithSerialName
        
        assertEquals(original.userId, deserialized.userId)
        assertEquals(original.userName, deserialized.userName)
    }
    
    @Test
    fun testSerialNameMatchesDirectToonEncoding() = runTest {
        val toon = Toon()
        val converter = ToonContentConverter(toon)
        val user = UserWithSerialName(userId = 789, userName = "Charlie")
        
        val directEncoding = toon.encodeToString(serializer<UserWithSerialName>(), user)
        
        val serialized = converter.serialize(
            contentType = ContentType.parse("application/toon"),
            charset = Charsets.UTF_8,
            typeInfo = typeInfo<UserWithSerialName>(),
            value = user
        )
        
        assertNotNull(serialized)
        val content = (serialized as io.ktor.http.content.ByteArrayContent).bytes()
        val converterEncoding = content.decodeToString()
        
        assertEquals(directEncoding, converterEncoding, "ContentConverter should produce same output as direct Toon.encodeToString for @SerialName")
    }
    
    @Test
    fun testTransientAnnotationInSerialization() = runTest {
        val converter = ToonContentConverter(Toon())
        val data = DataWithTransient(id = 1, name = "Test", tempValue = "should not appear")
        
        val serialized = converter.serialize(
            contentType = ContentType.parse("application/toon"),
            charset = Charsets.UTF_8,
            typeInfo = typeInfo<DataWithTransient>(),
            value = data
        )
        
        assertNotNull(serialized)
        val content = (serialized as io.ktor.http.content.ByteArrayContent).bytes()
        val toonString = content.decodeToString()
        
        assertTrue(toonString.contains("id"), "Should contain 'id' field")
        assertTrue(toonString.contains("name"), "Should contain 'name' field")
        assertFalse(toonString.contains("tempValue"), "Should not contain transient field 'tempValue'")
        assertFalse(toonString.contains("should not appear"), "Should not contain transient field value")
    }
    
    @Test
    fun testTransientAnnotationRoundTrip() = runTest {
        val converter = ToonContentConverter(Toon())
        val original = DataWithTransient(id = 42, name = "Alice", tempValue = "this will be lost")
        
        val serialized = converter.serialize(
            contentType = ContentType.parse("application/toon"),
            charset = Charsets.UTF_8,
            typeInfo = typeInfo<DataWithTransient>(),
            value = original
        )
        
        assertNotNull(serialized)
        val content = (serialized as io.ktor.http.content.ByteArrayContent).bytes()
        val channel = ByteReadChannel(content)
        
        val deserialized = converter.deserialize(
            charset = Charsets.UTF_8,
            typeInfo = typeInfo<DataWithTransient>(),
            content = channel
        ) as DataWithTransient
        
        assertEquals(original.id, deserialized.id)
        assertEquals(original.name, deserialized.name)
        assertEquals("default", deserialized.tempValue, "Transient field should have default value")
    }
    
    @Test
    fun testTransientMatchesDirectToonEncoding() = runTest {
        val toon = Toon()
        val converter = ToonContentConverter(toon)
        val data = DataWithTransient(id = 99, name = "Bob", tempValue = "ignored")
        
        val directEncoding = toon.encodeToString(serializer<DataWithTransient>(), data)
        
        val serialized = converter.serialize(
            contentType = ContentType.parse("application/toon"),
            charset = Charsets.UTF_8,
            typeInfo = typeInfo<DataWithTransient>(),
            value = data
        )
        
        assertNotNull(serialized)
        val content = (serialized as io.ktor.http.content.ByteArrayContent).bytes()
        val converterEncoding = content.decodeToString()
        
        assertEquals(directEncoding, converterEncoding, "ContentConverter should produce same output as direct Toon.encodeToString for @Transient")
    }
    
    @Test
    fun testSerialNameInCollections() = runTest {
        @Serializable
        data class Item(
            @SerialName("item_id")
            val itemId: Int,
            @SerialName("item_name")
            val itemName: String
        )
        
        val converter = ToonContentConverter(Toon())
        val items = listOf(
            Item(itemId = 1, itemName = "First"),
            Item(itemId = 2, itemName = "Second")
        )
        
        val serialized = converter.serialize(
            contentType = ContentType.parse("application/toon"),
            charset = Charsets.UTF_8,
            typeInfo = typeInfo<List<Item>>(),
            value = items
        )
        
        assertNotNull(serialized)
        val content = (serialized as io.ktor.http.content.ByteArrayContent).bytes()
        val toonString = content.decodeToString()
        
        assertTrue(toonString.contains("item_id"), "Table header should contain custom name 'item_id'")
        assertTrue(toonString.contains("item_name"), "Table header should contain custom name 'item_name'")
        
        val channel = ByteReadChannel(content)
        val deserialized = converter.deserialize(
            charset = Charsets.UTF_8,
            typeInfo = typeInfo<List<Item>>(),
            content = channel
        ) as List<Item>
        
        assertEquals(2, deserialized.size)
        assertEquals(1, deserialized[0].itemId)
        assertEquals("First", deserialized[0].itemName)
    }
    
    @Test
    fun testTransientInCollections() = runTest {
        @Serializable
        data class ItemWithTransient(
            val id: Int,
            val name: String,
            @Transient
            val internal: String = "default"
        )
        
        val converter = ToonContentConverter(Toon())
        val items = listOf(
            ItemWithTransient(id = 1, name = "First", internal = "temp1"),
            ItemWithTransient(id = 2, name = "Second", internal = "temp2")
        )
        
        val serialized = converter.serialize(
            contentType = ContentType.parse("application/toon"),
            charset = Charsets.UTF_8,
            typeInfo = typeInfo<List<ItemWithTransient>>(),
            value = items
        )
        
        assertNotNull(serialized)
        val content = (serialized as io.ktor.http.content.ByteArrayContent).bytes()
        val toonString = content.decodeToString()
        
        assertTrue(toonString.contains("{id,name}"), "Table header should only contain non-transient fields")
        assertFalse(toonString.contains("internal"), "Table header should not contain transient field")
        
        val channel = ByteReadChannel(content)
        val deserialized = converter.deserialize(
            charset = Charsets.UTF_8,
            typeInfo = typeInfo<List<ItemWithTransient>>(),
            content = channel
        ) as List<ItemWithTransient>
        
        assertEquals(2, deserialized.size)
        assertEquals("default", deserialized[0].internal, "Transient field should have default value")
        assertEquals("default", deserialized[1].internal, "Transient field should have default value")
    }
}
