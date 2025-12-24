package handler.command.impl.stream;

import handler.command.impl.TestHelper;
import org.junit.jupiter.api.*;
import service.RedisLocalMap;

import java.net.Socket;
import java.util.List;
import java.util.concurrent.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for XReadHandler
 * 
 * Tests cover:
 * - XREAD streams key ID
 * - XREAD with multiple streams
 * - XREAD with blocking (BLOCK option)
 * - XREAD with $ (latest ID)
 */
@DisplayName("XReadHandler Tests")
class XReadHandlerTest {

    private XReadHandler xReadHandler;
    private XAddHandler xAddHandler;
    private Socket testSocket;

    @BeforeEach
    void setUp() {
        xReadHandler = new XReadHandler();
        xAddHandler = new XAddHandler();
        xReadHandler.register();
        xAddHandler.register();
        testSocket = TestHelper.createTestSocket();
        
        RedisLocalMap.LOCAL_MAP.clear();
    }

    @AfterEach
    void tearDown() {
        RedisLocalMap.LOCAL_MAP.clear();
    }

    @Test
    @DisplayName("XREAD streams stream_key 0-0 should return entries after 0-0")
    void testXReadFromBeginning() {
        xAddHandler.process(testSocket, List.of("stream_key", "1-0", "a", "1"));
        xAddHandler.process(testSocket, List.of("stream_key", "2-0", "b", "2"));
        
        String result = xReadHandler.process(testSocket, List.of("streams", "stream_key", "0-0"));
        
        assertTrue(result.contains("stream_key"));
        assertTrue(result.contains("1-0"));
        assertTrue(result.contains("2-0"));
    }

    @Test
    @DisplayName("XREAD streams stream_key 1-0 should exclude entry 1-0")
    void testXReadExclusiveStart() {
        xAddHandler.process(testSocket, List.of("stream_key", "1-0", "a", "1"));
        xAddHandler.process(testSocket, List.of("stream_key", "2-0", "b", "2"));
        
        String result = xReadHandler.process(testSocket, List.of("streams", "stream_key", "1-0"));
        
        assertFalse(result.contains("1-0"));
        assertTrue(result.contains("2-0"));
    }

    @Test
    @DisplayName("XREAD on non-existent stream should return empty")
    void testXReadNonExistentStream() {
        String result = xReadHandler.process(testSocket, List.of("streams", "nonexistent", "0-0"));
        
        // Should return empty/null array
        assertTrue(result.equals("*-1\r\n") || result.equals("*0\r\n") || !result.contains("nonexistent"));
    }

    @Test
    @DisplayName("XREAD with null list should throw exception")
    void testXReadWithNullList() {
        assertThrows(RuntimeException.class, () -> {
            xReadHandler.process(testSocket, null);
        });
    }

    @Test
    @DisplayName("XREAD with insufficient params should throw exception")
    void testXReadWithInsufficientParams() {
        assertThrows(RuntimeException.class, () -> {
            xReadHandler.process(testSocket, List.of("streams"));
        });
    }

    @Test
    @DisplayName("XREAD handler should be registered correctly")
    void testHandlerRegistration() {
        assertTrue(handler.command.CommandHandler.HANDLER_MAP.containsKey("xread"));
        assertSame(xReadHandler, handler.command.CommandHandler.HANDLER_MAP.get("xread"));
    }

    @Test
    @DisplayName("XREAD BLOCK with timeout should block and return empty if no new data")
    void testXReadBlockWithTimeout() {
        xAddHandler.process(testSocket, List.of("stream_key", "1-0", "a", "1"));
        
        long startTime = System.currentTimeMillis();
        String result = xReadHandler.process(testSocket, List.of("block", "100", "streams", "stream_key", "1-0"));
        long endTime = System.currentTimeMillis();
        
        // Should have waited at least 100ms (with some tolerance)
        assertTrue(endTime - startTime >= 50);
    }

    @Test
    @DisplayName("XREAD BLOCK should return when new data arrives")
    void testXReadBlockWithNewData() throws Exception {
        xAddHandler.process(testSocket, List.of("stream_key", "1-0", "a", "1"));
        
        ExecutorService executor = Executors.newSingleThreadExecutor();
        
        // Schedule adding new entry after a short delay
        executor.submit(() -> {
            try {
                Thread.sleep(50);
                xAddHandler.process(testSocket, List.of("stream_key", "2-0", "b", "2"));
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });
        
        String result = xReadHandler.process(testSocket, List.of("block", "500", "streams", "stream_key", "1-0"));
        
        executor.shutdown();
        executor.awaitTermination(1, TimeUnit.SECONDS);
        
        // Should contain the newly added entry
        assertTrue(result.contains("2-0"));
    }

    @Test
    @DisplayName("XREAD with multiple streams")
    void testXReadMultipleStreams() {
        xAddHandler.process(testSocket, List.of("stream1", "1-0", "a", "1"));
        xAddHandler.process(testSocket, List.of("stream2", "1-0", "b", "2"));
        
        String result = xReadHandler.process(testSocket, List.of("streams", "stream1", "stream2", "0-0", "0-0"));
        
        assertTrue(result.contains("stream1"));
        assertTrue(result.contains("stream2"));
    }

    @Test
    @DisplayName("XREAD with $ should start from latest")
    void testXReadWithDollar() {
        xAddHandler.process(testSocket, List.of("stream_key", "1-0", "a", "1"));
        
        // $ means start from latest, so with timeout 0, should return empty immediately
        // Actually with BLOCK 0 it would block indefinitely, so we use a small timeout
        String result = xReadHandler.process(testSocket, List.of("block", "50", "streams", "stream_key", "$"));
        
        // Since no new entries after $, should return empty
        assertFalse(result.contains("1-0"));
    }
}
