package handler.command.impl.list;

import domain.CacheDto;
import enums.ValueType;
import handler.command.impl.TestHelper;
import org.junit.jupiter.api.*;
import service.RedisLocalMap;

import java.net.Socket;
import java.util.List;
import java.util.concurrent.LinkedBlockingDeque;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for List Handlers (LPushHandler, RPushHandler, LRangeHandler, LLenHandler, LPopHandler)
 * 
 * Tests cover:
 * - LPUSH adds elements to the left
 * - RPUSH adds elements to the right
 * - LRANGE returns range of elements
 * - LLEN returns list length
 * - LPOP removes and returns leftmost element
 */
@DisplayName("List Handler Tests")
class ListHandlerTest {

    private LPushHandler lPushHandler;
    private RPushHandler rPushHandler;
    private LRangeHandler lRangeHandler;
    private LLenHandler lLenHandler;
    private LPopHandler lPopHandler;
    private Socket testSocket;

    @BeforeEach
    void setUp() {
        lPushHandler = new LPushHandler();
        rPushHandler = new RPushHandler();
        lRangeHandler = new LRangeHandler();
        lLenHandler = new LLenHandler();
        lPopHandler = new LPopHandler();
        
        lPushHandler.register();
        rPushHandler.register();
        lRangeHandler.register();
        lLenHandler.register();
        lPopHandler.register();
        
        testSocket = TestHelper.createTestSocket();
        
        RedisLocalMap.LOCAL_MAP.clear();
        RedisLocalMap.BLPOP_CLIENT_BLOCK_QUEUE.clear();
    }

    @AfterEach
    void tearDown() {
        RedisLocalMap.LOCAL_MAP.clear();
        RedisLocalMap.BLPOP_CLIENT_BLOCK_QUEUE.clear();
    }

    // ==================== LPUSH Tests ====================
    
    @Test
    @DisplayName("LPUSH mylist a should return 1")
    void testLPushSingleElement() {
        String result = lPushHandler.process(testSocket, List.of("mylist", "a"));
        
        assertEquals(":1\r\n", result);
    }

    @Test
    @DisplayName("LPUSH mylist a b c should return 3")
    void testLPushMultipleElements() {
        String result = lPushHandler.process(testSocket, List.of("mylist", "a", "b", "c"));
        
        assertEquals(":3\r\n", result);
    }

    @Test
    @DisplayName("LPUSH should add elements to the left (head)")
    void testLPushAddsToLeft() {
        lPushHandler.process(testSocket, List.of("mylist", "a"));
        lPushHandler.process(testSocket, List.of("mylist", "b"));
        
        String result = lRangeHandler.process(testSocket, List.of("mylist", "0", "-1"));
        
        // b should be first, then a
        int indexB = result.indexOf("$1\r\nb");
        int indexA = result.indexOf("$1\r\na");
        assertTrue(indexB < indexA);
    }

    @Test
    @DisplayName("LPUSH with null list should throw exception")
    void testLPushWithNullList() {
        assertThrows(RuntimeException.class, () -> {
            lPushHandler.process(testSocket, null);
        });
    }

    @Test
    @DisplayName("LPUSH with insufficient params should throw exception")
    void testLPushWithInsufficientParams() {
        assertThrows(RuntimeException.class, () -> {
            lPushHandler.process(testSocket, List.of("mylist"));
        });
    }

    // ==================== RPUSH Tests ====================
    
    @Test
    @DisplayName("RPUSH mylist a should return 1")
    void testRPushSingleElement() {
        String result = rPushHandler.process(testSocket, List.of("mylist", "a"));
        
        assertEquals(":1\r\n", result);
    }

    @Test
    @DisplayName("RPUSH mylist a b c should return 3")
    void testRPushMultipleElements() {
        String result = rPushHandler.process(testSocket, List.of("mylist", "a", "b", "c"));
        
        assertEquals(":3\r\n", result);
    }

