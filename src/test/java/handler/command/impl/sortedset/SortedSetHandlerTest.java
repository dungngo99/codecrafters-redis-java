package handler.command.impl.sortedset;

import domain.CacheDto;
import enums.ValueType;
import handler.command.impl.TestHelper;
import org.junit.jupiter.api.*;
import service.RedisLocalMap;

import java.net.Socket;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for Sorted Set Handlers (ZAddHandler, ZRangeHandler, ZRankHandler, ZCardHandler, ZRemHandler, ZSCoreHandler)
 * 
 * Tests cover:
 * - ZADD adds members with scores
 * - ZRANGE returns members in score order
 * - ZRANK returns member rank
 * - ZCARD returns cardinality
 * - ZREM removes members
 * - ZSCORE returns member score
 */
@DisplayName("Sorted Set Handler Tests")
class SortedSetHandlerTest {

    private ZAddHandler zAddHandler;
    private ZRangeHandler zRangeHandler;
    private ZRankHandler zRankHandler;
    private ZCardHandler zCardHandler;
    private ZRemHandler zRemHandler;
    private ZSCoreHandler zScoreHandler;
    private Socket testSocket;

    @BeforeEach
    void setUp() {
        zAddHandler = new ZAddHandler();
        zRangeHandler = new ZRangeHandler();
        zRankHandler = new ZRankHandler();
        zCardHandler = new ZCardHandler();
        zRemHandler = new ZRemHandler();
        zScoreHandler = new ZSCoreHandler();
        
        zAddHandler.register();
        zRangeHandler.register();
        zRankHandler.register();
        zCardHandler.register();
        zRemHandler.register();
        zScoreHandler.register();
        
        testSocket = TestHelper.createTestSocket();
        
        RedisLocalMap.LOCAL_MAP.clear();
    }

    @AfterEach
    void tearDown() {
        RedisLocalMap.LOCAL_MAP.clear();
    }

    // ==================== ZADD Tests ====================
    
    @Test
    @DisplayName("ZADD myzset 1 member1 should return 1 for new member")
    void testZAddNewMember() {
        String result = zAddHandler.process(testSocket, List.of("myzset", "1", "member1"));
        
        assertEquals(":1\r\n", result);
    }

    @Test
    @DisplayName("ZADD should update existing member score and return 0")
    void testZAddUpdateScore() {
        zAddHandler.process(testSocket, List.of("myzset", "1", "member1"));
        String result = zAddHandler.process(testSocket, List.of("myzset", "2", "member1"));
        
        assertEquals(":0\r\n", result);
    }

    @Test
    @DisplayName("ZADD should create ZSET type")
    void testZAddCreatesZSetType() {
        zAddHandler.process(testSocket, List.of("myzset", "1", "member1"));
        
        CacheDto cache = RedisLocalMap.LOCAL_MAP.get("myzset");
        assertNotNull(cache);
        assertEquals(ValueType.ZSET, cache.getValueType());
    }

    @Test
    @DisplayName("ZADD with null list should throw exception")
    void testZAddWithNullList() {
        assertThrows(RuntimeException.class, () -> {
            zAddHandler.process(testSocket, null);
        });
    }

    @Test
    @DisplayName("ZADD with insufficient params should throw exception")
    void testZAddWithInsufficientParams() {
        assertThrows(RuntimeException.class, () -> {
            zAddHandler.process(testSocket, List.of("myzset", "1"));
        });
    }

    // ==================== ZRANGE Tests ====================
    
    @Test
    @DisplayName("ZRANGE should return members in score order")
    void testZRangeScoreOrder() {
        zAddHandler.process(testSocket, List.of("myzset", "3", "three"));
        zAddHandler.process(testSocket, List.of("myzset", "1", "one"));
        zAddHandler.process(testSocket, List.of("myzset", "2", "two"));
        
        String result = zRangeHandler.process(testSocket, List.of("myzset", "0", "-1"));
        
        // Should be ordered: one, two, three
        int indexOne = result.indexOf("one");
        int indexTwo = result.indexOf("two");
        int indexThree = result.indexOf("three");
        
        assertTrue(indexOne < indexTwo);
        assertTrue(indexTwo < indexThree);
    }

    @Test
    @DisplayName("ZRANGE 0 0 should return first member")
    void testZRangeFirstElement() {
        zAddHandler.process(testSocket, List.of("myzset", "1", "one"));
        zAddHandler.process(testSocket, List.of("myzset", "2", "two"));
        
        String result = zRangeHandler.process(testSocket, List.of("myzset", "0", "0"));
        
        assertTrue(result.startsWith("*1\r\n"));
        assertTrue(result.contains("one"));
        assertFalse(result.contains("two"));
    }

