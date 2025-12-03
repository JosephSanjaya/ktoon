package io.ktoon

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerializationException
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.StructureKind
import kotlinx.serialization.encoding.AbstractDecoder
import kotlinx.serialization.encoding.CompositeDecoder
import kotlinx.serialization.modules.EmptySerializersModule
import kotlinx.serialization.modules.SerializersModule

@OptIn(ExperimentalSerializationApi::class)
internal class ToonDecoder(
    input: String,
    override val serializersModule: SerializersModule = EmptySerializersModule()
) : AbstractDecoder() {
    private val lexer = ToonLexer(input)
    private var currentIndentationLevel = 0
    private var currentElementIndex = 0
    private var currentValue: String? = null
    private val expectedIndentationStack = mutableListOf<Int>()
    
    private fun getContextSnippet(lineNumber: Int): String {
        val lines = lexer.lines
        val startLine = maxOf(0, lineNumber - 2)
        val endLine = minOf(lines.size - 1, lineNumber + 1)
        
        val snippet = StringBuilder()
        for (i in startLine..endLine) {
            val marker = if (i == lineNumber) ">>> " else "    "
            snippet.append("$marker${i + 1}: ${lines[i]}\n")
        }
        return snippet.toString().trimEnd()
    }
    
    private fun throwSyntaxError(message: String): Nothing {
        val lineNumber = lexer.getCurrentLineNumber()
        val context = getContextSnippet(lineNumber - 1)
        throw SerializationException(
            "Syntax error at line $lineNumber: $message\n\nContext:\n$context"
        )
    }
    
    private fun throwIndentationError(expected: Int, actual: Int): Nothing {
        val lineNumber = lexer.getCurrentLineNumber()
        val context = getContextSnippet(lineNumber - 1)
        throw SerializationException(
            "Indentation error at line $lineNumber: expected $expected spaces but found $actual\n\nContext:\n$context"
        )
    }
    
    private fun throwTypeMismatch(expectedType: String, value: String, cause: Throwable? = null): Nothing {
        val lineNumber = lexer.getCurrentLineNumber()
        val context = getContextSnippet(lineNumber - 1)
        val causeMessage = if (cause != null) "\nCause: ${cause.message}" else ""
        throw SerializationException(
            "Type mismatch at line $lineNumber: cannot decode '$value' as $expectedType$causeMessage\n\nContext:\n$context"
        )
    }
    
    override fun beginStructure(descriptor: SerialDescriptor): CompositeDecoder {
        return when (descriptor.kind) {
            StructureKind.LIST -> {
                if (!lexer.hasMoreLines()) {
                    throwSyntaxError("Expected table header for collection but reached end of input")
                }
                if (lexer.isTableHeader()) {
                    val headerInfo = lexer.parseTableHeader()
                    lexer.advanceLine()
                    ToonCollectionDecoder(lexer, currentIndentationLevel, headerInfo, serializersModule)
                } else {
                    throwSyntaxError("Expected table header in format 'name[N]{field1,field2}:' for collection")
                }
            }
            StructureKind.CLASS, StructureKind.OBJECT, kotlinx.serialization.descriptors.PolymorphicKind.SEALED -> {
                // Skip empty lines to find the actual content
                while (lexer.hasMoreLines() && lexer.lines[lexer.currentLineIndex].trim().isEmpty()) {
                    lexer.advanceLine()
                }
                
                // Check if we need to increment indentation
                if (currentIndentationLevel > 0 || (lexer.hasMoreLines() && lexer.currentIndentation() > 0)) {
                    expectedIndentationStack.add(currentIndentationLevel)
                    currentIndentationLevel++
                }
                currentElementIndex = 0
                this
            }
            else -> {
                throw SerializationException(
                    "Unsupported structure kind: ${descriptor.kind} for type ${descriptor.serialName}"
                )
            }
        }
    }
    
    override fun endStructure(descriptor: SerialDescriptor) {
        when (descriptor.kind) {
            StructureKind.CLASS, StructureKind.OBJECT, kotlinx.serialization.descriptors.PolymorphicKind.SEALED -> {
                if (currentIndentationLevel > 0) {
                    currentIndentationLevel--
                    if (expectedIndentationStack.isNotEmpty()) {
                        expectedIndentationStack.removeAt(expectedIndentationStack.size - 1)
                    }
                }
            }
            else -> {}
        }
    }
    
    override fun decodeElementIndex(descriptor: SerialDescriptor): Int {
        while (lexer.hasMoreLines()) {
            val lineIndentation = lexer.currentIndentation()
            
            if (lineIndentation < currentIndentationLevel) {
                return CompositeDecoder.DECODE_DONE
            }
            
            if (lineIndentation > currentIndentationLevel) {
                throwIndentationError(currentIndentationLevel * 2, lineIndentation * 2)
            }
            
            if (lineIndentation == currentIndentationLevel) {
                val token = lexer.consumeToken()
                
                when (token) {
                    is Token.Identifier -> {
                        val fieldName = token.value
                        val colonToken = lexer.consumeToken()
                        
                        if (colonToken !is Token.Colon) {
                            throwSyntaxError("Expected ':' after field name '$fieldName'")
                        }
                        
                        val fieldIndex = (0 until descriptor.elementsCount).firstOrNull { 
                            descriptor.getElementName(it) == fieldName 
                        }
                        
                        if (fieldIndex == null) {
                            val availableFields = (0 until descriptor.elementsCount)
                                .map { descriptor.getElementName(it) }
                                .joinToString(", ")
                            throwSyntaxError(
                                "Unknown field '$fieldName' for type ${descriptor.serialName}. " +
                                "Available fields: $availableFields"
                            )
                        }
                        
                        val valueToken = lexer.consumeToken()
                        currentValue = when (valueToken) {
                            is Token.Identifier -> {
                                if (valueToken.value == "null") {
                                    null
                                } else {
                                    valueToken.value
                                }
                            }
                            is Token.Number -> valueToken.value
                            is Token.StringLiteral -> valueToken.value
                            null -> null
                            else -> throwSyntaxError("Unexpected token type after field '$fieldName'")
                        }
                        
                        lexer.advanceLine()
                        
                        return fieldIndex
                    }
                    else -> {
                        lexer.advanceLine()
                    }
                }
            } else {
                lexer.advanceLine()
            }
        }
        
        return CompositeDecoder.DECODE_DONE
    }
    
    override fun decodeNotNullMark(): Boolean {
        return currentValue != null
    }
    
    override fun decodeNull(): Nothing? {
        currentValue = null
        return null
    }
    
    override fun decodeBoolean(): Boolean {
        val value = currentValue ?: throwSyntaxError("No value available for decoding Boolean")
        return try {
            value.toBoolean()
        } catch (e: Exception) {
            throwTypeMismatch("Boolean", value, e)
        }
    }
    
    override fun decodeByte(): Byte {
        val value = currentValue ?: throwSyntaxError("No value available for decoding Byte")
        return try {
            value.toByte()
        } catch (e: NumberFormatException) {
            throwTypeMismatch("Byte", value, e)
        }
    }
    
    override fun decodeShort(): Short {
        val value = currentValue ?: throwSyntaxError("No value available for decoding Short")
        return try {
            value.toShort()
        } catch (e: NumberFormatException) {
            throwTypeMismatch("Short", value, e)
        }
    }
    
    override fun decodeInt(): Int {
        val value = currentValue ?: throwSyntaxError("No value available for decoding Int")
        return try {
            value.toInt()
        } catch (e: NumberFormatException) {
            throwTypeMismatch("Int", value, e)
        }
    }
    
    override fun decodeLong(): Long {
        val value = currentValue ?: throwSyntaxError("No value available for decoding Long")
        return try {
            value.toLong()
        } catch (e: NumberFormatException) {
            throwTypeMismatch("Long", value, e)
        }
    }
    
    override fun decodeFloat(): Float {
        val value = currentValue ?: throwSyntaxError("No value available for decoding Float")
        return try {
            value.toFloat()
        } catch (e: NumberFormatException) {
            throwTypeMismatch("Float", value, e)
        }
    }
    
    override fun decodeDouble(): Double {
        val value = currentValue ?: throwSyntaxError("No value available for decoding Double")
        return try {
            value.toDouble()
        } catch (e: NumberFormatException) {
            throwTypeMismatch("Double", value, e)
        }
    }
    
    override fun decodeChar(): Char {
        val value = currentValue ?: throwSyntaxError("No value available for decoding Char")
        return value.firstOrNull() ?: throwTypeMismatch("Char", value)
    }
    
    override fun decodeString(): String {
        return currentValue ?: throwSyntaxError("No value available for decoding String")
    }
}


