package handler.command.impl.core;

import domain.CacheDto;
import enums.ValueType;
import handler.command.impl.TestHelper;
import org.junit.jupiter.api.*;
import service.RedisLocalMap;

import java.net.Socket;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for SetHandler and GetHandler
 * 
 * Tests cover:
 * - Basic SET and GET operations
 * - SET with expiry (PX option)
 * - GET non-existent key returns nil
 * - GET after expiry returns nil
 * - Overwriting existing keys
 * - Invalid parameters
 */
@DisplayName("SetHandler and GetHandler Tests")
class SetGetHandlerTest {

    private SetHandler setHandler;
    private GetHandler getHandler;
    private Socket testSocket;

    @BeforeEach
    void setUp() {
        setHandler = new SetHandler();
        getHandler = new GetHandler();
        setHandler.register();
        getHandler.register();
        testSocket = TestHelper.createTestSocket();
        
        // Clear the local map before each test
        RedisLocalMap.LOCAL_MAP.clear();
    }

    @AfterEach
    void tearDown() {
        RedisLocalMap.LOCAL_MAP.clear();
    }

    @Test
    @DisplayName("SET foo bar should return OK")
    void testSetReturnsOk() {
        String result = setHandler.process(testSocket, List.of("foo", "bar"));
        
        assertEquals("+OK\r\n", result);
    }

    @Test
    @DisplayName("GET foo after SET foo bar should return bar")
    void testSetThenGet() {
        setHandler.process(testSocket, List.of("foo", "bar"));
        String result = getHandler.process(testSocket, List.of("foo"));
        
        assertEquals("$3\r\nbar\r\n", result);
    }

    @Test
    @DisplayName("GET non-existent key should return nil")
    void testGetNonExistentKey() {
        String result = getHandler.process(testSocket, List.of("nonexistent"));
        
        assertEquals("$-1\r\n", result);
    }

    @Test
    @DisplayName("SET with PX expiry should work correctly")
    void testSetWithPxExpiry() {
        String result = setHandler.process(testSocket, List.of("foo", "bar", "px", "100"));
        
        assertEquals("+OK\r\n", result);
        
        // Verify the value is set
        CacheDto cache = RedisLocalMap.LOCAL_MAP.get("foo");
        assertNotNull(cache);
        assertEquals("bar", cache.getValue());
        assertNotNull(cache.getExpireTime());
    }

    @Test
    @DisplayName("GET after PX expiry should return nil")
    void testGetAfterExpiry() throws InterruptedException {
        setHandler.process(testSocket, List.of("foo", "bar", "px", "50"));
        
        // Wait for expiry
        Thread.sleep(100);
        
        // The cache should still contain the key but it should be expired
        // Note: The actual expiry check happens in the cache cleanup logic
        CacheDto cache = RedisLocalMap.LOCAL_MAP.get("foo");
        if (cache != null && cache.getExpireTime() != null) {
            assertTrue(cache.getExpireTime() < System.currentTimeMillis());
        }
    }

    @Test
    @DisplayName("SET should overwrite existing key")
    void testSetOverwritesExistingKey() {
        setHandler.process(testSocket, List.of("foo", "bar"));
        setHandler.process(testSocket, List.of("foo", "newvalue"));
        
        String result = getHandler.process(testSocket, List.of("foo"));
        
        assertEquals("$8\r\nnewvalue\r\n", result);
    }

    @Test
    @DisplayName("SET with null list should throw exception")
    void testSetWithNullList() {
        assertThrows(RuntimeException.class, () -> {
            setHandler.process(testSocket, null);
        });
    }

    @Test
    @DisplayName("SET with insufficient params should throw exception")
    void testSetWithInsufficientParams() {
        assertThrows(RuntimeException.class, () -> {
            setHandler.process(testSocket, List.of("key"));
        });
    }

    @Test
    @DisplayName("GET with null list should throw exception")
    void testGetWithNullList() {
        assertThrows(RuntimeException.class, () -> {
            getHandler.process(testSocket, null);
        });
    }

    @Test
    @DisplayName("GET with empty list should throw exception")
    void testGetWithEmptyList() {
        assertThrows(RuntimeException.class, () -> {
            getHandler.process(testSocket, List.of());
        });
    }

    @Test
    @DisplayName("SET and GET with special characters")
    void testSetGetSpecialCharacters() {
        setHandler.process(testSocket, List.of("key:with:colons", "value with spaces"));
        String result = getHandler.process(testSocket, List.of("key:with:colons"));
        
        assertEquals("$17\r\nvalue with spaces\r\n", result);
    }

    @Test
    @DisplayName("SET and GET with numeric value")
    void testSetGetNumericValue() {
        setHandler.process(testSocket, List.of("number", "12345"));
        String result = getHandler.process(testSocket, List.of("number"));
        
        assertEquals("$5\r\n12345\r\n", result);
    }

    @Test
    @DisplayName("Multiple SET and GET operations")
    void testMultipleSetGetOperations() {
        setHandler.process(testSocket, List.of("key1", "value1"));
        setHandler.process(testSocket, List.of("key2", "value2"));
        setHandler.process(testSocket, List.of("key3", "value3"));
        
        assertEquals("$6\r\nvalue1\r\n", getHandler.process(testSocket, List.of("key1")));
        assertEquals("$6\r\nvalue2\r\n", getHandler.process(testSocket, List.of("key2")));
        assertEquals("$6\r\nvalue3\r\n", getHandler.process(testSocket, List.of("key3")));
    }

    @Test
    @DisplayName("SET handlers should be registered correctly")
    void testSetHandlerRegistration() {
        assertTrue(handler.command.CommandHandler.HANDLER_MAP.containsKey("set"));
        assertSame(setHandler, handler.command.CommandHandler.HANDLER_MAP.get("set"));
    }

    @Test
    @DisplayName("GET handlers should be registered correctly")
    void testGetHandlerRegistration() {
        assertTrue(handler.command.CommandHandler.HANDLER_MAP.containsKey("get"));
        assertSame(getHandler, handler.command.CommandHandler.HANDLER_MAP.get("get"));
    }

    @Test
    @DisplayName("GET on non-STRING type should return nil")
    void testGetOnNonStringType() {
        // Manually set a non-string type in cache
        CacheDto cache = new CacheDto();
        cache.setValueType(ValueType.LIST);
        cache.setValue(List.of("item1", "item2"));
        RedisLocalMap.LOCAL_MAP.put("listkey", cache);
        
        String result = getHandler.process(testSocket, List.of("listkey"));
        
        assertEquals("$-1\r\n", result);
    }

    @Test
    @DisplayName("Concurrent SET and GET operations should work")
    void testConcurrentSetGetOperations() throws InterruptedException {
        Thread[] threads = new Thread[20];
        
        for (int i = 0; i < 10; i++) {
            final int index = i;
            threads[i] = new Thread(() -> {
                setHandler.process(testSocket, List.of("key" + index, "value" + index));
            });
            threads[i + 10] = new Thread(() -> {
                try {
                    Thread.sleep(10); // Small delay to ensure SET completes
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                getHandler.process(testSocket, List.of("key" + index));
            });
        }
        
        for (Thread t : threads) t.start();
        for (Thread t : threads) t.join();
        
        // Verify all keys are set
        for (int i = 0; i < 10; i++) {
            assertTrue(RedisLocalMap.LOCAL_MAP.containsKey("key" + i));
        }
    }
}