    @Test
    @DisplayName("ZRANGE on non-existent key should return empty array")
    void testZRangeNonExistentKey() {
        String result = zRangeHandler.process(testSocket, List.of("nonexistent", "0", "-1"));
        
        assertEquals("*0\r\n", result);
    }

    @Test
    @DisplayName("ZRANGE with negative indices")
    void testZRangeNegativeIndices() {
        zAddHandler.process(testSocket, List.of("myzset", "1", "one"));
        zAddHandler.process(testSocket, List.of("myzset", "2", "two"));
        zAddHandler.process(testSocket, List.of("myzset", "3", "three"));
        
        String result = zRangeHandler.process(testSocket, List.of("myzset", "-2", "-1"));
        
        assertTrue(result.startsWith("*2\r\n"));
        assertTrue(result.contains("two"));
        assertTrue(result.contains("three"));
    }

    @Test
    @DisplayName("ZRANGE with null list should throw exception")
    void testZRangeWithNullList() {
        assertThrows(RuntimeException.class, () -> {
            zRangeHandler.process(testSocket, null);
        });
    }

    // ==================== ZRANK Tests ====================
    
    @Test
    @DisplayName("ZRANK should return correct rank (0-based)")
    void testZRankReturnsCorrectRank() {
        zAddHandler.process(testSocket, List.of("myzset", "1", "one"));
        zAddHandler.process(testSocket, List.of("myzset", "2", "two"));
        zAddHandler.process(testSocket, List.of("myzset", "3", "three"));
        
        String rankOne = zRankHandler.process(testSocket, List.of("myzset", "one"));
        String rankTwo = zRankHandler.process(testSocket, List.of("myzset", "two"));
        String rankThree = zRankHandler.process(testSocket, List.of("myzset", "three"));
        
        assertEquals(":0\r\n", rankOne);
        assertEquals(":1\r\n", rankTwo);
        assertEquals(":2\r\n", rankThree);
    }

    @Test
    @DisplayName("ZRANK on non-existent member should return nil")
    void testZRankNonExistentMember() {
        zAddHandler.process(testSocket, List.of("myzset", "1", "one"));
        
        String result = zRankHandler.process(testSocket, List.of("myzset", "nonexistent"));
        
        assertEquals("$-1\r\n", result);
    }

    @Test
    @DisplayName("ZRANK on non-existent key should return nil")
    void testZRankNonExistentKey() {
        String result = zRankHandler.process(testSocket, List.of("nonexistent", "member"));
        
        assertEquals("$-1\r\n", result);
    }

    @Test
    @DisplayName("ZRANK with null list should throw exception")
    void testZRankWithNullList() {
        assertThrows(RuntimeException.class, () -> {
            zRankHandler.process(testSocket, null);
        });
    }

    // ==================== ZCARD Tests ====================
    
    @Test
    @DisplayName("ZCARD should return correct cardinality")
    void testZCardReturnsCardinality() {
        zAddHandler.process(testSocket, List.of("myzset", "1", "one"));
        zAddHandler.process(testSocket, List.of("myzset", "2", "two"));
        zAddHandler.process(testSocket, List.of("myzset", "3", "three"));
        
        String result = zCardHandler.process(testSocket, List.of("myzset"));
        
        assertEquals(":3\r\n", result);
    }

    @Test
    @DisplayName("ZCARD on non-existent key should return 0")
    void testZCardNonExistentKey() {
        String result = zCardHandler.process(testSocket, List.of("nonexistent"));
        
        assertEquals(":0\r\n", result);
    }

    @Test
    @DisplayName("ZCARD with null list should throw exception")
    void testZCardWithNullList() {
        assertThrows(RuntimeException.class, () -> {
            zCardHandler.process(testSocket, null);
        });
    }

    // ==================== ZREM Tests ====================
    
    @Test
    @DisplayName("ZREM should remove member and return 1")
    void testZRemRemovesMember() {
        zAddHandler.process(testSocket, List.of("myzset", "1", "one"));
        zAddHandler.process(testSocket, List.of("myzset", "2", "two"));
        
        String result = zRemHandler.process(testSocket, List.of("myzset", "one"));
        
        assertEquals(":1\r\n", result);
        
        // Verify cardinality
        String cardResult = zCardHandler.process(testSocket, List.of("myzset"));
        assertEquals(":1\r\n", cardResult);
    }

    @Test
    @DisplayName("ZREM on non-existent member should return 0")
    void testZRemNonExistentMember() {
        zAddHandler.process(testSocket, List.of("myzset", "1", "one"));
        
        String result = zRemHandler.process(testSocket, List.of("myzset", "nonexistent"));
        
        assertEquals(":0\r\n", result);
    }

    @Test
    @DisplayName("ZREM on non-existent key should return 0")
    void testZRemNonExistentKey() {
        String result = zRemHandler.process(testSocket, List.of("nonexistent", "member"));
        
        assertEquals(":0\r\n", result);
    }

