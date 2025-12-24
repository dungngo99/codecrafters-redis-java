package handler.command.impl.stream;

import handler.command.impl.TestHelper;
import org.junit.jupiter.api.*;
import service.RedisLocalMap;

import java.net.Socket;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for XRangeHandler
 * 
 * Tests cover:
 * - XRANGE with explicit start and end
 * - XRANGE with - (minimum) start
 * - XRANGE with + (maximum) end
 * - XRANGE on non-existent stream
 * - XRANGE on empty stream
 */
@DisplayName("XRangeHandler Tests")
class XRangeHandlerTest {

    private XRangeHandler xRangeHandler;
    private XAddHandler xAddHandler;
    private Socket testSocket;

    @BeforeEach
    void setUp() {
        xRangeHandler = new XRangeHandler();
        xAddHandler = new XAddHandler();
        xRangeHandler.register();
        xAddHandler.register();
        testSocket = TestHelper.createTestSocket();
        
        RedisLocalMap.LOCAL_MAP.clear();
    }

    @AfterEach
    void tearDown() {
        RedisLocalMap.LOCAL_MAP.clear();
    }

    @Test
    @DisplayName("XRANGE should return entries within range")
    void testXRangeWithinRange() {
        xAddHandler.process(testSocket, List.of("stream_key", "1-0", "a", "1"));
        xAddHandler.process(testSocket, List.of("stream_key", "2-0", "b", "2"));
        xAddHandler.process(testSocket, List.of("stream_key", "3-0", "c", "3"));
        
        String result = xRangeHandler.process(testSocket, List.of("stream_key", "1-0", "2-0"));
        
        assertTrue(result.contains("1-0"));
        assertTrue(result.contains("2-0"));
        assertFalse(result.contains("3-0"));
    }

    @Test
    @DisplayName("XRANGE with - and + should return all entries")
    void testXRangeAllEntries() {
        xAddHandler.process(testSocket, List.of("stream_key", "1-0", "a", "1"));
        xAddHandler.process(testSocket, List.of("stream_key", "2-0", "b", "2"));
        xAddHandler.process(testSocket, List.of("stream_key", "3-0", "c", "3"));
        
        String result = xRangeHandler.process(testSocket, List.of("stream_key", "-", "+"));
        
        assertTrue(result.contains("1-0"));
        assertTrue(result.contains("2-0"));
        assertTrue(result.contains("3-0"));
    }

    @Test
    @DisplayName("XRANGE on non-existent stream should return nil")
    void testXRangeNonExistentStream() {
        String result = xRangeHandler.process(testSocket, List.of("nonexistent", "-", "+"));
        
        assertEquals("$-1\r\n", result);
    }

    @Test
    @DisplayName("XRANGE with null list should throw exception")
    void testXRangeWithNullList() {
        assertThrows(RuntimeException.class, () -> {
            xRangeHandler.process(testSocket, null);
        });
    }

    @Test
    @DisplayName("XRANGE with insufficient params should throw exception")
    void testXRangeWithInsufficientParams() {
        assertThrows(RuntimeException.class, () -> {
            xRangeHandler.process(testSocket, List.of("stream_key", "1-0"));
        });
    }

    @Test
    @DisplayName("XRANGE handler should be registered correctly")
    void testHandlerRegistration() {
        assertTrue(handler.command.CommandHandler.HANDLER_MAP.containsKey("xrange"));
        assertSame(xRangeHandler, handler.command.CommandHandler.HANDLER_MAP.get("xrange"));
    }

    @Test
    @DisplayName("XRANGE should return entries in order")
    void testXRangeOrder() {
        xAddHandler.process(testSocket, List.of("stream_key", "1-0", "a", "1"));
        xAddHandler.process(testSocket, List.of("stream_key", "2-0", "b", "2"));
        
        String result = xRangeHandler.process(testSocket, List.of("stream_key", "-", "+"));
        
        // 1-0 should appear before 2-0 in the result
        int index1 = result.indexOf("1-0");
        int index2 = result.indexOf("2-0");
        assertTrue(index1 < index2);
    }

    @Test
    @DisplayName("XRANGE with start ID only should work")
    void testXRangeWithStartOnly() {
        xAddHandler.process(testSocket, List.of("stream_key", "1-0", "a", "1"));
        xAddHandler.process(testSocket, List.of("stream_key", "2-0", "b", "2"));
        xAddHandler.process(testSocket, List.of("stream_key", "3-0", "c", "3"));
        
        String result = xRangeHandler.process(testSocket, List.of("stream_key", "2-0", "+"));
        
        assertFalse(result.contains("1-0"));
        assertTrue(result.contains("2-0"));
        assertTrue(result.contains("3-0"));
    }
}
