package handler.command.impl.core;

import domain.CacheDto;
import domain.StreamDto;
import enums.ValueType;
import handler.command.impl.TestHelper;
import org.junit.jupiter.api.*;
import service.RedisLocalMap;

import java.net.Socket;
import java.util.List;
import java.util.concurrent.LinkedBlockingDeque;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for TypeHandler
 * 
 * Tests cover:
 * - TYPE on string key returns "string"
 * - TYPE on stream key returns "stream"
 * - TYPE on non-existent key returns "none"
 * - Invalid parameters
 */
@DisplayName("TypeHandler Tests")
class TypeHandlerTest {

    private TypeHandler typeHandler;
    private SetHandler setHandler;
    private Socket testSocket;

    @BeforeEach
    void setUp() {
        typeHandler = new TypeHandler();
        setHandler = new SetHandler();
        typeHandler.register();
        setHandler.register();
        testSocket = TestHelper.createTestSocket();
        
        RedisLocalMap.LOCAL_MAP.clear();
    }

    @AfterEach
    void tearDown() {
        RedisLocalMap.LOCAL_MAP.clear();
    }

    @Test
    @DisplayName("TYPE on string key should return 'string'")
    void testTypeOnStringKey() {
        setHandler.process(testSocket, List.of("some_key", "foo"));
        String result = typeHandler.process(testSocket, List.of("some_key"));
        
        assertEquals("+string\r\n", result);
    }

    @Test
    @DisplayName("TYPE on stream key should return 'stream'")
    void testTypeOnStreamKey() {
        CacheDto cache = new CacheDto();
        cache.setValueType(ValueType.STREAM);
        cache.setValue(new StreamDto());
        RedisLocalMap.LOCAL_MAP.put("stream_key", cache);
        
        String result = typeHandler.process(testSocket, List.of("stream_key"));
        
        assertEquals("+stream\r\n", result);
    }

    @Test
    @DisplayName("TYPE on non-existent key should return 'none'")
    void testTypeOnNonExistentKey() {
        String result = typeHandler.process(testSocket, List.of("nonexistent"));
        
        assertEquals("+none\r\n", result);
    }

    @Test
    @DisplayName("TYPE with null list should throw exception")
    void testTypeWithNullList() {
        assertThrows(RuntimeException.class, () -> {
            typeHandler.process(testSocket, null);
        });
    }

    @Test
    @DisplayName("TYPE with empty list should throw exception")
    void testTypeWithEmptyList() {
        assertThrows(RuntimeException.class, () -> {
            typeHandler.process(testSocket, List.of());
        });
    }

    @Test
    @DisplayName("TYPE on list key should return 'none' (not supported)")
    void testTypeOnListKey() {
        CacheDto cache = new CacheDto();
        cache.setValueType(ValueType.LIST);
        cache.setValue(new LinkedBlockingDeque<>());
        RedisLocalMap.LOCAL_MAP.put("list_key", cache);
        
        String result = typeHandler.process(testSocket, List.of("list_key"));
        
        // LIST type is not currently supported by TypeHandler
        assertEquals("+none\r\n", result);
    }

    @Test
    @DisplayName("TYPE on zset key should return 'none' (not supported)")
    void testTypeOnZSetKey() {
        CacheDto cache = new CacheDto();
        cache.setValueType(ValueType.ZSET);
        cache.setValue(new domain.ZSet());
        RedisLocalMap.LOCAL_MAP.put("zset_key", cache);
        
        String result = typeHandler.process(testSocket, List.of("zset_key"));
        
        // ZSET type is not currently supported by TypeHandler
        assertEquals("+none\r\n", result);
    }

    @Test
    @DisplayName("TYPE handler should be registered correctly")
    void testHandlerRegistration() {
        assertTrue(handler.command.CommandHandler.HANDLER_MAP.containsKey("type"));
        assertSame(typeHandler, handler.command.CommandHandler.HANDLER_MAP.get("type"));
    }

    @Test
    @DisplayName("Multiple TYPE operations on different key types")
    void testMultipleTypeOperations() {
        // Setup string key
        setHandler.process(testSocket, List.of("string_key", "value"));
        
        // Setup stream key
        CacheDto streamCache = new CacheDto();
        streamCache.setValueType(ValueType.STREAM);
        streamCache.setValue(new StreamDto());
        RedisLocalMap.LOCAL_MAP.put("stream_key", streamCache);
        
        assertEquals("+string\r\n", typeHandler.process(testSocket, List.of("string_key")));
        assertEquals("+stream\r\n", typeHandler.process(testSocket, List.of("stream_key")));
        assertEquals("+none\r\n", typeHandler.process(testSocket, List.of("missing_key")));
    }
}
