package io.ktoon

import kotlinx.serialization.SerializationException

internal class ToonLexer(input: String) {
    internal val lines: List<String> = input.lines()
    internal var currentLineIndex = 0
    private var currentPosition = 0
    
    private val currentLine: String
        get() = if (currentLineIndex < lines.size) lines[currentLineIndex] else ""
    
    fun currentIndentation(): Int {
        val line = currentLine
        var count = 0
        for (char in line) {
            if (char == ' ') {
                count++
            } else {
                break
            }
        }
        return count / 2
    }
    
    fun peekToken(): Token? {
        skipWhitespace()
        
        if (isAtEnd()) {
            return null
        }
        
        val line = currentLine
        val remaining = line.substring(currentPosition)
        
        if (isTableHeader()) {
            return parseTableHeaderToken()
        }
        
        return when {
            remaining.startsWith(':') -> Token.Colon
            remaining.startsWith(',') -> Token.Comma
            remaining[0].isDigit() || remaining[0] == '-' -> parseNumberToken()
            remaining[0].isLetter() || remaining[0] == '_' -> parseIdentifierToken()
            remaining[0] == '"' -> parseStringToken()
            else -> throw SerializationException("Unexpected character '${remaining[0]}' at line ${currentLineIndex + 1}, position $currentPosition")
        }
    }
    
    fun consumeToken(): Token? {
        val token = peekToken()
        if (token != null) {
            advanceToken(token)
        }
        return token
    }
    
    fun isTableHeader(): Boolean {
        val line = currentLine.trim()
        val pattern = Regex("""^\w+\[\d+]\{[^}]*}:$""")
        return pattern.matches(line)
    }
    
    fun parseTableHeader(): TableHeaderInfo {
        val line = currentLine.trim()
        val pattern = Regex("""^(\w+)\[(\d+)]\{([^}]*)}:$""")
        val match = pattern.matchEntire(line)
            ?: throw SerializationException("Invalid table header format at line ${currentLineIndex + 1}: $line")
        
        val name = match.groupValues[1]
        val size = match.groupValues[2].toInt()
        val fieldsString = match.groupValues[3]
        val fields = if (fieldsString.isEmpty()) {
            emptyList()
        } else {
            fieldsString.split(',').map { it.trim() }
        }
        
        return TableHeaderInfo(name, size, fields)
    }
    
    fun parseIdentifier(): String {
        skipWhitespace()
        val line = currentLine
        val start = currentPosition
        
        while (currentPosition < line.length && 
               (line[currentPosition].isLetterOrDigit() || line[currentPosition] == '_')) {
            currentPosition++
        }
        
        return line.substring(start, currentPosition)
    }
    
    fun parseNumber(): String {
        skipWhitespace()
        val line = currentLine
        val start = currentPosition
        
        if (currentPosition < line.length && line[currentPosition] == '-') {
            currentPosition++
        }
        
        while (currentPosition < line.length && 
               (line[currentPosition].isDigit() || line[currentPosition] == '.' || line[currentPosition] == '-')) {
            currentPosition++
        }
        
        return line.substring(start, currentPosition)
    }
    
    fun parseString(): String {
        skipWhitespace()
        val line = currentLine
        
        if (currentPosition >= line.length || line[currentPosition] != '"') {
            throw SerializationException("Expected string literal at line ${currentLineIndex + 1}, position $currentPosition")
        }
        
        currentPosition++
        val start = currentPosition
        
        while (currentPosition < line.length && line[currentPosition] != '"') {
            if (line[currentPosition] == '\\' && currentPosition + 1 < line.length) {
                currentPosition += 2
            } else {
                currentPosition++
            }
        }
        
        if (currentPosition >= line.length) {
            throw SerializationException("Unterminated string literal at line ${currentLineIndex + 1}")
        }
        
        val result = line.substring(start, currentPosition)
        currentPosition++
        
        return result
    }
    
    fun advanceLine() {
        currentLineIndex++
        currentPosition = 0
    }
    
    fun hasMoreLines(): Boolean {
        return currentLineIndex < lines.size
    }
    
    fun getCurrentLineNumber(): Int {
        return currentLineIndex + 1
    }
    
    private fun skipWhitespace() {
        val line = currentLine
        while (currentPosition < line.length && line[currentPosition] == ' ') {
            currentPosition++
        }
    }
    
    private fun isAtEnd(): Boolean {
        return currentLineIndex >= lines.size || currentPosition >= currentLine.length
    }
    
    private fun parseIdentifierToken(): Token.Identifier {
        val value = parseIdentifier()
        return Token.Identifier(value)
    }
    
    private fun parseNumberToken(): Token.Number {
        val value = parseNumber()
        return Token.Number(value)
    }
    
    private fun parseStringToken(): Token.StringLiteral {
        val value = parseString()
        return Token.StringLiteral(value)
    }
    
    private fun parseTableHeaderToken(): Token.TableHeader {
        val info = parseTableHeader()
        return Token.TableHeader(info.name, info.size, info.fields)
    }
    
    private fun advanceToken(token: Token) {
        when (token) {
            is Token.Colon -> currentPosition++
            is Token.Comma -> currentPosition++
            is Token.Identifier -> {}
            is Token.Number -> {}
            is Token.StringLiteral -> {}
            is Token.TableHeader -> advanceLine()
        }
    }
}

internal sealed class Token {
    data class Identifier(val value: String) : Token()
    data class Number(val value: String) : Token()
    data class StringLiteral(val value: String) : Token()
    object Colon : Token()
    object Comma : Token()
    data class TableHeader(val name: String, val size: Int, val fields: List<String>) : Token()
}

internal data class TableHeaderInfo(
    val name: String,
    val size: Int,
    val fields: List<String>
)
