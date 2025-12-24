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
 * Unit tests for IncrHandler
 * 
 * Tests cover:
 * - INCR on non-existent key (should initialize to 0 then increment)
 * - INCR on existing numeric string
 * - INCR on non-numeric string should return error
 * - INCR on negative numbers
 * - Multiple INCR operations
 */
@DisplayName("IncrHandler Tests")
class IncrHandlerTest {

    private IncrHandler incrHandler;
    private SetHandler setHandler;
    private Socket testSocket;

    @BeforeEach
    void setUp() {
        incrHandler = new IncrHandler();
        setHandler = new SetHandler();
        incrHandler.register();
        setHandler.register();
        testSocket = TestHelper.createTestSocket();
        
        RedisLocalMap.LOCAL_MAP.clear();
    }

    @AfterEach
    void tearDown() {
        RedisLocalMap.LOCAL_MAP.clear();
    }

    @Test
    @DisplayName("INCR non-existent key should return 1")
    void testIncrNonExistentKey() {
        String result = incrHandler.process(testSocket, List.of("counter"));
        
        assertEquals(":1\r\n", result);
    }

    @Test
    @DisplayName("SET foo 5, INCR foo should return 6")
    void testIncrExistingNumericValue() {
        setHandler.process(testSocket, List.of("foo", "5"));
        String result = incrHandler.process(testSocket, List.of("foo"));
        
        assertEquals(":6\r\n", result);
    }

    @Test
    @DisplayName("Multiple INCR operations should increment correctly")
    void testMultipleIncrOperations() {
        setHandler.process(testSocket, List.of("foo", "5"));
        
        String result1 = incrHandler.process(testSocket, List.of("foo"));
        String result2 = incrHandler.process(testSocket, List.of("foo"));
        
        assertEquals(":6\r\n", result1);
        assertEquals(":7\r\n", result2);
    }

    @Test
    @DisplayName("SET foo bar, INCR foo should return error")
    void testIncrNonNumericValue() {
        setHandler.process(testSocket, List.of("foo", "bar"));
        String result = incrHandler.process(testSocket, List.of("foo"));
        
        assertTrue(result.startsWith("-ERR"));
        assertTrue(result.contains("not an integer"));
    }

    @Test
    @DisplayName("INCR with null list should throw exception")
    void testIncrWithNullList() {
        assertThrows(RuntimeException.class, () -> {
            incrHandler.process(testSocket, null);
        });
    }

    @Test
    @DisplayName("INCR with empty list should throw exception")
    void testIncrWithEmptyList() {
        assertThrows(RuntimeException.class, () -> {
            incrHandler.process(testSocket, List.of());
        });
    }

    @Test
    @DisplayName("INCR on negative number should work")
    void testIncrNegativeNumber() {
        setHandler.process(testSocket, List.of("foo", "-5"));
        String result = incrHandler.process(testSocket, List.of("foo"));
        
        assertEquals(":-4\r\n", result);
    }

    @Test
    @DisplayName("INCR on zero should return 1")
    void testIncrZero() {
        setHandler.process(testSocket, List.of("foo", "0"));
        String result = incrHandler.process(testSocket, List.of("foo"));
        
        assertEquals(":1\r\n", result);
    }

    @Test
    @DisplayName("INCR handler should be registered correctly")
    void testHandlerRegistration() {
        assertTrue(handler.command.CommandHandler.HANDLER_MAP.containsKey("incr"));
        assertSame(incrHandler, handler.command.CommandHandler.HANDLER_MAP.get("incr"));
    }

    @Test
    @DisplayName("INCR on non-STRING type should throw exception")
    void testIncrOnNonStringType() {
        CacheDto cache = new CacheDto();
        cache.setValueType(ValueType.LIST);
        cache.setValue(List.of("item1"));
        RedisLocalMap.LOCAL_MAP.put("listkey", cache);
        
        assertThrows(RuntimeException.class, () -> {
            incrHandler.process(testSocket, List.of("listkey"));
        });
    }

    @Test
    @DisplayName("INCR on string with spaces should return error")
    void testIncrStringWithSpaces() {
        setHandler.process(testSocket, List.of("foo", "5 5"));
        String result = incrHandler.process(testSocket, List.of("foo"));
        
        assertTrue(result.startsWith("-ERR"));
    }

    @Test
    @DisplayName("INCR on string with decimal should return error")
    void testIncrDecimalString() {
        setHandler.process(testSocket, List.of("foo", "5.5"));
        String result = incrHandler.process(testSocket, List.of("foo"));
        
        assertTrue(result.startsWith("-ERR"));
    }

    @Test
    @DisplayName("Concurrent INCR operations should work correctly")
    void testConcurrentIncrOperations() throws InterruptedException {
        setHandler.process(testSocket, List.of("counter", "0"));
        
        Thread[] threads = new Thread[10];
        for (int i = 0; i < 10; i++) {
            threads[i] = new Thread(() -> {
                incrHandler.process(testSocket, List.of("counter"));
            });
        }
        
        for (Thread t : threads) t.start();
        for (Thread t : threads) t.join();
        
        CacheDto cache = RedisLocalMap.LOCAL_MAP.get("counter");
        int finalValue = Integer.parseInt((String) cache.getValue());
        assertEquals(10, finalValue);
    }
}
