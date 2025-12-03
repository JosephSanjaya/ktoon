package io.ktoon

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.contextual
import kotlinx.serialization.serializer
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ToonCustomSerializerTest {
    
    // Custom type that needs a custom serializer
    data class CustomDate(val year: Int, val month: Int, val day: Int) {
        override fun toString(): String = "$year-$month-$day"
        
        companion object {
            fun parse(s: String): CustomDate {
                val parts = s.trim().split("-")
                require(parts.size == 3) { "Invalid date format: $s" }
                return CustomDate(parts[0].toInt(), parts[1].toInt(), parts[2].toInt())
            }
        }
    }
    
    // Custom serializer for CustomDate
    object CustomDateSerializer : KSerializer<CustomDate> {
        override val descriptor: SerialDescriptor = 
            PrimitiveSerialDescriptor("CustomDate", PrimitiveKind.STRING)
        
        override fun serialize(encoder: Encoder, value: CustomDate) {
            encoder.encodeString(value.toString())
        }
        
        override fun deserialize(decoder: Decoder): CustomDate {
            return CustomDate.parse(decoder.decodeString())
        }
    }
    
    @Serializable
    data class Event(
        val name: String,
        @kotlinx.serialization.Contextual
        val date: CustomDate
    )
    
    @Test
    fun testCustomSerializerInEncoding() {
        val module = SerializersModule {
            contextual(CustomDateSerializer)
        }
        
        val toon = Toon(module)
        val event = Event(name = "Birthday", date = CustomDate(2024, 12, 25))
        
        val encoded = toon.encodeToString(serializer(), event)
        
        assertTrue(encoded.contains("name:"), "Encoded output should contain 'name' field")
        assertTrue(encoded.contains("Birthday"), "Encoded output should contain name value")
        assertTrue(encoded.contains("date:"), "Encoded output should contain 'date' field")
        assertTrue(encoded.contains("2024-12-25"), "Encoded output should contain custom serialized date")
    }
    
    @Test
    fun testCustomSerializerInDecoding() {
        val module = SerializersModule {
            contextual(CustomDateSerializer)
        }
        
        val toon = Toon(module)
        val toonString = """
            name: Birthday
            date: 2024-12-25
        """.trimIndent()
        
        val decoded = toon.decodeFromString(serializer<Event>(), toonString)
        
        assertEquals("Birthday", decoded.name)
        assertEquals(2024, decoded.date.year)
        assertEquals(12, decoded.date.month)
        assertEquals(25, decoded.date.day)
    }
    
    @Test
    fun testCustomSerializerRoundTrip() {
        val module = SerializersModule {
            contextual(CustomDateSerializer)
        }
        
        val toon = Toon(module)
        val original = Event(name = "NewYear", date = CustomDate(2025, 1, 1))
        
        val encoded = toon.encodeToString(serializer(), original)
        val decoded = toon.decodeFromString(serializer<Event>(), encoded)
        
        assertEquals(original.name, decoded.name)
        assertEquals(original.date.year, decoded.date.year)
        assertEquals(original.date.month, decoded.date.month)
        assertEquals(original.date.day, decoded.date.day)
    }
    
    @Test
    fun testCustomSerializerInCollections() {
        val module = SerializersModule {
            contextual(CustomDateSerializer)
        }
        
        val toon = Toon(module)
        val events = listOf(
            Event(name = "Birthday", date = CustomDate(2024, 12, 25)),
            Event(name = "New Year", date = CustomDate(2025, 1, 1))
        )
        
        val encoded = toon.encodeToString(serializer<List<Event>>(), events)
        val decoded = toon.decodeFromString(serializer<List<Event>>(), encoded)
        
        assertEquals(2, decoded.size)
        assertEquals("Birthday", decoded[0].name)
        assertEquals(2024, decoded[0].date.year)
        assertEquals("New Year", decoded[1].name)
        assertEquals(2025, decoded[1].date.year)
    }
}
