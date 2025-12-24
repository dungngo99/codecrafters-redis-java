package handler.command.impl.core;

import handler.command.impl.TestHelper;
import org.junit.jupiter.api.*;

import java.net.Socket;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for EchoHandler
 * 
 * Tests cover:
 * - Basic ECHO command with simple message
 * - ECHO with empty string
 * - ECHO with special characters
 * - Invalid parameters (null, empty list)
 */
@DisplayName("EchoHandler Tests")
class EchoHandlerTest {

    private EchoHandler echoHandler;
    private Socket testSocket;

    @BeforeEach
    void setUp() {
        echoHandler = new EchoHandler();
        echoHandler.register();
        testSocket = TestHelper.createTestSocket();
    }

    @Test
    @DisplayName("ECHO 'hey' should return bulk string with 'hey'")
    void testEchoSimpleMessage() {
        String result = echoHandler.process(testSocket, List.of("hey"));
        
        assertEquals("$3\r\nhey\r\n", result);
    }

    @Test
    @DisplayName("ECHO 'hello world' should return bulk string")
    void testEchoWithSpace() {
        String result = echoHandler.process(testSocket, List.of("hello world"));
        
        assertEquals("$11\r\nhello world\r\n", result);
    }

    @Test
    @DisplayName("ECHO empty string should return bulk string with empty content")
    void testEchoEmptyString() {
        String result = echoHandler.process(testSocket, List.of(""));
        
        assertEquals("$0\r\n\r\n", result);
    }

    @Test
    @DisplayName("ECHO with null list should throw exception")
    void testEchoWithNullList() {
        assertThrows(RuntimeException.class, () -> {
            echoHandler.process(testSocket, null);
        });
    }

    @Test
    @DisplayName("ECHO with empty list should throw exception")
    void testEchoWithEmptyList() {
        assertThrows(RuntimeException.class, () -> {
            echoHandler.process(testSocket, List.of());
        });
    }

    @Test
    @DisplayName("ECHO with special characters")
    void testEchoSpecialCharacters() {
        String result = echoHandler.process(testSocket, List.of("hello\nworld"));
        
        assertEquals("$11\r\nhello\nworld\r\n", result);
    }

    @Test
    @DisplayName("ECHO with unicode characters")
    void testEchoUnicodeCharacters() {
        String message = "こんにちは";
        String result = echoHandler.process(testSocket, List.of(message));
        
        // Unicode characters have different byte lengths
        assertTrue(result.startsWith("$"));
        assertTrue(result.contains(message));
        assertTrue(result.endsWith("\r\n"));
    }

    @Test
    @DisplayName("ECHO handler should be registered correctly")
    void testHandlerRegistration() {
        assertTrue(handler.command.CommandHandler.HANDLER_MAP.containsKey("echo"));
        assertSame(echoHandler, handler.command.CommandHandler.HANDLER_MAP.get("echo"));
    }

    @Test
    @DisplayName("Multiple ECHO commands should work correctly")
    void testMultipleEchoCommands() {
        String result1 = echoHandler.process(testSocket, List.of("first"));
        String result2 = echoHandler.process(testSocket, List.of("second"));
        
        assertEquals("$5\r\nfirst\r\n", result1);
        assertEquals("$6\r\nsecond\r\n", result2);
    }

    @Test
    @DisplayName("ECHO with very long string")
    void testEchoLongString() {
        String longMessage = "a".repeat(10000);
        String result = echoHandler.process(testSocket, List.of(longMessage));
        
        assertEquals("$10000\r\n" + longMessage + "\r\n", result);
    }
}
