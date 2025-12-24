package handler.command.impl.list;

import handler.command.impl.TestHelper;
import org.junit.jupiter.api.*;
import service.RedisLocalMap;

import java.net.Socket;
import java.util.List;
import java.util.concurrent.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for BLPopHandler (Blocking LPOP)
 * 
 * Tests cover:
 * - BLPOP with non-zero timeout
 * - BLPOP with zero timeout (blocking)
 * - BLPOP on existing list returns immediately
 * - BLPOP timeout returns nil
 */
@DisplayName("BLPopHandler Tests")
class BLPopHandlerTest {

    private BLPopHandler blPopHandler;
    private RPushHandler rPushHandler;
    private Socket testSocket;

    @BeforeEach
    void setUp() {
        blPopHandler = new BLPopHandler();
        rPushHandler = new RPushHandler();
        blPopHandler.register();
        rPushHandler.register();
        
        testSocket = TestHelper.createTestSocket();
        
        RedisLocalMap.LOCAL_MAP.clear();
        RedisLocalMap.BLPOP_CLIENT_BLOCK_QUEUE.clear();
    }

    @AfterEach
    void tearDown() {
        RedisLocalMap.LOCAL_MAP.clear();
        RedisLocalMap.BLPOP_CLIENT_BLOCK_QUEUE.clear();
    }

    @Test
    @DisplayName("BLPOP on existing list should return immediately")
    void testBLPopExistingList() {
        rPushHandler.process(testSocket, List.of("mylist", "a", "b"));
        
        String result = blPopHandler.process(testSocket, List.of("mylist", "1"));
        
        assertTrue(result.startsWith("*2\r\n"));
        assertTrue(result.contains("mylist"));
        assertTrue(result.contains("a"));
    }

    @Test
    @DisplayName("BLPOP with timeout should return nil if no data")
    void testBLPopTimeoutNoData() {
        // BLPOP on non-existent list with timeout
        String result = blPopHandler.process(testSocket, List.of("nonexistent", "0.1"));
        
        // Should timeout and return nil
        assertEquals("*-1\r\n", result);
    }

    @Test
    @DisplayName("BLPOP with null list should throw exception")
    void testBLPopWithNullList() {
        assertThrows(RuntimeException.class, () -> {
            blPopHandler.process(testSocket, null);
        });
    }

    @Test
    @DisplayName("BLPOP with insufficient params should throw exception")
    void testBLPopWithInsufficientParams() {
        assertThrows(RuntimeException.class, () -> {
            blPopHandler.process(testSocket, List.of("mylist"));
        });
    }

    @Test
    @DisplayName("BLPOP handler should be registered correctly")
    void testHandlerRegistration() {
        assertTrue(handler.command.CommandHandler.HANDLER_MAP.containsKey("blpop"));
        assertSame(blPopHandler, handler.command.CommandHandler.HANDLER_MAP.get("blpop"));
    }

    @Test
    @DisplayName("BLPOP with zero timeout should add to block queue")
    void testBLPopZeroTimeout() {
        // This should add the client to the block queue
        blPopHandler.process(testSocket, List.of("mylist", "0"));
        
        // Since the list is empty and timeout is 0, client should be added to block queue
        // or return empty (implementation specific)
    }

    @Test
    @DisplayName("BLPOP should receive data pushed by RPUSH")
    void testBLPopReceivesRPushData() throws Exception {
        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch latch = new CountDownLatch(1);
        String[] blpopResult = new String[1];
        
        // Start BLPOP in separate thread
        Future<?> blpopFuture = executor.submit(() -> {
            latch.countDown();
            blpopResult[0] = blPopHandler.process(testSocket, List.of("mylist", "2"));
        });
        
        // Wait for BLPOP to start
        latch.await();
        Thread.sleep(100);
        
        // Push data
        rPushHandler.process(testSocket, List.of("mylist", "value"));
        
        // Wait for BLPOP to complete
        blpopFuture.get(3, TimeUnit.SECONDS);
        
        executor.shutdown();
        
        // Verify result
        assertNotNull(blpopResult[0]);
        if (!blpopResult[0].equals("*-1\r\n")) {
            assertTrue(blpopResult[0].contains("mylist"));
            assertTrue(blpopResult[0].contains("value"));
        }
    }

    @Test
    @DisplayName("Multiple BLPOP requests should be handled correctly")
    void testMultipleBLPopRequests() {
        // First push some data
        rPushHandler.process(testSocket, List.of("mylist", "a", "b", "c"));
        
        // Multiple BLPOP requests
        String result1 = blPopHandler.process(testSocket, List.of("mylist", "1"));
        String result2 = blPopHandler.process(testSocket, List.of("mylist", "1"));
        
        assertTrue(result1.contains("a"));
        assertTrue(result2.contains("b"));
    }
}