    @Test
    @DisplayName("RPUSH should add elements to the right (tail)")
    void testRPushAddsToRight() {
        rPushHandler.process(testSocket, List.of("mylist", "a"));
        rPushHandler.process(testSocket, List.of("mylist", "b"));
        
        String result = lRangeHandler.process(testSocket, List.of("mylist", "0", "-1"));
        
        // a should be first, then b
        int indexA = result.indexOf("$1\r\na");
        int indexB = result.indexOf("$1\r\nb");
        assertTrue(indexA < indexB);
    }

    @Test
    @DisplayName("RPUSH with null list should throw exception")
    void testRPushWithNullList() {
        assertThrows(RuntimeException.class, () -> {
            rPushHandler.process(testSocket, null);
        });
    }

    // ==================== LRANGE Tests ====================
    
    @Test
    @DisplayName("LRANGE mylist 0 -1 should return all elements")
    void testLRangeAllElements() {
        rPushHandler.process(testSocket, List.of("mylist", "a", "b", "c"));
        
        String result = lRangeHandler.process(testSocket, List.of("mylist", "0", "-1"));
        
        assertTrue(result.startsWith("*3\r\n"));
        assertTrue(result.contains("a"));
        assertTrue(result.contains("b"));
        assertTrue(result.contains("c"));
    }

    @Test
    @DisplayName("LRANGE mylist 0 0 should return first element")
    void testLRangeFirstElement() {
        rPushHandler.process(testSocket, List.of("mylist", "a", "b", "c"));
        
        String result = lRangeHandler.process(testSocket, List.of("mylist", "0", "0"));
        
        assertTrue(result.startsWith("*1\r\n"));
        assertTrue(result.contains("a"));
    }

    @Test
    @DisplayName("LRANGE mylist -2 -1 should return last two elements")
    void testLRangeNegativeIndices() {
        rPushHandler.process(testSocket, List.of("mylist", "a", "b", "c"));
        
        String result = lRangeHandler.process(testSocket, List.of("mylist", "-2", "-1"));
        
        assertTrue(result.startsWith("*2\r\n"));
        assertTrue(result.contains("b"));
        assertTrue(result.contains("c"));
    }

    @Test
    @DisplayName("LRANGE on non-existent key should return empty array")
    void testLRangeNonExistentKey() {
        String result = lRangeHandler.process(testSocket, List.of("nonexistent", "0", "-1"));
        
        assertEquals("*0\r\n", result);
    }

    @Test
    @DisplayName("LRANGE with null list should throw exception")
    void testLRangeWithNullList() {
        assertThrows(RuntimeException.class, () -> {
            lRangeHandler.process(testSocket, null);
        });
    }

    @Test
    @DisplayName("LRANGE with insufficient params should throw exception")
    void testLRangeWithInsufficientParams() {
        assertThrows(RuntimeException.class, () -> {
            lRangeHandler.process(testSocket, List.of("mylist", "0"));
        });
    }

    // ==================== LLEN Tests ====================
    
    @Test
    @DisplayName("LLEN on existing list should return length")
    void testLLenExistingList() {
        rPushHandler.process(testSocket, List.of("mylist", "a", "b", "c"));
        
        String result = lLenHandler.process(testSocket, List.of("mylist"));
        
        assertEquals(":3\r\n", result);
    }

    @Test
    @DisplayName("LLEN on non-existent key should return 0")
    void testLLenNonExistentKey() {
        String result = lLenHandler.process(testSocket, List.of("nonexistent"));
        
        assertEquals(":0\r\n", result);
    }

    @Test
    @DisplayName("LLEN with null list should throw exception")
    void testLLenWithNullList() {
        assertThrows(RuntimeException.class, () -> {
            lLenHandler.process(testSocket, null);
        });
    }

    @Test
    @DisplayName("LLEN with empty list should throw exception")
    void testLLenWithEmptyList() {
        assertThrows(RuntimeException.class, () -> {
            lLenHandler.process(testSocket, List.of());
        });
    }

    // ==================== LPOP Tests ====================
    
    @Test
    @DisplayName("LPOP should remove and return leftmost element")
    void testLPopReturnsLeftmost() {
        rPushHandler.process(testSocket, List.of("mylist", "a", "b", "c"));
        
        String result = lPopHandler.process(testSocket, List.of("mylist"));
        
        assertEquals("$1\r\na\r\n", result);
        
        // Verify list now has 2 elements
        String lenResult = lLenHandler.process(testSocket, List.of("mylist"));
        assertEquals(":2\r\n", lenResult);
    }

