package io.ktoon

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.StructureKind
import kotlinx.serialization.encoding.AbstractEncoder
import kotlinx.serialization.encoding.CompositeEncoder
import kotlinx.serialization.modules.EmptySerializersModule
import kotlinx.serialization.modules.SerializersModule

@OptIn(ExperimentalSerializationApi::class)
internal class ToonEncoder(
    override val serializersModule: SerializersModule = EmptySerializersModule()
) : AbstractEncoder() {
    private val output = StringBuilder()
    private var indentationLevel = 0
    private var isFirstElement = true
    private var currentFieldName: String? = null
    private val visitedObjects = mutableSetOf<Int>()
    private var encodingState = EncodingState.IDLE
    
    private enum class EncodingState {
        IDLE,
        ENCODING_STRUCTURE,
        ENCODING_COLLECTION,
        ENCODING_VALUE
    }
    
    private fun writeIndentation() {
        repeat(indentationLevel) {
            output.append("  ")
        }
    }
    
    override fun beginStructure(descriptor: SerialDescriptor): CompositeEncoder {
        if (encodingState == EncodingState.ENCODING_VALUE) {
            throw IllegalStateException(
                "Cannot begin structure while encoding a value. " +
                "This indicates an invalid encoder method call sequence."
            )
        }
        
        when (descriptor.kind) {
            StructureKind.LIST -> {
                encodingState = EncodingState.ENCODING_COLLECTION
                val collectionEncoder = ToonCollectionEncoder(output, indentationLevel, currentFieldName, serializersModule)
                currentFieldName = null
                return collectionEncoder
            }
            StructureKind.CLASS, StructureKind.OBJECT, kotlinx.serialization.descriptors.PolymorphicKind.SEALED -> {
                encodingState = EncodingState.ENCODING_STRUCTURE
                if (currentFieldName != null) {
                    if (!isFirstElement) {
                        output.append("\n")
                    }
                    isFirstElement = false
                    writeIndentation()
                    output.append(currentFieldName)
                    output.append(":\n")
                    currentFieldName = null
                    indentationLevel++
                }
                return this
            }
            StructureKind.MAP -> {
                throw kotlinx.serialization.SerializationException(
                    "Unsupported type: Map serialization is not supported in TOON format. " +
                    "Type: ${descriptor.serialName}"
                )
            }
            else -> {
                throw kotlinx.serialization.SerializationException(
                    "Unsupported structure kind: ${descriptor.kind}. " +
                    "Type: ${descriptor.serialName}"
                )
            }
        }
    }
    
    override fun beginCollection(descriptor: SerialDescriptor, collectionSize: Int): CompositeEncoder {
        if (encodingState == EncodingState.ENCODING_VALUE) {
            throw IllegalStateException(
                "Cannot begin collection while encoding a value. " +
                "This indicates an invalid encoder method call sequence."
            )
        }
        
        encodingState = EncodingState.ENCODING_COLLECTION
        val collectionEncoder = ToonCollectionEncoder(output, indentationLevel, currentFieldName, serializersModule)
        collectionEncoder.setCollectionSize(collectionSize)
        currentFieldName = null
        return collectionEncoder
    }
    
    override fun endStructure(descriptor: SerialDescriptor) {
        when (descriptor.kind) {
            StructureKind.CLASS, StructureKind.OBJECT, kotlinx.serialization.descriptors.PolymorphicKind.SEALED -> {
                if (indentationLevel > 0) {
                    indentationLevel--
                }
                encodingState = EncodingState.IDLE
            }
            else -> {
                encodingState = EncodingState.IDLE
            }
        }
    }
    
    override fun encodeElement(descriptor: SerialDescriptor, index: Int): Boolean {
        currentFieldName = descriptor.getElementName(index)
        return true
    }
    
    override fun encodeValue(value: Any) {
        encodingState = EncodingState.ENCODING_VALUE
        
        if (currentFieldName != null) {
            if (!isFirstElement) {
                output.append("\n")
            }
            isFirstElement = false
            writeIndentation()
            output.append(currentFieldName)
            output.append(": ")
            currentFieldName = null
        }
        output.append(value.toString())
        
        encodingState = EncodingState.IDLE
    }
    
    override fun encodeNull() {
        if (currentFieldName != null) {
            if (!isFirstElement) {
                output.append("\n")
            }
            isFirstElement = false
            writeIndentation()
            output.append(currentFieldName)
            output.append(": ")
            currentFieldName = null
        }
        output.append("null")
    }
    
    override fun encodeBoolean(value: Boolean) {
        encodeValue(value)
    }
    
    override fun encodeByte(value: Byte) {
        encodeValue(value)
    }
    
    override fun encodeShort(value: Short) {
        encodeValue(value)
    }
    
    override fun encodeInt(value: Int) {
        encodeValue(value)
    }
    
    override fun encodeLong(value: Long) {
        encodeValue(value)
    }
    
    override fun encodeFloat(value: Float) {
        encodeValue(value)
    }
    
    override fun encodeDouble(value: Double) {
        encodeValue(value)
    }
    
    override fun encodeChar(value: Char) {
        encodeValue(value)
    }
    
    override fun encodeString(value: String) {
        encodeValue(value)
    }
    
    override fun toString(): String = output.toString()
}

