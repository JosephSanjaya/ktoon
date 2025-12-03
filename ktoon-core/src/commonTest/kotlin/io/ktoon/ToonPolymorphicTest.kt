package io.ktoon

import kotlinx.serialization.Serializable
import kotlinx.serialization.SerialName
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import kotlinx.serialization.modules.subclass
import kotlinx.serialization.serializer
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ToonPolymorphicTest {
    
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
    fun testPolymorphicSerialization() {
        val module = SerializersModule {
            polymorphic(Animal::class) {
                subclass(Dog::class)
                subclass(Cat::class)
            }
        }
        
        val toon = Toon(module)
        val dog: Animal = Dog(name = "Buddy", breed = "Golden Retriever")
        
        val encoded = toon.encodeToString(serializer<Animal>(), dog)
        
        assertTrue(encoded.contains("type:"), "Encoded output should contain type discriminator")
        assertTrue(encoded.contains("dog"), "Encoded output should contain type value 'dog'")
        assertTrue(encoded.contains("name:"), "Encoded output should contain 'name' field")
        assertTrue(encoded.contains("Buddy"), "Encoded output should contain name value")
        assertTrue(encoded.contains("breed:"), "Encoded output should contain 'breed' field")
        assertTrue(encoded.contains("Golden Retriever"), "Encoded output should contain breed value")
    }
    
    @Test
    fun testPolymorphicRoundTrip() {
        val module = SerializersModule {
            polymorphic(Animal::class) {
                subclass(Dog::class)
                subclass(Cat::class)
            }
        }
        
        val toon = Toon(module)
        val cat: Animal = Cat(name = "Whiskers", indoor = true)
        
        val encoded = toon.encodeToString(serializer<Animal>(), cat)
        val decoded = toon.decodeFromString(serializer<Animal>(), encoded)
        
        assertTrue(decoded is Cat, "Decoded object should be a Cat")
        assertEquals("Whiskers", decoded.name)
        assertEquals(true, (decoded as Cat).indoor)
    }
}
