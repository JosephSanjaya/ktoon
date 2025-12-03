package io.ktoon

import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerializationStrategy
import kotlinx.serialization.StringFormat
import kotlinx.serialization.modules.EmptySerializersModule
import kotlinx.serialization.modules.SerializersModule

@OptIn(ExperimentalSerializationApi::class)
class Toon(override val serializersModule: SerializersModule = EmptySerializersModule()) : StringFormat {
    
    override fun <T> encodeToString(serializer: SerializationStrategy<T>, value: T): String {
        val encoder = ToonEncoder(serializersModule)
        encoder.encodeSerializableValue(serializer, value)
        return encoder.toString()
    }
    
    override fun <T> decodeFromString(deserializer: DeserializationStrategy<T>, string: String): T {
        val decoder = ToonDecoder(string, serializersModule)
        return decoder.decodeSerializableValue(deserializer)
    }
    
    companion object Default : StringFormat {
        override val serializersModule: SerializersModule = EmptySerializersModule()
        
        override fun <T> encodeToString(serializer: SerializationStrategy<T>, value: T): String {
            val encoder = ToonEncoder(serializersModule)
            encoder.encodeSerializableValue(serializer, value)
            return encoder.toString()
        }
        
        override fun <T> decodeFromString(deserializer: DeserializationStrategy<T>, string: String): T {
            val decoder = ToonDecoder(string, serializersModule)
            return decoder.decodeSerializableValue(deserializer)
        }
    }
}