@OptIn(ExperimentalSerializationApi::class)
internal class ToonCollectionEncoder(
    private val output: StringBuilder,
    private val indentationLevel: Int,
    private val collectionName: String?,
    override val serializersModule: SerializersModule
) : AbstractEncoder() {
    
    private var fieldNames: List<String>? = null
    private var headerWritten = false
    private var collectionSize = 0
    private var elementCount = 0
    
    fun setCollectionSize(size: Int) {
        collectionSize = size
    }
    
    override fun encodeElement(descriptor: SerialDescriptor, index: Int): Boolean {
        return true
    }
    
    override fun beginStructure(descriptor: SerialDescriptor): CompositeEncoder {
        if (!headerWritten && descriptor.kind == StructureKind.CLASS) {
            fieldNames = (0 until descriptor.elementsCount).map { descriptor.getElementName(it) }
            writeTableHeader()
            headerWritten = true
        } else if (!headerWritten) {
            throw kotlinx.serialization.SerializationException(
                "Collections in TOON format must contain objects (CLASS structures). " +
                "Found: ${descriptor.kind}, Type: ${descriptor.serialName}"
            )
        }
        
        elementCount++
        return ToonRowEncoder(output, indentationLevel, serializersModule)
    }
    
    private fun writeTableHeader() {
        if (!output.endsWith("\n") && output.isNotEmpty()) {
            output.append("\n")
        }
        repeat(indentationLevel) {
            output.append("  ")
        }
        output.append(collectionName ?: "items")
        output.append("[")
        output.append(collectionSize)
        output.append("]{")
        output.append(fieldNames?.joinToString(",") ?: "")
        output.append("}:\n")
    }
    
    override fun endStructure(descriptor: SerialDescriptor) {
    }
}

@OptIn(ExperimentalSerializationApi::class)
internal class ToonRowEncoder(
    private val output: StringBuilder,
    private val indentationLevel: Int,
    override val serializersModule: SerializersModule
) : AbstractEncoder() {
    
    private val rowValues = mutableListOf<String>()
    
    override fun encodeValue(value: Any) {
        rowValues.add(value.toString())
    }
    
    override fun encodeNull() {
        rowValues.add("")
    }
    
    override fun encodeBoolean(value: Boolean) {
        rowValues.add(value.toString())
    }
    
    override fun encodeByte(value: Byte) {
        rowValues.add(value.toString())
    }
    
    override fun encodeShort(value: Short) {
        rowValues.add(value.toString())
    }
    
    override fun encodeInt(value: Int) {
        rowValues.add(value.toString())
    }
    
    override fun encodeLong(value: Long) {
        rowValues.add(value.toString())
    }
    
    override fun encodeFloat(value: Float) {
        rowValues.add(value.toString())
    }
    
    override fun encodeDouble(value: Double) {
        rowValues.add(value.toString())
    }
    
    override fun encodeChar(value: Char) {
        rowValues.add(value.toString())
    }
    
    override fun encodeString(value: String) {
        rowValues.add(value)
    }
    
    override fun endStructure(descriptor: SerialDescriptor) {
        repeat(indentationLevel + 1) {
            output.append("  ")
        }
        output.append(rowValues.joinToString(","))
        output.append("\n")
    }
}
