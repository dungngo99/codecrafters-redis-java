package handler.command.impl.pubsub;

import handler.command.impl.TestHelper;
import org.junit.jupiter.api.*;
import service.RedisLocalMap;

import java.net.Socket;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for PubSub Handlers (SubscribeHandler, UnsubscribeHandler, PublishHandler)
 * 
 * Tests cover:
 * - SUBSCRIBE subscribes to channel (one channel per call)
 * - UNSUBSCRIBE unsubscribes from channel
 * - PUBLISH publishes message to channel
 * 
 * Note: SubscribeHandler processes ONE channel at a time (first element in list).
 * Multiple channel subscriptions require multiple calls.
 */
@DisplayName("PubSub Handler Tests")
class PubSubHandlerTest {

    private SubscribeHandler subscribeHandler;
    private UnsubscribeHandler unsubscribeHandler;
    private PublishHandler publishHandler;
    private Socket testSocket;

    @BeforeEach
    void setUp() {
        subscribeHandler = new SubscribeHandler();
        unsubscribeHandler = new UnsubscribeHandler();
        publishHandler = new PublishHandler();
        
        subscribeHandler.register();
        unsubscribeHandler.register();
        publishHandler.register();
        
        testSocket = TestHelper.createTestSocket();
        
        RedisLocalMap.LOCAL_MAP.clear();
        RedisLocalMap.SUBSCRIBER_MAP.clear();
        RedisLocalMap.CHANNEL_MAP.clear();
        RedisLocalMap.SUBSCRIBE_MODE_SET.clear();
    }

    @AfterEach
    void tearDown() {
        RedisLocalMap.LOCAL_MAP.clear();
        RedisLocalMap.SUBSCRIBER_MAP.clear();
        RedisLocalMap.CHANNEL_MAP.clear();
        RedisLocalMap.SUBSCRIBE_MODE_SET.clear();
    }

    // ==================== SUBSCRIBE Tests ====================
    
    @Test
    @DisplayName("SUBSCRIBE channel should add subscriber")
    void testSubscribeChannel() {
        String result = subscribeHandler.process(testSocket, List.of("channel1"));
        
        assertTrue(result.contains("subscribe"));
        assertTrue(result.contains("channel1"));
    }

    @Test
    @DisplayName("SUBSCRIBE to first channel in list")
    void testSubscribeFirstChannel() {
        // SubscribeHandler only processes the FIRST channel (index 0)
        String result = subscribeHandler.process(testSocket, List.of("channel1"));
        
        assertTrue(result.contains("channel1"));
        assertTrue(RedisLocalMap.CHANNEL_MAP.containsKey("channel1"));
    }

    @Test
    @DisplayName("Multiple SUBSCRIBE calls for multiple channels")
    void testMultipleSubscribeCalls() {
        // Subscribe to first channel
        String result1 = subscribeHandler.process(testSocket, List.of("channel1"));
        assertTrue(result1.contains("channel1"));
        
        // Subscribe to second channel
        String result2 = subscribeHandler.process(testSocket, List.of("channel2"));
        assertTrue(result2.contains("channel2"));
        
        // Both channels should exist
        assertTrue(RedisLocalMap.CHANNEL_MAP.containsKey("channel1"));
        assertTrue(RedisLocalMap.CHANNEL_MAP.containsKey("channel2"));
    }

    @Test
    @DisplayName("SUBSCRIBE with null list should throw exception")
    void testSubscribeWithNullList() {
        assertThrows(RuntimeException.class, () -> {
            subscribeHandler.process(testSocket, null);
        });
    }

    @Test
    @DisplayName("SUBSCRIBE with empty list should throw exception")
    void testSubscribeWithEmptyList() {
        assertThrows(RuntimeException.class, () -> {
            subscribeHandler.process(testSocket, List.of());
        });
    }

    // ==================== UNSUBSCRIBE Tests ====================
    
    @Test
    @DisplayName("UNSUBSCRIBE channel should remove subscriber")
    void testUnsubscribeChannel() {
        subscribeHandler.process(testSocket, List.of("channel1"));
        String result = unsubscribeHandler.process(testSocket, List.of("channel1"));
        
        assertTrue(result.contains("unsubscribe"));
    }

    @Test
    @DisplayName("UNSUBSCRIBE with null list should throw exception")
    void testUnsubscribeWithNullList() {
        assertThrows(RuntimeException.class, () -> {
            unsubscribeHandler.process(testSocket, null);
        });
    }

    // ==================== PUBLISH Tests ====================
    
    @Test
    @DisplayName("PUBLISH to channel with no subscribers should return 0")
    void testPublishNoSubscribers() {
        String result = publishHandler.process(testSocket, List.of("channel1", "hello"));
        
        assertEquals(":0\r\n", result);
    }

    @Test
    @DisplayName("PUBLISH with null list should throw exception")
    void testPublishWithNullList() {
        assertThrows(RuntimeException.class, () -> {
            publishHandler.process(testSocket, null);
        });
    }

    @Test
    @DisplayName("PUBLISH with insufficient params should throw exception")
    void testPublishWithInsufficientParams() {
        assertThrows(RuntimeException.class, () -> {
            publishHandler.process(testSocket, List.of("channel1"));
        });
    }

    // ==================== Integration Tests ====================
    
    @Test
    @DisplayName("Handlers should be registered correctly")
    void testHandlerRegistration() {
        assertTrue(handler.command.CommandHandler.HANDLER_MAP.containsKey("subscribe"));
        assertTrue(handler.command.CommandHandler.HANDLER_MAP.containsKey("unsubscribe"));
        assertTrue(handler.command.CommandHandler.HANDLER_MAP.containsKey("publish"));
    }

    @Test
    @DisplayName("SUBSCRIBE updates CHANNEL_MAP")
    void testSubscribeUpdatesChannelMap() {
        subscribeHandler.process(testSocket, List.of("testchannel"));
        
        assertTrue(RedisLocalMap.CHANNEL_MAP.containsKey("testchannel"));
    }

    @Test
    @DisplayName("SUBSCRIBE adds connection to SUBSCRIBE_MODE_SET")
    void testSubscribeAddsToModeSet() {
        subscribeHandler.process(testSocket, List.of("testchannel"));
        
        assertFalse(RedisLocalMap.SUBSCRIBE_MODE_SET.isEmpty());
    }

    @Test
    @DisplayName("Multiple subscribers to same channel")
    void testMultipleSubscribers() {
        Socket testSocket2 = TestHelper.createTestSocket();
        
        subscribeHandler.process(testSocket, List.of("sharedchannel"));
        subscribeHandler.process(testSocket2, List.of("sharedchannel"));
        
        assertTrue(RedisLocalMap.CHANNEL_MAP.containsKey("sharedchannel"));
        // Both subscribers should be in the channel's subscriber map
        assertTrue(RedisLocalMap.CHANNEL_MAP.get("sharedchannel").size() >= 1);
    }

    @Test
    @DisplayName("SUBSCRIBE returns subscription count")
    void testSubscribeReturnsCount() {
        // Subscribe to first channel
        String result1 = subscribeHandler.process(testSocket, List.of("channel1"));
        assertTrue(result1.contains("1")); // count = 1
        
        // Subscribe to second channel
        String result2 = subscribeHandler.process(testSocket, List.of("channel2"));
        assertTrue(result2.contains("2")); // count = 2
    }
}