    @Test
    @DisplayName("LPOP on non-existent key should return nil")
    void testLPopNonExistentKey() {
        String result = lPopHandler.process(testSocket, List.of("nonexistent"));
        
        assertEquals("$-1\r\n", result);
    }

    @Test
    @DisplayName("LPOP on empty list should return nil")
    void testLPopEmptyList() {
        // Create empty list
        CacheDto cache = new CacheDto();
        cache.setValueType(ValueType.LIST);
        cache.setValue(new LinkedBlockingDeque<>());
        RedisLocalMap.LOCAL_MAP.put("emptylist", cache);
        
        String result = lPopHandler.process(testSocket, List.of("emptylist"));
        
        assertEquals("$-1\r\n", result);
    }

    @Test
    @DisplayName("LPOP with count should return multiple elements")
    void testLPopWithCount() {
        rPushHandler.process(testSocket, List.of("mylist", "a", "b", "c"));
        
        String result = lPopHandler.process(testSocket, List.of("mylist", "2"));
        
        assertTrue(result.startsWith("*2\r\n"));
        assertTrue(result.contains("a"));
        assertTrue(result.contains("b"));
    }

    @Test
    @DisplayName("LPOP with null list should throw exception")
    void testLPopWithNullList() {
        assertThrows(RuntimeException.class, () -> {
            lPopHandler.process(testSocket, null);
        });
    }

    // ==================== Integration Tests ====================
    
    @Test
    @DisplayName("Combined LPUSH, RPUSH, LPOP, LLEN operations")
    void testCombinedOperations() {
        // RPUSH a, b
        rPushHandler.process(testSocket, List.of("mylist", "a", "b"));
        assertEquals(":2\r\n", lLenHandler.process(testSocket, List.of("mylist")));
        
        // LPUSH c (list becomes: c, a, b)
        lPushHandler.process(testSocket, List.of("mylist", "c"));
        assertEquals(":3\r\n", lLenHandler.process(testSocket, List.of("mylist")));
        
        // LPOP should return c
        String popResult = lPopHandler.process(testSocket, List.of("mylist"));
        assertEquals("$1\r\nc\r\n", popResult);
        
        // LRANGE should now return a, b
        String rangeResult = lRangeHandler.process(testSocket, List.of("mylist", "0", "-1"));
        assertTrue(rangeResult.startsWith("*2\r\n"));
    }

    @Test
    @DisplayName("Handlers should be registered correctly")
    void testHandlerRegistration() {
        assertTrue(handler.command.CommandHandler.HANDLER_MAP.containsKey("lpush"));
        assertTrue(handler.command.CommandHandler.HANDLER_MAP.containsKey("rpush"));
        assertTrue(handler.command.CommandHandler.HANDLER_MAP.containsKey("lrange"));
        assertTrue(handler.command.CommandHandler.HANDLER_MAP.containsKey("llen"));
        assertTrue(handler.command.CommandHandler.HANDLER_MAP.containsKey("lpop"));
    }

    @Test
    @DisplayName("LLEN on non-LIST type should throw exception")
    void testLLenOnNonListType() {
        CacheDto cache = new CacheDto();
        cache.setValueType(ValueType.STRING);
        cache.setValue("string value");
        RedisLocalMap.LOCAL_MAP.put("stringkey", cache);
        
        assertThrows(RuntimeException.class, () -> {
            lLenHandler.process(testSocket, List.of("stringkey"));
        });
    }

    @Test
    @DisplayName("Multiple pop operations should empty the list")
    void testMultiplePopsEmptyList() {
        rPushHandler.process(testSocket, List.of("mylist", "a", "b"));
        
        lPopHandler.process(testSocket, List.of("mylist"));
        lPopHandler.process(testSocket, List.of("mylist"));
        String result = lPopHandler.process(testSocket, List.of("mylist"));
        
        assertEquals("$-1\r\n", result);
    }
}