@OptIn(ExperimentalSerializationApi::class)
internal class ToonCollectionDecoder(
    private val lexer: ToonLexer,
    private val indentationLevel: Int,
    private val headerInfo: TableHeaderInfo,
    override val serializersModule: SerializersModule
) : AbstractDecoder() {
    
    private var currentRowIndex = 0
    private var actualRowCount = 0
    
    override fun decodeCollectionSize(descriptor: SerialDescriptor): Int {
        return headerInfo.size
    }
    
    override fun decodeElementIndex(descriptor: SerialDescriptor): Int {
        if (currentRowIndex >= headerInfo.size) {
            if (actualRowCount < headerInfo.size) {
                val lineNumber = lexer.getCurrentLineNumber()
                val context = getContextSnippet(lineNumber - 1)
                throw SerializationException(
                    "Table size mismatch at line $lineNumber: header declares ${headerInfo.size} rows " +
                    "but only $actualRowCount rows were found\n\nContext:\n$context"
                )
            }
            return CompositeDecoder.DECODE_DONE
        }
        
        val index = currentRowIndex
        currentRowIndex++
        return index
    }
    
    override fun beginStructure(descriptor: SerialDescriptor): CompositeDecoder {
        if (!lexer.hasMoreLines()) {
            val lineNumber = lexer.getCurrentLineNumber()
            val context = getContextSnippet(lineNumber - 1)
            throw SerializationException(
                "Expected row data at line $lineNumber but reached end of input. " +
                "Table header declared ${headerInfo.size} rows but only $actualRowCount were found\n\nContext:\n$context"
            )
        }
        
        actualRowCount++
        return ToonRowDecoder(lexer, indentationLevel, headerInfo.fields, serializersModule)
    }
    
    private fun getContextSnippet(lineNumber: Int): String {
        val lines = lexer.lines
        val startLine = maxOf(0, lineNumber - 2)
        val endLine = minOf(lines.size - 1, lineNumber + 1)
        
        val snippet = StringBuilder()
        for (i in startLine..endLine) {
            val marker = if (i == lineNumber) ">>> " else "    "
            snippet.append("$marker${i + 1}: ${lines[i]}\n")
        }
        return snippet.toString().trimEnd()
    }
}


