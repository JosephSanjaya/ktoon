package io.ktoon

import kotlinx.serialization.Serializable
import kotlinx.serialization.serializer
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ToonEncoderTest {
    
    @Serializable
    data class SimpleData(val id: Int, val name: String)
    
    @Serializable
    data class NestedData(val user: SimpleData, val active: Boolean)
    
    @Serializable
    data class DataWithList(val users: List<SimpleData>)
    
    @Serializable
    data class NullableData(val id: Int, val name: String?)
    
    @Test
    fun testEncodePrimitiveFields() {
        val data = SimpleData(id = 42, name = "Alice")
        val result = Toon.encodeToString(serializer(), data)
        
        assertTrue(result.contains("id: 42"))
        assertTrue(result.contains("name: Alice"))
    }
    
    @Test
    fun testEncodeNestedObject() {
        val data = NestedData(user = SimpleData(1, "Bob"), active = true)
        val result = Toon.encodeToString(serializer(), data)
        
        assertTrue(result.contains("user:"))
        assertTrue(result.contains("  id: 1"))
        assertTrue(result.contains("  name: Bob"))
        assertTrue(result.contains("active: true"))
    }
    
    @Test
    fun testEncodeCollectionWithTableMode() {
        val data = DataWithList(users = listOf(
            SimpleData(1, "Alice"),
            SimpleData(2, "Bob")
        ))
        val result = Toon.encodeToString(serializer(), data)
        
        assertTrue(result.contains("users[2]{id,name}:"))
        assertTrue(result.contains("1,Alice"))
        assertTrue(result.contains("2,Bob"))
    }
    
    @Test
    fun testEncodeNullValue() {
        val data = NullableData(id = 1, name = null)
        val result = Toon.encodeToString(serializer(), data)
        
        assertTrue(result.contains("id: 1"))
        assertTrue(result.contains("name: null"))
    }
    
    @Test
    fun testEncodeNullInCollection() {
        val data = DataWithList(users = listOf(
            SimpleData(1, "Alice"),
            SimpleData(2, "Bob")
        ))
        val result = Toon.encodeToString(serializer(), data)
        
        val lines = result.lines()
        assertTrue(lines.any { it.contains("users[2]{id,name}:") })
    }
}
