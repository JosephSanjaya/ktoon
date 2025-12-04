package io.ktoon.ktor.server

import io.ktoon.Toon
import io.ktor.http.ContentType
import io.ktor.util.reflect.typeInfo
import io.ktor.utils.io.ByteReadChannel
import io.ktor.utils.io.charsets.Charsets
import kotlinx.coroutines.test.runTest
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
import kotlin.test.assertNotNull

class ToonContentConverterCustomConfigTest {
    
    data class CustomTimestamp(val epochSeconds: Long) {
        override fun toString(): String = "TS_$epochSeconds"
        
        companion object {
            fun parse(s: String): CustomTimestamp {
                require(s.startsWith("TS_")) { "Invalid timestamp format: $s" }
                return CustomTimestamp(s.substring(3).toLong())
            }
        }
    }
    
    object CustomTimestampSerializer : KSerializer<CustomTimestamp> {
        override val descriptor: SerialDescriptor = 
            PrimitiveSerialDescriptor("CustomTimestamp", PrimitiveKind.STRING)
        
        override fun serialize(encoder: Encoder, value: CustomTimestamp) {
            encoder.encodeString(value.toString())
        }
        
        override fun deserialize(decoder: Decoder): CustomTimestamp {
            return CustomTimestamp.parse(decoder.decodeString())
        }
    }
    
    @Serializable
    data class LogEntry(
        val message: String,
        @kotlinx.serialization.Contextual
        val timestamp: CustomTimestamp
    )
    
    @Test
    fun testCustomToonInstanceCanBePassedToConverter() = runTest {
        val customModule = SerializersModule {
            contextual(CustomTimestampSerializer)
        }
        
        val customToon = Toon(customModule)
        val converter = ToonContentConverter(customToon)
        
        val entry = LogEntry(
            message = "SystemStarted",
            timestamp = CustomTimestamp(1234567890L)
        )
        
        val serialized = converter.serialize(
            contentType = ContentType.parse("application/toon"),
            charset = Charsets.UTF_8,
            typeInfo = typeInfo<LogEntry>(),
            value = entry
        )
        
        assertNotNull(serialized, "Custom Toon instance should serialize successfully")
    }
    
    @Test
    fun testCustomInstanceIsUsedByConverter() = runTest {
        val customModule = SerializersModule {
            contextual(CustomTimestampSerializer)
        }
        
        val customToon = Toon(customModule)
        val converter = ToonContentConverter(customToon)
        
        val entry = LogEntry(
            message = "TestEvent",
            timestamp = CustomTimestamp(9876543210L)
        )
        
        val serialized = converter.serialize(
            contentType = ContentType.parse("application/toon"),
            charset = Charsets.UTF_8,
            typeInfo = typeInfo<LogEntry>(),
            value = entry
        )
        
        assertNotNull(serialized)
        val content = (serialized as io.ktor.http.content.ByteArrayContent).bytes()
        val toonString = content.decodeToString()
        
        assert(toonString.contains("TS_9876543210")) {
            "Custom serializer should be used, expected 'TS_9876543210' in output: $toonString"
        }
    }
    
    @Test
    fun testCustomToonInstanceRoundTrip() = runTest {
        val customModule = SerializersModule {
            contextual(CustomTimestampSerializer)
        }
        
        val customToon = Toon(customModule)
        
        val original = LogEntry(
            message = "RoundTripTest",
            timestamp = CustomTimestamp(1609459200L)
        )
        
        val directEncoding = customToon.encodeToString(serializer<LogEntry>(), original)
        assert(directEncoding.contains("TS_")) {
            "Direct encoding should use custom serializer, expected 'TS_' in output: $directEncoding"
        }
        
        val converter = ToonContentConverter(customToon)
        
        val serialized = converter.serialize(
            contentType = ContentType.parse("application/toon"),
            charset = Charsets.UTF_8,
            typeInfo = typeInfo<LogEntry>(),
            value = original
        )
        
        assertNotNull(serialized)
        val content = (serialized as io.ktor.http.content.ByteArrayContent).bytes()
        
        val channel = ByteReadChannel(content)
        
        val deserialized = converter.deserialize(
            charset = Charsets.UTF_8,
            typeInfo = typeInfo<LogEntry>(),
            content = channel
        ) as LogEntry
        
        assertEquals(original.message, deserialized.message)
        assertEquals(original.timestamp.epochSeconds, deserialized.timestamp.epochSeconds)
    }
    
    data class CustomId(val prefix: String, val number: Int) {
        override fun toString(): String = "$prefix$number"
    }
    
    object CustomIdSerializer : KSerializer<CustomId> {
        override val descriptor: SerialDescriptor = 
            PrimitiveSerialDescriptor("CustomId", PrimitiveKind.STRING)
        
        override fun serialize(encoder: Encoder, value: CustomId) {
            encoder.encodeString(value.toString())
        }
        
        override fun deserialize(decoder: Decoder): CustomId {
            val s = decoder.decodeString()
            val prefixEnd = s.indexOfFirst { it.isDigit() }
            require(prefixEnd > 0) { "Invalid ID format: $s" }
            val prefix = s.substring(0, prefixEnd)
            val number = s.substring(prefixEnd).toInt()
            return CustomId(prefix, number)
        }
    }
    
    @Serializable
    data class Record(
        @kotlinx.serialization.Contextual
        val id: CustomId,
        val name: String,
        @kotlinx.serialization.Contextual
        val created: CustomTimestamp
    )
    
    @Test
    fun testCustomSerializersModuleWithMultipleTypes() = runTest {
        val customModule = SerializersModule {
            contextual(CustomTimestampSerializer)
            contextual(CustomIdSerializer)
        }
        
        val customToon = Toon(customModule)
        val converter = ToonContentConverter(customToon)
        
        val record = Record(
            id = CustomId("REC", 42),
            name = "TestRecord",
            created = CustomTimestamp(1234567890L)
        )
        
        val serialized = converter.serialize(
            contentType = ContentType.parse("application/toon"),
            charset = Charsets.UTF_8,
            typeInfo = typeInfo<Record>(),
            value = record
        )
        
        assertNotNull(serialized)
        val content = (serialized as io.ktor.http.content.ByteArrayContent).bytes()
        val channel = ByteReadChannel(content)
        
        val deserialized = converter.deserialize(
            charset = Charsets.UTF_8,
            typeInfo = typeInfo<Record>(),
            content = channel
        ) as Record
        
        assertEquals(record.id.prefix, deserialized.id.prefix)
        assertEquals(record.id.number, deserialized.id.number)
        assertEquals(record.name, deserialized.name)
        assertEquals(record.created.epochSeconds, deserialized.created.epochSeconds)
    }
}