@OptIn(ExperimentalSerializationApi::class)
internal class ToonRowDecoder(
    private val lexer: ToonLexer,
    private val indentationLevel: Int,
    private val fieldNames: List<String>,
    override val serializersModule: SerializersModule
) : AbstractDecoder() {
    
    private val rowValues: List<String>
    private var currentFieldIndex = 0
    private val rowLineNumber: Int
    
    init {
        rowLineNumber = lexer.getCurrentLineNumber()
        val currentLine = lexer.lines[lexer.currentLineIndex]
        val trimmedLine = currentLine.trim()
        rowValues = trimmedLine.split(',').map { it.trim() }
        lexer.advanceLine()
        
        if (rowValues.size != fieldNames.size) {
            val context = getContextSnippet(rowLineNumber - 1)
            throw SerializationException(
                "Row field count mismatch at line $rowLineNumber: " +
                "row has ${rowValues.size} values but header declares ${fieldNames.size} fields (${fieldNames.joinToString(", ")})\n" +
                "Row values: [${rowValues.joinToString(", ")}]\n\nContext:\n$context"
            )
        }
    }
    
    private fun getContextSnippet(lineNumber: Int): String {
        val lines = lexer.lines
        val startLine = maxOf(0, lineNumber - 2)
        val endLine = minOf(lines.size - 1, lineNumber + 1)
        
        val snippet = StringBuilder()
        for (i in startLine..endLine) {
            val marker = if (i == lineNumber) ">>> " else "    "
            snippet.append("$marker${i + 1}: ${lines[i]}\n")
        }
        return snippet.toString().trimEnd()
    }
    
    override fun decodeElementIndex(descriptor: SerialDescriptor): Int {
        if (currentFieldIndex >= fieldNames.size) {
            return CompositeDecoder.DECODE_DONE
        }
        
        val index = currentFieldIndex
        currentFieldIndex++
        return index
    }
    
    override fun decodeNotNullMark(): Boolean {
        val value = rowValues[currentFieldIndex - 1]
        return value.isNotEmpty()
    }
    
    override fun decodeNull(): Nothing? {
        return null
    }
    
    private fun getCurrentValue(): String {
        val value = rowValues[currentFieldIndex - 1]
        if (value.isEmpty()) {
            val fieldName = fieldNames[currentFieldIndex - 1]
            val context = getContextSnippet(rowLineNumber - 1)
            throw SerializationException(
                "Cannot decode empty value for field '$fieldName' as non-nullable type at line $rowLineNumber\n\nContext:\n$context"
            )
        }
        return value
    }
    
    private fun throwTypeMismatch(expectedType: String, value: String, fieldName: String, cause: Throwable? = null): Nothing {
        val context = getContextSnippet(rowLineNumber - 1)
        val causeMessage = if (cause != null) "\nCause: ${cause.message}" else ""
        throw SerializationException(
            "Type mismatch at line $rowLineNumber, field '$fieldName': cannot decode '$value' as $expectedType$causeMessage\n\nContext:\n$context"
        )
    }
    
    override fun decodeBoolean(): Boolean {
        val value = getCurrentValue()
        val fieldName = fieldNames[currentFieldIndex - 1]
        return try {
            value.toBoolean()
        } catch (e: Exception) {
            throwTypeMismatch("Boolean", value, fieldName, e)
        }
    }
    
    override fun decodeByte(): Byte {
        val value = getCurrentValue()
        val fieldName = fieldNames[currentFieldIndex - 1]
        return try {
            value.toByte()
        } catch (e: NumberFormatException) {
            throwTypeMismatch("Byte", value, fieldName, e)
        }
    }
    
    override fun decodeShort(): Short {
        val value = getCurrentValue()
        val fieldName = fieldNames[currentFieldIndex - 1]
        return try {
            value.toShort()
        } catch (e: NumberFormatException) {
            throwTypeMismatch("Short", value, fieldName, e)
        }
    }
    
    override fun decodeInt(): Int {
        val value = getCurrentValue()
        val fieldName = fieldNames[currentFieldIndex - 1]
        return try {
            value.toInt()
        } catch (e: NumberFormatException) {
            throwTypeMismatch("Int", value, fieldName, e)
        }
    }
    
    override fun decodeLong(): Long {
        val value = getCurrentValue()
        val fieldName = fieldNames[currentFieldIndex - 1]
        return try {
            value.toLong()
        } catch (e: NumberFormatException) {
            throwTypeMismatch("Long", value, fieldName, e)
        }
    }
    
    override fun decodeFloat(): Float {
        val value = getCurrentValue()
        val fieldName = fieldNames[currentFieldIndex - 1]
        return try {
            value.toFloat()
        } catch (e: NumberFormatException) {
            throwTypeMismatch("Float", value, fieldName, e)
        }
    }
    
    override fun decodeDouble(): Double {
        val value = getCurrentValue()
        val fieldName = fieldNames[currentFieldIndex - 1]
        return try {
            value.toDouble()
        } catch (e: NumberFormatException) {
            throwTypeMismatch("Double", value, fieldName, e)
        }
    }
    
    override fun decodeChar(): Char {
        val value = getCurrentValue()
        val fieldName = fieldNames[currentFieldIndex - 1]
        return value.firstOrNull() ?: throwTypeMismatch("Char", value, fieldName)
    }
    
    override fun decodeString(): String {
        return getCurrentValue()
    }
}
