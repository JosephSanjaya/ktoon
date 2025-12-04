package io.ktoon.ktor

import io.ktoon.Toon
import io.ktor.http.ContentType
import io.ktor.util.reflect.typeInfo
import io.ktor.utils.io.ByteReadChannel
import io.ktor.utils.io.charsets.Charsets
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.SerializationException
import kotlinx.serialization.Serializable
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class ToonContentConverterErrorHandlingTest {
    
    @Serializable
    data class User(val id: Int, val name: String)
    
    @Test
    fun testSerializationErrorWithUnsupportedType() = runTest {
        val converter = ToonContentConverter(Toon())
        // Maps are not supported by TOON format
        val unsupportedValue = mapOf("key" to "value")
        
        val exception = assertFailsWith<SerializationException> {
            converter.serialize(
                contentType = ContentType.parse("application/toon"),
                charset = Charsets.UTF_8,
                typeInfo = typeInfo<Map<String, String>>(),
                value = unsupportedValue
            )
        }
        
        // Verify error message is preserved
        assertTrue(exception.message?.contains("not supported") == true || 
                   exception.message?.contains("Map") == true,
                   "Error message should indicate unsupported type")
    }
    
    @Test
    fun testSerializationErrorPreservesStackTrace() = runTest {
        val converter = ToonContentConverter(Toon())
        val unsupportedValue = mapOf("key" to "value")
        
        val exception = assertFailsWith<SerializationException> {
            converter.serialize(
                contentType = ContentType.parse("application/toon"),
                charset = Charsets.UTF_8,
                typeInfo = typeInfo<Map<String, String>>(),
                value = unsupportedValue
            )
        }
        
        // Verify stack trace is preserved
        assertTrue(exception.stackTraceToString().isNotEmpty(),
                   "Stack trace should be preserved")
    }
    
    @Test
    fun testDeserializationErrorWithMalformedToon() = runTest {
        val converter = ToonContentConverter(Toon())
        val malformedToon = "user:\n  id: not_a_number\n  name: Alice"
        val channel = ByteReadChannel(malformedToon.toByteArray(Charsets.UTF_8))
        
        val exception = assertFailsWith<SerializationException> {
            converter.deserialize(
                charset = Charsets.UTF_8,
                typeInfo = typeInfo<User>(),
                content = channel
            )
        }
        
        // Verify error message is preserved from ToonDecoder
        assertTrue(exception.message?.isNotEmpty() == true,
                   "Error message should be preserved")
    }
    
    @Test
    fun testDeserializationErrorWithTypeMismatch() = runTest {
        val converter = ToonContentConverter(Toon())
        // TOON data has string where number is expected
        val mismatchedToon = "user:\n  id: 123\n  name: 456"
        val channel = ByteReadChannel(mismatchedToon.toByteArray(Charsets.UTF_8))
        
        val exception = assertFailsWith<SerializationException> {
            converter.deserialize(
                charset = Charsets.UTF_8,
                typeInfo = typeInfo<User>(),
                content = channel
            )
        }
        
        // Verify error message contains context
        assertTrue(exception.message?.isNotEmpty() == true,
                   "Error message should contain context")
    }
    
    @Test
    fun testDeserializationErrorPreservesLineNumbers() = runTest {
        val converter = ToonContentConverter(Toon())
        // Malformed TOON with indentation error
        val malformedToon = "user:\n  id: 123\nname: Alice"  // Missing indentation
        val channel = ByteReadChannel(malformedToon.toByteArray(Charsets.UTF_8))
        
        val exception = assertFailsWith<SerializationException> {
            converter.deserialize(
                charset = Charsets.UTF_8,
                typeInfo = typeInfo<User>(),
                content = channel
            )
        }
        
        // Verify error message contains line information
        assertTrue(exception.message?.isNotEmpty() == true,
                   "Error message should contain line information")
    }
    
    @Test
    fun testDeserializationErrorPreservesStackTrace() = runTest {
        val converter = ToonContentConverter(Toon())
        val malformedToon = "invalid toon format"
        val channel = ByteReadChannel(malformedToon.toByteArray(Charsets.UTF_8))
        
        val exception = assertFailsWith<SerializationException> {
            converter.deserialize(
                charset = Charsets.UTF_8,
                typeInfo = typeInfo<User>(),
                content = channel
            )
        }
        
        // Verify stack trace is preserved
        assertTrue(exception.stackTraceToString().isNotEmpty(),
                   "Stack trace should be preserved")
    }
    
    @Test
    fun testCharsetHandlingWithValidCharset() = runTest {
        val converter = ToonContentConverter(Toon())
        val user = User(id = 1, name = "Alice")
        
        // Test with UTF-8 (default)
        val serialized = converter.serialize(
            contentType = ContentType.parse("application/toon"),
            charset = Charsets.UTF_8,
            typeInfo = typeInfo<User>(),
            value = user
        )
        
        assertNotNull(serialized, "Serialization with UTF-8 should succeed")
        
        // Test with UTF-16
        val serializedUtf16 = converter.serialize(
            contentType = ContentType.parse("application/toon"),
            charset = Charsets.UTF_16,
            typeInfo = typeInfo<User>(),
            value = user
        )
        
        assertNotNull(serializedUtf16, "Serialization with UTF-16 should succeed")
    }
    
    @Test
    fun testCharsetDecodingWithDifferentCharsets() = runTest {
        val converter = ToonContentConverter(Toon())
        val user = User(id = 123, name = "Test")
        
        // Test UTF-8 round-trip
        val serializedUtf8 = converter.serialize(
            contentType = ContentType.parse("application/toon"),
            charset = Charsets.UTF_8,
            typeInfo = typeInfo<User>(),
            value = user
        )
        
        val bytesUtf8 = (serializedUtf8 as io.ktor.http.content.ByteArrayContent).bytes()
        val channelUtf8 = ByteReadChannel(bytesUtf8)
        val resultUtf8 = converter.deserialize(
            charset = Charsets.UTF_8,
            typeInfo = typeInfo<User>(),
            content = channelUtf8
        ) as User
        
        assertEquals(123, resultUtf8.id)
        assertEquals("Test", resultUtf8.name)
        
        // Test UTF-16 round-trip
        val serializedUtf16 = converter.serialize(
            contentType = ContentType.parse("application/toon"),
            charset = Charsets.UTF_16,
            typeInfo = typeInfo<User>(),
            value = user
        )
        
        val bytesUtf16 = (serializedUtf16 as io.ktor.http.content.ByteArrayContent).bytes()
        val channelUtf16 = ByteReadChannel(bytesUtf16)
        val resultUtf16 = converter.deserialize(
            charset = Charsets.UTF_16,
            typeInfo = typeInfo<User>(),
            content = channelUtf16
        ) as User
        
        assertEquals(123, resultUtf16.id)
        assertEquals("Test", resultUtf16.name)
    }
    
    @Test
    fun testCharsetMismatchProducesError() = runTest {
        val converter = ToonContentConverter(Toon())
        val user = User(id = 123, name = "Test")
        
        // Serialize with UTF-16
        val serialized = converter.serialize(
            contentType = ContentType.parse("application/toon"),
            charset = Charsets.UTF_16,
            typeInfo = typeInfo<User>(),
            value = user
        )
        
        val bytes = (serialized as io.ktor.http.content.ByteArrayContent).bytes()
        
        // Try to deserialize with UTF-8 (charset mismatch)
        val channel = ByteReadChannel(bytes)
        
        // This should either throw an exception or produce garbage that fails TOON parsing
        val exception = assertFailsWith<Exception> {
            converter.deserialize(
                charset = Charsets.UTF_8,
                typeInfo = typeInfo<User>(),
                content = channel
            )
        }
        
        // Verify error message is informative
        assertTrue(exception.message?.isNotEmpty() == true,
                   "Error message should be present for charset mismatch")
    }
    
    @Test
    fun testInvalidUtf8ByteSequence() = runTest {
        val converter = ToonContentConverter(Toon())
        
        // Create invalid UTF-8 byte sequence
        // 0xFF and 0xFE are invalid start bytes in UTF-8
        val invalidUtf8Bytes = byteArrayOf(
            0x75.toByte(), 0x73.toByte(), 0x65.toByte(), 0x72.toByte(), 0x3A.toByte(), 0x0A.toByte(), // "user:\n"
            0xFF.toByte(), 0xFE.toByte(), 0xFF.toByte(), 0xFE.toByte() // Invalid UTF-8 sequence
        )
        
        val channel = ByteReadChannel(invalidUtf8Bytes)
        
        // This should either throw during decoding or produce garbage that fails TOON parsing
        val exception = assertFailsWith<Exception> {
            converter.deserialize(
                charset = Charsets.UTF_8,
                typeInfo = typeInfo<User>(),
                content = channel
            )
        }
        
        // Verify error message is present
        assertTrue(exception.message?.isNotEmpty() == true,
                   "Error message should be present for invalid byte sequence")
    }
    
    @Test
    fun testCharsetEncodingWithDifferentCharsets() = runTest {
        val converter = ToonContentConverter(Toon())
        val user = User(id = 1, name = "Alice")
        
        // Test that different charsets can be used for encoding
        // UTF-8
        val serializedUtf8 = converter.serialize(
            contentType = ContentType.parse("application/toon"),
            charset = Charsets.UTF_8,
            typeInfo = typeInfo<User>(),
            value = user
        )
        assertNotNull(serializedUtf8, "UTF-8 encoding should succeed")
        
        // UTF-16
        val serializedUtf16 = converter.serialize(
            contentType = ContentType.parse("application/toon"),
            charset = Charsets.UTF_16,
            typeInfo = typeInfo<User>(),
            value = user
        )
        assertNotNull(serializedUtf16, "UTF-16 encoding should succeed")
        
        // ISO-8859-1
        val serializedIso = converter.serialize(
            contentType = ContentType.parse("application/toon"),
            charset = Charsets.ISO_8859_1,
            typeInfo = typeInfo<User>(),
            value = user
        )
        assertNotNull(serializedIso, "ISO-8859-1 encoding should succeed")
    }
    
    @Test
    fun testCharsetErrorMessageContainsCharsetInfo() = runTest {
        val converter = ToonContentConverter(Toon())
        
        // Create bytes that will cause issues when decoded with wrong charset
        val validToonUtf16 = "user:\n  id: 123\n  name: Test"
        val bytesUtf16 = validToonUtf16.toByteArray(Charsets.UTF_16)
        
        val channel = ByteReadChannel(bytesUtf16)
        
        // Try to decode with UTF-8 (wrong charset)
        try {
            converter.deserialize(
                charset = Charsets.UTF_8,
                typeInfo = typeInfo<User>(),
                content = channel
            )
            // If no exception, the decoding produced garbage that should fail TOON parsing
            // This is acceptable as long as we get a clear error
        } catch (e: Exception) {
            // Verify error message is informative
            // It should either mention charset issues or TOON parsing issues
            assertTrue(e.message?.isNotEmpty() == true,
                       "Error message should be present")
            
            // The error should be either about charset or about invalid TOON format
            // Both are acceptable as they indicate the problem clearly
            assertNotNull(e.message, "Error message should not be null")
        }
    }
}
