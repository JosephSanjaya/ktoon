package io.ktoon

import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.serializer
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertFailsWith

class ToonErrorHandlingTest {
    
    @Serializable
    data class SimpleData(val id: Int, val name: String)
    
    @Serializable
    data class WithCollection(val items: List<SimpleData>)
    
    @Test
    fun testDecoderSyntaxErrorWithLineNumber() {
        val invalidToon = """
            id 123
            name: Alice
        """.trimIndent()
        
        val exception = assertFailsWith<SerializationException> {
            Toon.decodeFromString(serializer<SimpleData>(), invalidToon)
        }
        
        assertContains(exception.message ?: "", "line")
        assertContains(exception.message ?: "", ":")
    }
    
    @Test
    fun testDecoderTypeMismatch() {
        val invalidToon = """
            id: notanumber
            name: Alice
        """.trimIndent()
        
        val exception = assertFailsWith<SerializationException> {
            Toon.decodeFromString(serializer<SimpleData>(), invalidToon)
        }
        
        assertContains(exception.message ?: "", "Type mismatch")
        assertContains(exception.message ?: "", "Int")
    }
    
    @Test
    fun testDecoderUnknownField() {
        val invalidToon = """
            id: 123
            unknownField: value
            name: Alice
        """.trimIndent()
        
        val exception = assertFailsWith<SerializationException> {
            Toon.decodeFromString(serializer<SimpleData>(), invalidToon)
        }
        
        assertContains(exception.message ?: "", "Unknown field")
        assertContains(exception.message ?: "", "unknownField")
    }
    
    @Test
    fun testDecoderIndentationError() {
        val invalidToon = """
            id: 123
              name: Alice
        """.trimIndent()
        
        val exception = assertFailsWith<SerializationException> {
            Toon.decodeFromString(serializer<SimpleData>(), invalidToon)
        }
        
        assertContains(exception.message ?: "", "Indentation error")
    }
    
    @Test
    fun testDecoderTableSizeMismatch() {
        val invalidToon = """
            items[3]{id,name}:
              1,Alice
              2,Bob
        """.trimIndent()
        
        // Just verify that an error is thrown - the specific message may vary
        assertFailsWith<SerializationException> {
            Toon.decodeFromString(serializer<WithCollection>(), invalidToon)
        }
    }
    
    @Test
    fun testDecoderRowFieldCountMismatch() {
        val invalidToon = """
            items[2]{id,name}:
              1,Alice
              2,Bob,Extra
        """.trimIndent()
        
        // Just verify that an error is thrown - the specific message may vary
        assertFailsWith<SerializationException> {
            Toon.decodeFromString(serializer<WithCollection>(), invalidToon)
        }
    }
    
    @Test
    fun testDecoderMissingTableHeader() {
        val invalidToon = """
            items:
              1,Alice
              2,Bob
        """.trimIndent()
        
        val exception = assertFailsWith<SerializationException> {
            Toon.decodeFromString(serializer<WithCollection>(), invalidToon)
        }
        
        assertContains(exception.message ?: "", "Expected table header")
    }
    
    @Test
    fun testEncoderUnsupportedType() {
        @Serializable
        data class WithMap(val data: Map<String, String>)
        
        val exception = assertFailsWith<SerializationException> {
            Toon.encodeToString(serializer<WithMap>(), WithMap(mapOf("key" to "value")))
        }
        
        // Check for unsupported type error
        val message = exception.message ?: ""
        val hasUnsupportedError = message.contains("Unsupported") || message.contains("Map") || message.contains("not supported")
        assert(hasUnsupportedError) { "Expected unsupported type error but got: $message" }
    }
}
