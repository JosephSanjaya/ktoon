package io.ktoon

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.serializer
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ToonAnnotationTest {
    
    @Serializable
    data class DataWithSerialName(
        @SerialName("user_id")
        val userId: Int,
        @SerialName("user_name")
        val userName: String
    )
    
    @Serializable
    data class DataWithTransient(
        val id: Int,
        val name: String,
        @kotlinx.serialization.Transient
        val tempValue: String = "ignored"
    )
    
    @Test
    fun testSerialNameInEncoding() {
        val data = DataWithSerialName(userId = 123, userName = "Alice")
        val encoded = Toon.encodeToString(serializer(), data)
        
        assertTrue(encoded.contains("user_id:"), "Encoded output should contain custom name 'user_id'")
        assertTrue(encoded.contains("user_name:"), "Encoded output should contain custom name 'user_name'")
        assertTrue(!encoded.contains("userId:"), "Encoded output should not contain original property name 'userId'")
        assertTrue(!encoded.contains("userName:"), "Encoded output should not contain original property name 'userName'")
    }
    
    @Test
    fun testSerialNameInDecoding() {
        val toonString = """
            user_id: 456
            user_name: Bob
        """.trimIndent()
        
        val decoded = Toon.decodeFromString(serializer<DataWithSerialName>(), toonString)
        
        assertEquals(456, decoded.userId)
        assertEquals("Bob", decoded.userName)
    }
    
    @Test
    fun testSerialNameRoundTrip() {
        val original = DataWithSerialName(userId = 789, userName = "Charlie")
        val encoded = Toon.encodeToString(serializer(), original)
        val decoded = Toon.decodeFromString(serializer<DataWithSerialName>(), encoded)
        
        assertEquals(original.userId, decoded.userId)
        assertEquals(original.userName, decoded.userName)
    }
    
    @Test
    fun testSerialNameInCollections() {
        @Serializable
        data class Item(
            @SerialName("item_id")
            val itemId: Int,
            @SerialName("item_name")
            val itemName: String
        )
        
        val items = listOf(
            Item(itemId = 1, itemName = "First"),
            Item(itemId = 2, itemName = "Second")
        )
        
        val encoded = Toon.encodeToString(serializer<List<Item>>(), items)
        
        assertTrue(encoded.contains("item_id"), "Table header should contain custom name 'item_id'")
        assertTrue(encoded.contains("item_name"), "Table header should contain custom name 'item_name'")
        
        val decoded = Toon.decodeFromString(serializer<List<Item>>(), encoded)
        
        assertEquals(2, decoded.size)
        assertEquals(1, decoded[0].itemId)
        assertEquals("First", decoded[0].itemName)
        assertEquals(2, decoded[1].itemId)
        assertEquals("Second", decoded[1].itemName)
    }
    
    @Test
    fun testTransientFieldExcludedFromEncoding() {
        val data = DataWithTransient(id = 1, name = "Test", tempValue = "should not appear")
        val encoded = Toon.encodeToString(serializer(), data)
        
        assertTrue(encoded.contains("id:"), "Encoded output should contain 'id'")
        assertTrue(encoded.contains("name:"), "Encoded output should contain 'name'")
        assertTrue(!encoded.contains("tempValue"), "Encoded output should not contain transient field 'tempValue'")
        assertTrue(!encoded.contains("should not appear"), "Encoded output should not contain transient field value")
    }
    
    @Test
    fun testTransientFieldHandledDuringDecoding() {
        val toonString = """
            id: 42
            name: Alice
        """.trimIndent()
        
        val decoded = Toon.decodeFromString(serializer<DataWithTransient>(), toonString)
        
        assertEquals(42, decoded.id)
        assertEquals("Alice", decoded.name)
        assertEquals("ignored", decoded.tempValue)
    }
    
    @Test
    fun testTransientFieldRoundTrip() {
        val original = DataWithTransient(id = 99, name = "Bob", tempValue = "this will be lost")
        val encoded = Toon.encodeToString(serializer(), original)
        val decoded = Toon.decodeFromString(serializer<DataWithTransient>(), encoded)
        
        assertEquals(original.id, decoded.id)
        assertEquals(original.name, decoded.name)
        assertEquals("ignored", decoded.tempValue)
    }
    
    @Test
    fun testTransientInCollections() {
        @Serializable
        data class ItemWithTransient(
            val id: Int,
            val name: String,
            @kotlinx.serialization.Transient
            val internal: String = "default"
        )
        
        val items = listOf(
            ItemWithTransient(id = 1, name = "First", internal = "temp1"),
            ItemWithTransient(id = 2, name = "Second", internal = "temp2")
        )
        
        val encoded = Toon.encodeToString(serializer<List<ItemWithTransient>>(), items)
        
        assertTrue(encoded.contains("items[2]{id,name}:"), "Table header should only contain non-transient fields")
        assertTrue(!encoded.contains("internal"), "Table header should not contain transient field")
        
        val decoded = Toon.decodeFromString(serializer<List<ItemWithTransient>>(), encoded)
        
        assertEquals(2, decoded.size)
        assertEquals(1, decoded[0].id)
        assertEquals("First", decoded[0].name)
        assertEquals("default", decoded[0].internal)
        assertEquals(2, decoded[1].id)
        assertEquals("Second", decoded[1].name)
        assertEquals("default", decoded[1].internal)
    }
}
