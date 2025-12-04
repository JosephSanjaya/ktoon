package io.ktoon.ktor

import io.ktoon.Toon
import io.ktor.http.ContentType
import io.ktor.util.reflect.typeInfo
import io.ktor.utils.io.ByteReadChannel
import io.ktor.utils.io.charsets.Charsets
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import kotlinx.serialization.modules.subclass
import kotlinx.serialization.serializer
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class ToonContentConverterPolymorphicTest {
    
    @Serializable
    sealed class Animal {
        abstract val name: String
    }
    
    @Serializable
    @SerialName("dog")
    data class Dog(
        override val name: String,
        val breed: String
    ) : Animal()
    
    @Serializable
    @SerialName("cat")
    data class Cat(
        override val name: String,
        val indoor: Boolean
    ) : Animal()
    
    @Serializable
    data class Zoo(
        val animals: List<Animal>
    )
    
    @Test
    fun testPolymorphicSerialization() = runTest {
        val module = SerializersModule {
            polymorphic(Animal::class) {
                subclass(Dog::class)
                subclass(Cat::class)
            }
        }
        
        val toon = Toon(module)
        val converter = ToonContentConverter(toon)
        val dog: Animal = Dog(name = "Buddy", breed = "Golden Retriever")
        
        val serialized = converter.serialize(
            contentType = ContentType.parse("application/toon"),
            charset = Charsets.UTF_8,
            typeInfo = typeInfo<Animal>(),
            value = dog
        )
        
        assertNotNull(serialized)
        val content = (serialized as io.ktor.http.content.ByteArrayContent).bytes()
        val toonString = content.decodeToString()
        
        assertTrue(toonString.contains("type:"), "Should contain type discriminator")
        assertTrue(toonString.contains("dog"), "Should contain type value 'dog'")
        assertTrue(toonString.contains("name:"), "Should contain 'name' field")
        assertTrue(toonString.contains("Buddy"), "Should contain name value")
        assertTrue(toonString.contains("breed:"), "Should contain 'breed' field")
        assertTrue(toonString.contains("Golden Retriever"), "Should contain breed value")
    }
    
    @Test
    fun testPolymorphicRoundTrip() = runTest {
        val module = SerializersModule {
            polymorphic(Animal::class) {
                subclass(Dog::class)
                subclass(Cat::class)
            }
        }
        
        val toon = Toon(module)
        val converter = ToonContentConverter(toon)
        val cat: Animal = Cat(name = "Whiskers", indoor = true)
        
        val serialized = converter.serialize(
            contentType = ContentType.parse("application/toon"),
            charset = Charsets.UTF_8,
            typeInfo = typeInfo<Animal>(),
            value = cat
        )
        
        assertNotNull(serialized)
        val content = (serialized as io.ktor.http.content.ByteArrayContent).bytes()
        val channel = ByteReadChannel(content)
        
        val deserialized = converter.deserialize(
            charset = Charsets.UTF_8,
            typeInfo = typeInfo<Animal>(),
            content = channel
        ) as Animal
        
        assertTrue(deserialized is Cat, "Deserialized object should be a Cat")
        assertEquals("Whiskers", deserialized.name)
        assertEquals(true, (deserialized as Cat).indoor)
    }
    
    @Test
    fun testPolymorphicTypePreservation() = runTest {
        val module = SerializersModule {
            polymorphic(Animal::class) {
                subclass(Dog::class)
                subclass(Cat::class)
            }
        }
        
        val toon = Toon(module)
        val converter = ToonContentConverter(toon)
        
        val dog: Animal = Dog(name = "Max", breed = "Labrador")
        
        val serialized = converter.serialize(
            contentType = ContentType.parse("application/toon"),
            charset = Charsets.UTF_8,
            typeInfo = typeInfo<Animal>(),
            value = dog
        )
        
        assertNotNull(serialized)
        val content = (serialized as io.ktor.http.content.ByteArrayContent).bytes()
        val channel = ByteReadChannel(content)
        
        val deserialized = converter.deserialize(
            charset = Charsets.UTF_8,
            typeInfo = typeInfo<Animal>(),
            content = channel
        ) as Animal
        
        assertTrue(deserialized is Dog, "Concrete type should be preserved as Dog")
        assertEquals("Max", deserialized.name)
        assertEquals("Labrador", (deserialized as Dog).breed)
    }
    
    @Test
    fun testPolymorphicDelegationMatchesDirectEncoding() = runTest {
        val module = SerializersModule {
            polymorphic(Animal::class) {
                subclass(Dog::class)
                subclass(Cat::class)
            }
        }
        
        val toon = Toon(module)
        val converter = ToonContentConverter(toon)
        val dog: Animal = Dog(name = "Buddy", breed = "Golden Retriever")
        
        val directEncoding = toon.encodeToString(serializer<Animal>(), dog)
        
        val serialized = converter.serialize(
            contentType = ContentType.parse("application/toon"),
            charset = Charsets.UTF_8,
            typeInfo = typeInfo<Animal>(),
            value = dog
        )
        
        assertNotNull(serialized)
        val content = (serialized as io.ktor.http.content.ByteArrayContent).bytes()
        val converterEncoding = content.decodeToString()
        
        assertEquals(directEncoding, converterEncoding, "ContentConverter should produce same output as direct Toon.encodeToString for polymorphic types")
    }
}
