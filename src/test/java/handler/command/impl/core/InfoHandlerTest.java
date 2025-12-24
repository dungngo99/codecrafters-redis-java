package handler.command.impl.core;

import constants.OutputConstants;
import handler.command.impl.TestHelper;
import org.junit.jupiter.api.*;

import java.net.Socket;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for InfoHandler
 * 
 * Tests cover:
 * - INFO replication for master node
 * - INFO replication for replica node
 * - Invalid subcommands
 * - Invalid parameters
 */
@DisplayName("InfoHandler Tests")
class InfoHandlerTest {

    private InfoHandler infoHandler;
    private Socket testSocket;

    @BeforeEach
    void setUp() {
        infoHandler = new InfoHandler();
        infoHandler.register();
        testSocket = TestHelper.createTestSocket();
        
        // Clear any existing role configuration
        System.clearProperty(OutputConstants.REDIS_SERVER_ROLE_TYPE);
        System.clearProperty(OutputConstants.REDIS_SERVER_REPLICA_OF);
        System.clearProperty(OutputConstants.MASTER_REPLID);
    }

    @AfterEach
    void tearDown() {
        System.clearProperty(OutputConstants.REDIS_SERVER_ROLE_TYPE);
        System.clearProperty(OutputConstants.REDIS_SERVER_REPLICA_OF);
        System.clearProperty(OutputConstants.MASTER_REPLID);
    }

    @Test
    @DisplayName("INFO replication on master node should return master role")
    void testInfoReplicationMaster() {
        String result = infoHandler.process(testSocket, List.of("replication"));
        
        assertTrue(result.startsWith("$"));
        assertTrue(result.contains("role:master"));
        assertTrue(result.contains("master_replid:"));
        assertTrue(result.contains("master_repl_offset:0"));
    }

    @Test
    @DisplayName("INFO replication on replica node should return slave role")
    void testInfoReplicationReplica() {
        System.setProperty(OutputConstants.REDIS_SERVER_REPLICA_OF, "localhost 6379");
        
        String result = infoHandler.process(testSocket, List.of("replication"));
        
        assertTrue(result.startsWith("$"));
        assertTrue(result.contains("role:slave"));
        assertTrue(result.contains("master_replid:"));
        assertTrue(result.contains("master_repl_offset:0"));
    }

    @Test
    @DisplayName("INFO with null list should throw exception")
    void testInfoWithNullList() {
        assertThrows(RuntimeException.class, () -> {
            infoHandler.process(testSocket, null);
        });
    }

    @Test
    @DisplayName("INFO with empty list should throw exception")
    void testInfoWithEmptyList() {
        assertThrows(RuntimeException.class, () -> {
            infoHandler.process(testSocket, List.of());
        });
    }

    @Test
    @DisplayName("INFO with invalid subcommand should throw exception")
    void testInfoWithInvalidSubcommand() {
        assertThrows(RuntimeException.class, () -> {
            infoHandler.process(testSocket, List.of("invalid"));
        });
    }

    @Test
    @DisplayName("INFO replication is case insensitive")
    void testInfoReplicationCaseInsensitive() {
        String result1 = infoHandler.process(testSocket, List.of("REPLICATION"));
        String result2 = infoHandler.process(testSocket, List.of("Replication"));
        String result3 = infoHandler.process(testSocket, List.of("replication"));
        
        assertTrue(result1.contains("role:"));
        assertTrue(result2.contains("role:"));
        assertTrue(result3.contains("role:"));
    }

    @Test
    @DisplayName("INFO handler should be registered correctly")
    void testHandlerRegistration() {
        assertTrue(handler.command.CommandHandler.HANDLER_MAP.containsKey("info"));
        assertSame(infoHandler, handler.command.CommandHandler.HANDLER_MAP.get("info"));
    }

    @Test
    @DisplayName("INFO replication should contain master_replid of 40 characters")
    void testInfoReplicationMasterReplIdLength() {
        String result = infoHandler.process(testSocket, List.of("replication"));
        
        // Extract master_replid value
        String[] lines = result.split("\r\n");
        String masterReplId = null;
        for (String line : lines) {
            if (line.contains("master_replid:")) {
                masterReplId = line.split(":")[1];
                break;
            }
        }
        
        assertNotNull(masterReplId);
        assertEquals(40, masterReplId.length());
    }
}