    @Test
    @DisplayName("ZREM with null list should throw exception")
    void testZRemWithNullList() {
        assertThrows(RuntimeException.class, () -> {
            zRemHandler.process(testSocket, null);
        });
    }

    // ==================== ZSCORE Tests ====================
    
    @Test
    @DisplayName("ZSCORE should return member score")
    void testZScoreReturnsScore() {
        zAddHandler.process(testSocket, List.of("myzset", "3.14", "pi"));
        
        String result = zScoreHandler.process(testSocket, List.of("myzset", "pi"));
        
        assertTrue(result.contains("3.14"));
    }

    @Test
    @DisplayName("ZSCORE on non-existent member should return nil")
    void testZScoreNonExistentMember() {
        zAddHandler.process(testSocket, List.of("myzset", "1", "one"));
        
        String result = zScoreHandler.process(testSocket, List.of("myzset", "nonexistent"));
        
        assertEquals("$-1\r\n", result);
    }

    @Test
    @DisplayName("ZSCORE on non-existent key should return nil")
    void testZScoreNonExistentKey() {
        String result = zScoreHandler.process(testSocket, List.of("nonexistent", "member"));
        
        assertEquals("$-1\r\n", result);
    }

    @Test
    @DisplayName("ZSCORE with null list should throw exception")
    void testZScoreWithNullList() {
        assertThrows(RuntimeException.class, () -> {
            zScoreHandler.process(testSocket, null);
        });
    }

    // ==================== Integration Tests ====================
    
    @Test
    @DisplayName("Combined sorted set operations")
    void testCombinedOperations() {
        // Add members
        zAddHandler.process(testSocket, List.of("myzset", "1", "one"));
        zAddHandler.process(testSocket, List.of("myzset", "2", "two"));
        zAddHandler.process(testSocket, List.of("myzset", "3", "three"));
        
        // Check cardinality
        assertEquals(":3\r\n", zCardHandler.process(testSocket, List.of("myzset")));
        
        // Update score
        zAddHandler.process(testSocket, List.of("myzset", "0.5", "one"));
        
        // Verify rank changed (one should still be first with lower score)
        assertEquals(":0\r\n", zRankHandler.process(testSocket, List.of("myzset", "one")));
        
        // Remove member
        zRemHandler.process(testSocket, List.of("myzset", "two"));
        
        // Check cardinality after removal
        assertEquals(":2\r\n", zCardHandler.process(testSocket, List.of("myzset")));
    }

    @Test
    @DisplayName("Handlers should be registered correctly")
    void testHandlerRegistration() {
        assertTrue(handler.command.CommandHandler.HANDLER_MAP.containsKey("zadd"));
        assertTrue(handler.command.CommandHandler.HANDLER_MAP.containsKey("zrange"));
        assertTrue(handler.command.CommandHandler.HANDLER_MAP.containsKey("zrank"));
        assertTrue(handler.command.CommandHandler.HANDLER_MAP.containsKey("zcard"));
        assertTrue(handler.command.CommandHandler.HANDLER_MAP.containsKey("zrem"));
        assertTrue(handler.command.CommandHandler.HANDLER_MAP.containsKey("zscore"));
    }

    @Test
    @DisplayName("ZADD with same score should maintain insertion order or alphabetical")
    void testZAddSameScore() {
        zAddHandler.process(testSocket, List.of("myzset", "1", "b"));
        zAddHandler.process(testSocket, List.of("myzset", "1", "a"));
        zAddHandler.process(testSocket, List.of("myzset", "1", "c"));
        
        String result = zRangeHandler.process(testSocket, List.of("myzset", "0", "-1"));
        
        // All should be present
        assertTrue(result.contains("a"));
        assertTrue(result.contains("b"));
        assertTrue(result.contains("c"));
    }

    @Test
    @DisplayName("ZADD with negative score")
    void testZAddNegativeScore() {
        zAddHandler.process(testSocket, List.of("myzset", "-5", "negative"));
        zAddHandler.process(testSocket, List.of("myzset", "5", "positive"));
        
        String result = zRangeHandler.process(testSocket, List.of("myzset", "0", "-1"));
        
        // negative should come first
        int indexNeg = result.indexOf("negative");
        int indexPos = result.indexOf("positive");
        assertTrue(indexNeg < indexPos);
    }

    @Test
    @DisplayName("ZADD with floating point score")
    void testZAddFloatingPointScore() {
        zAddHandler.process(testSocket, List.of("myzset", "1.5", "one_point_five"));
        
        String result = zScoreHandler.process(testSocket, List.of("myzset", "one_point_five"));
        
        assertTrue(result.contains("1.5"));
    }
}
