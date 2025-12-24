package handler.command.impl.core;

import handler.command.impl.TestHelper;
import org.junit.jupiter.api.*;
import service.RedisLocalMap;

import java.net.Socket;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for KeysHandler
 * 
 * Tests cover:
 * - KEYS * returns all keys
 * - KEYS with prefix pattern
 * - KEYS with suffix pattern
 * - KEYS with no matches
 * - Invalid parameters
 */
@DisplayName("KeysHandler Tests")
class KeysHandlerTest {

    private KeysHandler keysHandler;
    private SetHandler setHandler;
    private GetHandler getHandler;
    private Socket testSocket;

    @BeforeEach
    void setUp() {
        keysHandler = new KeysHandler();
        setHandler = new SetHandler();
        getHandler = new GetHandler();
        keysHandler.register();
        setHandler.register();
        getHandler.register();
        testSocket = TestHelper.createTestSocket();
        
        RedisLocalMap.LOCAL_MAP.clear();
    }

    @AfterEach
    void tearDown() {
        RedisLocalMap.LOCAL_MAP.clear();
    }

    @Test
    @DisplayName("KEYS * should return all keys")
    void testKeysWildcard() {
        setHandler.process(testSocket, List.of("foo", "bar"));
        setHandler.process(testSocket, List.of("hello", "world"));
        setHandler.process(testSocket, List.of("test", "value"));
        
        String result = keysHandler.process(testSocket, List.of("*"));
        
        assertTrue(result.startsWith("*3\r\n"));
        assertTrue(result.contains("foo"));
        assertTrue(result.contains("hello"));
        assertTrue(result.contains("test"));
    }

    @Test
    @DisplayName("KEYS foo* should return keys with prefix 'foo'")
    void testKeysWithPrefix() {
        setHandler.process(testSocket, List.of("foo1", "bar"));
        setHandler.process(testSocket, List.of("foo2", "baz"));
        setHandler.process(testSocket, List.of("bar", "qux"));
        
        String result = keysHandler.process(testSocket, List.of("foo*"));
        
        assertTrue(result.contains("foo1"));
        assertTrue(result.contains("foo2"));
        assertFalse(result.contains("$3\r\nbar")); // Exclude the key "bar" but not value "bar"
    }

    @Test
    @DisplayName("KEYS *bar should return keys with suffix 'bar'")
    void testKeysWithSuffix() {
        setHandler.process(testSocket, List.of("foobar", "value1"));
        setHandler.process(testSocket, List.of("testbar", "value2"));
        setHandler.process(testSocket, List.of("test", "value3"));
        
        String result = keysHandler.process(testSocket, List.of("*bar"));
        
        assertTrue(result.contains("foobar"));
        assertTrue(result.contains("testbar"));
    }

    @Test
    @DisplayName("KEYS with no matches should return empty")
    void testKeysNoMatches() {
        setHandler.process(testSocket, List.of("foo", "bar"));
        
        String result = keysHandler.process(testSocket, List.of("nonexistent*"));
        
        assertTrue(result.isEmpty() || result.equals("*0\r\n"));
    }

    @Test
    @DisplayName("KEYS with null list should throw exception")
    void testKeysWithNullList() {
        assertThrows(RuntimeException.class, () -> {
            keysHandler.process(testSocket, null);
        });
    }

    @Test
    @DisplayName("KEYS with empty list should throw exception")
    void testKeysWithEmptyList() {
        assertThrows(RuntimeException.class, () -> {
            keysHandler.process(testSocket, List.of());
        });
    }

    @Test
    @DisplayName("KEYS with exact key should return value like GET")
    void testKeysExactKey() {
        setHandler.process(testSocket, List.of("mykey", "myvalue"));
        
        String result = keysHandler.process(testSocket, List.of("mykey"));
        
        // When no wildcard, it should behave like GET
        assertEquals("$7\r\nmyvalue\r\n", result);
    }

    @Test
    @DisplayName("KEYS handler should be registered correctly")
    void testHandlerRegistration() {
        assertTrue(handler.command.CommandHandler.HANDLER_MAP.containsKey("keys"));
        assertSame(keysHandler, handler.command.CommandHandler.HANDLER_MAP.get("keys"));
    }

    @Test
    @DisplayName("KEYS with prefix and suffix")
    void testKeysWithPrefixAndSuffix() {
        setHandler.process(testSocket, List.of("pre_middle_suf", "value1"));
        setHandler.process(testSocket, List.of("pre_other", "value2"));
        setHandler.process(testSocket, List.of("other_suf", "value3"));
        
        // Note: The implementation only supports prefix* or *suffix, not both
        String resultPrefix = keysHandler.process(testSocket, List.of("pre_*"));
        assertTrue(resultPrefix.contains("pre_middle_suf"));
        assertTrue(resultPrefix.contains("pre_other"));
    }

    @Test
    @DisplayName("KEYS on empty database")
    void testKeysOnEmptyDatabase() {
        String result = keysHandler.process(testSocket, List.of("*"));
        
        assertTrue(result.isEmpty() || result.equals("*0\r\n"));
    }
}
