package io.ktoon

import kotlinx.serialization.Serializable
import kotlinx.serialization.serializer
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class ToonTest {
    
    @Serializable
    data class SimpleData(val id: Int, val name: String)
    
    @Test
    fun testEncodeToStringWithSimpleDataClass() {
        val data = SimpleData(id = 1, name = "Alice")
        val result = Toon.encodeToString(serializer(), data)
        
        assertNotNull(result)
    }
    
    @Test
    fun testDecodeFromStringWithValidToonString() {
        val toonString = """
            id: 1
            name: Alice
        """.trimIndent()
        val result = Toon.decodeFromString(serializer<SimpleData>(), toonString)
        
        assertNotNull(result)
        assertEquals(1, result.id)
        assertEquals("Alice", result.name)
    }
    
    @Test
    fun testRoundTripWithBasicObject() {
        val original = SimpleData(id = 42, name = "Bob")
        val encoded = Toon.encodeToString(serializer(), original)
        val decoded = Toon.decodeFromString(serializer<SimpleData>(), encoded)
        
        assertNotNull(encoded)
        assertNotNull(decoded)
        assertEquals(original.id, decoded.id)
        assertEquals(original.name, decoded.name)
    }
}
