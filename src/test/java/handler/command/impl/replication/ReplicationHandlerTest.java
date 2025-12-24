package handler.command.impl.replication;

import handler.command.impl.TestHelper;
import org.junit.jupiter.api.*;
import replication.MasterManager;
import service.RedisLocalMap;
import service.SystemPropHelper;

import java.net.Socket;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for Replication Handlers (ReplConfigHandler)
 * 
 * Tests cover:
 * - REPLCONF LISTENING-PORT
 * - REPLCONF CAPA
 * - REPLCONF GETACK
 * - REPLCONF ACK (requires master node setup)
 */
@DisplayName("Replication Handler Tests")
class ReplicationHandlerTest {

    private ReplConfigHandler replConfigHandler;
    private Socket testSocket;

    @BeforeEach
    void setUp() {
        replConfigHandler = new ReplConfigHandler();
        replConfigHandler.register();
        
        testSocket = TestHelper.createTestSocket();
        
        RedisLocalMap.LOCAL_MAP.clear();
    }

    @AfterEach
    void tearDown() {
        RedisLocalMap.LOCAL_MAP.clear();
        MasterManager.getMasterNodeMap().clear();
    }

    @Test
    @DisplayName("REPLCONF handler should be registered correctly")
    void testHandlerRegistration() {
        assertTrue(handler.command.CommandHandler.HANDLER_MAP.containsKey("replconf"));
        assertSame(replConfigHandler, handler.command.CommandHandler.HANDLER_MAP.get("replconf"));
    }

    @Test
    @DisplayName("REPLCONF LISTENING-PORT should return OK")
    void testReplconfListeningPort() {
        String result = replConfigHandler.process(testSocket, List.of("LISTENING-PORT", "6380"));
        
        assertEquals("+OK\r\n", result);
    }

    @Test
    @DisplayName("REPLCONF CAPA should return OK")
    void testReplconfCapa() {
        String result = replConfigHandler.process(testSocket, List.of("CAPA", "psync2"));
        
        assertEquals("+OK\r\n", result);
    }

    @Test
    @DisplayName("REPLCONF with null list should throw exception")
    void testReplconfWithNullList() {
        assertThrows(RuntimeException.class, () -> {
            replConfigHandler.process(testSocket, null);
        });
    }

    @Test
    @DisplayName("REPLCONF with insufficient params should throw exception")
    void testReplconfWithInsufficientParams() {
        assertThrows(RuntimeException.class, () -> {
            replConfigHandler.process(testSocket, List.of("LISTENING-PORT"));
        });
    }

    @Test
    @DisplayName("REPLCONF GETACK should return REPLCONF ACK response")
    void testReplconfGetAck() {
        String result = replConfigHandler.process(testSocket, List.of("GETACK", "*"));
        
        // Should return REPLCONF ACK <offset>
        assertTrue(result.contains("REPLCONF") || result.contains("replconf"));
        assertTrue(result.contains("ACK") || result.contains("ack"));
    }

    @Test
    @DisplayName("REPLCONF ACK with master setup should return empty")
    void testReplconfAckWithMaster() {
        // Setup as master node first
        String masterNodeId = SystemPropHelper.getSetMasterNodeId();
        MasterManager.registerMasterManager(masterNodeId, "localhost", 6379);
        
        String result = replConfigHandler.process(testSocket, List.of("ACK", "0"));
        
        // ACK returns empty string
        assertEquals("", result);
    }

    @Test
    @DisplayName("REPLCONF with lowercase command should work")
    void testReplconfLowercase() {
        String result = replConfigHandler.process(testSocket, List.of("listening-port", "6380"));
        
        assertEquals("+OK\r\n", result);
    }

    @Test
    @DisplayName("REPLCONF with unknown subcommand should throw exception")
    void testReplconfUnknownSubcommand() {
        assertThrows(RuntimeException.class, () -> {
            replConfigHandler.process(testSocket, List.of("UNKNOWN", "value"));
        });
    }
}
