package io.ktoon

import kotlinx.serialization.SerializationException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ToonLexerTest {
    
    @Test
    fun testIndentationCountingNoIndentation() {
        val lexer = ToonLexer("name: Alice")
        assertEquals(0, lexer.currentIndentation())
    }
    
    @Test
    fun testIndentationCountingSingleLevel() {
        val lexer = ToonLexer("  name: Alice")
        assertEquals(1, lexer.currentIndentation())
    }
    
    @Test
    fun testIndentationCountingMultipleLevels() {
        val lexer = ToonLexer("    name: Alice")
        assertEquals(2, lexer.currentIndentation())
    }
    
    @Test
    fun testIndentationCountingDeepNesting() {
        val lexer = ToonLexer("      name: Alice")
        assertEquals(3, lexer.currentIndentation())
    }
    
    @Test
    fun testIndentationCountingEmptyLine() {
        val lexer = ToonLexer("")
        assertEquals(0, lexer.currentIndentation())
    }
    
    @Test
    fun testIndentationCountingOnlySpaces() {
        val lexer = ToonLexer("    ")
        assertEquals(2, lexer.currentIndentation())
    }
    
    @Test
    fun testPeekTokenIdentifier() {
        val lexer = ToonLexer("name")
        val token = lexer.peekToken()
        assertTrue(token is Token.Identifier)
        assertEquals("name", (token as Token.Identifier).value)
    }
    
    @Test
    fun testPeekTokenNumber() {
        val lexer = ToonLexer("123")
        val token = lexer.peekToken()
        assertTrue(token is Token.Number)
        assertEquals("123", (token as Token.Number).value)
    }
    
    @Test
    fun testPeekTokenNegativeNumber() {
        val lexer = ToonLexer("-456")
        val token = lexer.peekToken()
        assertTrue(token is Token.Number)
        assertEquals("-456", (token as Token.Number).value)
    }
    
    @Test
    fun testPeekTokenDecimalNumber() {
        val lexer = ToonLexer("3.14")
        val token = lexer.peekToken()
        assertTrue(token is Token.Number)
        assertEquals("3.14", (token as Token.Number).value)
    }
    
    @Test
    fun testPeekTokenString() {
        val lexer = ToonLexer("\"hello world\"")
        val token = lexer.peekToken()
        assertTrue(token is Token.StringLiteral)
        assertEquals("hello world", (token as Token.StringLiteral).value)
    }
    
    @Test
    fun testPeekTokenColon() {
        val lexer = ToonLexer(":")
        val token = lexer.peekToken()
        assertTrue(token is Token.Colon)
    }
    
    @Test
    fun testPeekTokenComma() {
        val lexer = ToonLexer(",")
        val token = lexer.peekToken()
        assertTrue(token is Token.Comma)
    }
    
    @Test
    fun testPeekTokenReturnsToken() {
        val lexer = ToonLexer("name")
        val token = lexer.peekToken()
        assertTrue(token is Token.Identifier)
        assertEquals("name", (token as Token.Identifier).value)
    }
    
    @Test
    fun testPeekTokenWithLeadingWhitespace() {
        val lexer = ToonLexer("  name")
        val token = lexer.peekToken()
        assertTrue(token is Token.Identifier)
        assertEquals("name", (token as Token.Identifier).value)
    }
    
    @Test
    fun testPeekTokenReturnsNullAtEnd() {
        val lexer = ToonLexer("")
        val token = lexer.peekToken()
        assertNull(token)
    }
    
    @Test
    fun testConsumeTokenIdentifier() {
        val lexer = ToonLexer("name: Alice")
        val token = lexer.consumeToken()
        assertTrue(token is Token.Identifier)
        assertEquals("name", (token as Token.Identifier).value)
    }
    
    @Test
    fun testConsumeTokenAdvancesPosition() {
        val lexer = ToonLexer("name: Alice")
        lexer.consumeToken()
        val nextToken = lexer.peekToken()
        assertTrue(nextToken is Token.Colon)
    }
    
    @Test
    fun testConsumeTokenSequence() {
        val lexer = ToonLexer("name: Alice")
        
        val token1 = lexer.consumeToken()
        assertTrue(token1 is Token.Identifier)
        assertEquals("name", (token1 as Token.Identifier).value)
        
        val token2 = lexer.consumeToken()
        assertTrue(token2 is Token.Colon)
        
        val token3 = lexer.consumeToken()
        assertTrue(token3 is Token.Identifier)
        assertEquals("Alice", (token3 as Token.Identifier).value)
    }
    
    @Test
    fun testConsumeTokenReturnsNullAtEnd() {
        val lexer = ToonLexer("")
        val token = lexer.consumeToken()
        assertNull(token)
    }
    
    @Test
    fun testTableHeaderDetectionValid() {
        val lexer = ToonLexer("users[2]{id,name}:")
        assertTrue(lexer.isTableHeader())
    }
    
    @Test
    fun testTableHeaderDetectionValidWithSpaces() {
        val lexer = ToonLexer("  users[2]{id,name}:")
        assertTrue(lexer.isTableHeader())
    }
    
    @Test
    fun testTableHeaderDetectionEmptyFields() {
        val lexer = ToonLexer("items[0]{}:")
        assertTrue(lexer.isTableHeader())
    }
    
    @Test
    fun testTableHeaderDetectionSingleField() {
        val lexer = ToonLexer("data[1]{value}:")
        assertTrue(lexer.isTableHeader())
    }
    
    @Test
    fun testTableHeaderDetectionNotTableHeader() {
        val lexer = ToonLexer("name: Alice")
        assertFalse(lexer.isTableHeader())
    }
    
    @Test
    fun testTableHeaderDetectionMissingColon() {
        val lexer = ToonLexer("users[2]{id,name}")
        assertFalse(lexer.isTableHeader())
    }
    
    @Test
    fun testTableHeaderDetectionMissingBrackets() {
        val lexer = ToonLexer("users{id,name}:")
        assertFalse(lexer.isTableHeader())
    }
    
    @Test
    fun testTableHeaderParsingBasic() {
        val lexer = ToonLexer("users[2]{id,name}:")
        val header = lexer.parseTableHeader()
        
        assertEquals("users", header.name)
        assertEquals(2, header.size)
        assertEquals(listOf("id", "name"), header.fields)
    }
    
    @Test
    fun testTableHeaderParsingEmptyFields() {
        val lexer = ToonLexer("items[0]{}:")
        val header = lexer.parseTableHeader()
        
        assertEquals("items", header.name)
        assertEquals(0, header.size)
        assertEquals(emptyList(), header.fields)
    }
    
    @Test
    fun testTableHeaderParsingSingleField() {
        val lexer = ToonLexer("data[5]{value}:")
        val header = lexer.parseTableHeader()
        
        assertEquals("data", header.name)
        assertEquals(5, header.size)
        assertEquals(listOf("value"), header.fields)
    }
    
    @Test
    fun testTableHeaderParsingMultipleFields() {
        val lexer = ToonLexer("records[10]{id,name,email,age}:")
        val header = lexer.parseTableHeader()
        
        assertEquals("records", header.name)
        assertEquals(10, header.size)
        assertEquals(listOf("id", "name", "email", "age"), header.fields)
    }
    
    @Test
    fun testTableHeaderParsingWithWhitespace() {
        val lexer = ToonLexer("  users[2]{id,name}:")
        val header = lexer.parseTableHeader()
        
        assertEquals("users", header.name)
        assertEquals(2, header.size)
        assertEquals(listOf("id", "name"), header.fields)
    }
    
    @Test
    fun testTableHeaderParsingInvalidFormat() {
        val lexer = ToonLexer("invalid format")
        assertFailsWith<SerializationException> {
            lexer.parseTableHeader()
        }
    }
    
    @Test
    fun testParseIdentifierSimple() {
        val lexer = ToonLexer("name")
        val identifier = lexer.parseIdentifier()
        assertEquals("name", identifier)
    }
    
    @Test
    fun testParseIdentifierWithUnderscore() {
        val lexer = ToonLexer("user_name")
        val identifier = lexer.parseIdentifier()
        assertEquals("user_name", identifier)
    }
    
    @Test
    fun testParseIdentifierWithNumbers() {
        val lexer = ToonLexer("user123")
        val identifier = lexer.parseIdentifier()
        assertEquals("user123", identifier)
    }
    
    @Test
    fun testParseIdentifierStopsAtColon() {
        val lexer = ToonLexer("name: Alice")
        val identifier = lexer.parseIdentifier()
        assertEquals("name", identifier)
    }
    
    @Test
    fun testParseNumberInteger() {
        val lexer = ToonLexer("123")
        val number = lexer.parseNumber()
        assertEquals("123", number)
    }
    
    @Test
    fun testParseNumberNegative() {
        val lexer = ToonLexer("-456")
        val number = lexer.parseNumber()
        assertEquals("-456", number)
    }
    
    @Test
    fun testParseNumberDecimal() {
        val lexer = ToonLexer("3.14159")
        val number = lexer.parseNumber()
        assertEquals("3.14159", number)
    }
    
    @Test
    fun testParseNumberNegativeDecimal() {
        val lexer = ToonLexer("-2.5")
        val number = lexer.parseNumber()
        assertEquals("-2.5", number)
    }
    
    @Test
    fun testParseNumberStopsAtNonDigit() {
        val lexer = ToonLexer("123,456")
        val number = lexer.parseNumber()
        assertEquals("123", number)
    }
    
    @Test
    fun testParseStringSimple() {
        val lexer = ToonLexer("\"hello\"")
        val string = lexer.parseString()
        assertEquals("hello", string)
    }
    
    @Test
    fun testParseStringWithSpaces() {
        val lexer = ToonLexer("\"hello world\"")
        val string = lexer.parseString()
        assertEquals("hello world", string)
    }
    
    @Test
    fun testParseStringEmpty() {
        val lexer = ToonLexer("\"\"")
        val string = lexer.parseString()
        assertEquals("", string)
    }
    
    @Test
    fun testParseStringWithEscapedQuote() {
        val lexer = ToonLexer("\"hello \\\"world\\\"\"")
        val string = lexer.parseString()
        assertEquals("hello \\\"world\\\"", string)
    }
    
    @Test
    fun testParseStringUnterminated() {
        val lexer = ToonLexer("\"unterminated")
        assertFailsWith<SerializationException> {
            lexer.parseString()
        }
    }
    
    @Test
    fun testParseStringMissingOpeningQuote() {
        val lexer = ToonLexer("hello\"")
        assertFailsWith<SerializationException> {
            lexer.parseString()
        }
    }
    
    @Test
    fun testAdvanceLineMovesToNextLine() {
        val lexer = ToonLexer("line1\nline2")
        assertEquals(1, lexer.getCurrentLineNumber())
        lexer.advanceLine()
        assertEquals(2, lexer.getCurrentLineNumber())
    }
    
    @Test
    fun testHasMoreLinesTrue() {
        val lexer = ToonLexer("line1\nline2")
        assertTrue(lexer.hasMoreLines())
    }
    
    @Test
    fun testHasMoreLinesFalseAtEnd() {
        val lexer = ToonLexer("line1")
        lexer.advanceLine()
        assertFalse(lexer.hasMoreLines())
    }
    
    @Test
    fun testGetCurrentLineNumberStartsAtOne() {
        val lexer = ToonLexer("line1")
        assertEquals(1, lexer.getCurrentLineNumber())
    }
    
    @Test
    fun testGetCurrentLineNumberIncrementsWithAdvance() {
        val lexer = ToonLexer("line1\nline2\nline3")
        assertEquals(1, lexer.getCurrentLineNumber())
        lexer.advanceLine()
        assertEquals(2, lexer.getCurrentLineNumber())
        lexer.advanceLine()
        assertEquals(3, lexer.getCurrentLineNumber())
    }
    
    @Test
    fun testPeekTokenTableHeader() {
        val lexer = ToonLexer("users[2]{id,name}:")
        val token = lexer.peekToken()
        assertTrue(token is Token.TableHeader)
        val header = token as Token.TableHeader
        assertEquals("users", header.name)
        assertEquals(2, header.size)
        assertEquals(listOf("id", "name"), header.fields)
    }
    
    @Test
    fun testConsumeTokenTableHeaderAdvancesLine() {
        val lexer = ToonLexer("users[2]{id,name}:\n  1,Alice")
        assertEquals(1, lexer.getCurrentLineNumber())
        lexer.consumeToken()
        assertEquals(2, lexer.getCurrentLineNumber())
    }
    
    @Test
    fun testMultilineTokenSequence() {
        val input = """
            name: Alice
            age: 30
        """.trimIndent()
        
        val lexer = ToonLexer(input)
        
        assertEquals("name", (lexer.consumeToken() as Token.Identifier).value)
        assertTrue(lexer.consumeToken() is Token.Colon)
        assertEquals("Alice", (lexer.consumeToken() as Token.Identifier).value)
        
        lexer.advanceLine()
        
        assertEquals("age", (lexer.consumeToken() as Token.Identifier).value)
        assertTrue(lexer.consumeToken() is Token.Colon)
        assertEquals("30", (lexer.consumeToken() as Token.Number).value)
    }
    
    @Test
    fun testInvalidCharacterThrowsException() {
        val lexer = ToonLexer("@invalid")
        assertFailsWith<SerializationException> {
            lexer.peekToken()
        }
    }
}
