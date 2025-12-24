package handler.command.impl.core;

import handler.command.impl.TestHelper;
import org.junit.jupiter.api.*;
import service.RedisLocalMap;
import service.ServerUtils;

import java.net.Socket;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for PingHandler
 * 
 * Tests cover:
 * - Basic PING command returning PONG
 * - PING in subscribe mode returning array format
 * - Multiple sequential PING commands
 * - Concurrent PING commands
 */
@DisplayName("PingHandler Tests")
class PingHandlerTest {

    private PingHandler pingHandler;
    private Socket testSocket;

    @BeforeEach
    void setUp() {
        pingHandler = new PingHandler();
        pingHandler.register();
        testSocket = TestHelper.createTestSocket();
        
        // Clear subscribe mode set
        RedisLocalMap.SUBSCRIBE_MODE_SET.clear();
    }

    @AfterEach
    void tearDown() {
        RedisLocalMap.SUBSCRIBE_MODE_SET.clear();
    }

    @Test
    @DisplayName("PING should return PONG as simple string")
    void testPingReturnsPong() {
        String result = pingHandler.process(testSocket, List.of());
        
        assertEquals("+PONG\r\n", result);
    }

    @Test
    @DisplayName("PING should return PONG even with null list")
    void testPingWithNullList() {
        String result = pingHandler.process(testSocket, null);
        
        assertEquals("+PONG\r\n", result);
    }

    @Test
    @DisplayName("Multiple PING commands in sequence should all return PONG")
    void testMultiplePingCommands() {
        String result1 = pingHandler.process(testSocket, List.of());
        String result2 = pingHandler.process(testSocket, List.of());
        
        assertEquals("+PONG\r\n", result1);
        assertEquals("+PONG\r\n", result2);
    }

    @Test
    @DisplayName("PING in subscribe mode should return array format")
    void testPingInSubscribeMode() {
        // Add client to subscribe mode
        String clientId = ServerUtils.formatIdFromSocket(testSocket);
        RedisLocalMap.SUBSCRIBE_MODE_SET.add(clientId);
        
        String result = pingHandler.process(testSocket, List.of());
        
        // Should return array format: *2\r\n$4\r\npong\r\n$0\r\n\r\n
        assertTrue(result.startsWith("*2\r\n"));
        assertTrue(result.contains("pong"));
    }

    @Test
    @DisplayName("PING handler should be registered correctly")
    void testHandlerRegistration() {
        assertTrue(handler.command.CommandHandler.HANDLER_MAP.containsKey("ping"));
        assertSame(pingHandler, handler.command.CommandHandler.HANDLER_MAP.get("ping"));
    }

    @Test
    @DisplayName("Concurrent PING commands should work correctly")
    void testConcurrentPingCommands() throws InterruptedException {
        Thread[] threads = new Thread[10];
        String[] results = new String[10];
        
        for (int i = 0; i < 10; i++) {
            final int index = i;
            threads[i] = new Thread(() -> {
                Socket threadSocket = TestHelper.createTestSocket("127.0.0." + index, 12345 + index);
                results[index] = pingHandler.process(threadSocket, List.of());
            });
        }
        
        for (Thread t : threads) t.start();
        for (Thread t : threads) t.join();
        
        for (String result : results) {
            assertEquals("+PONG\r\n", result);
        }
    }
}
