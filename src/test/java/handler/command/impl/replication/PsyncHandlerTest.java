package handler.command.impl.replication;

import constants.OutputConstants;
import handler.command.impl.TestHelper;
import handler.job.JobHandler;
import org.junit.jupiter.api.*;
import replication.MasterManager;
import service.SystemPropHelper;

import java.io.IOException;
import java.net.Socket;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for PsyncHandler
 * 
 * Tests cover:
 * - PSYNC command returns FULLRESYNC response
 * - PSYNC registers replica connection
 * - PSYNC increments connected replica count
 * - Invalid parameters handling
 */
@DisplayName("PsyncHandler Tests")
class PsyncHandlerTest {

    private PsyncHandler psyncHandler;
    private Socket testSocket;

    @BeforeEach
    void setUp() throws IOException {
        psyncHandler = new PsyncHandler();
        psyncHandler.register();
        
        testSocket = TestHelper.createTestSocket();
        
        // Clear any existing configuration
        System.clearProperty(OutputConstants.REDIS_SERVER_ROLE_TYPE);
        System.clearProperty(OutputConstants.MASTER_REPLID);
        
        // Clear job handlers
        JobHandler.JOB_MAP.clear();
        
        // Register as master node
        String masterNodeId = SystemPropHelper.getSetMasterNodeId();
        MasterManager.registerMasterManager(masterNodeId, "localhost", 6379);
    }

    @AfterEach
    void tearDown() throws Exception {
        System.clearProperty(OutputConstants.REDIS_SERVER_ROLE_TYPE);
        System.clearProperty(OutputConstants.MASTER_REPLID);
        JobHandler.JOB_MAP.clear();
        MasterManager.getMasterNodeMap().clear();
    }

    @Test
    @DisplayName("PSYNC ? -1 should return FULLRESYNC response")
    void testPsyncReturnsFullResync() {
        String result = psyncHandler.process(testSocket, List.of("?", "-1"));
        
        assertTrue(result.startsWith("+FULLRESYNC"));
        assertTrue(result.contains(" 0")); // offset should be 0
        assertTrue(result.endsWith("\r\n"));
    }

    @Test
    @DisplayName("PSYNC should contain master_replid in response")
    void testPsyncContainsMasterReplId() {
        String result = psyncHandler.process(testSocket, List.of("?", "-1"));
        
        // Extract the master_replid from response: +FULLRESYNC <master_replid> <offset>
        String[] parts = result.replace("+", "").replace("\r\n", "").split(" ");
        assertEquals(3, parts.length);
        assertEquals("FULLRESYNC", parts[0]);
        assertEquals(40, parts[1].length()); // master_replid is 40 chars
        assertEquals("0", parts[2]); // offset is 0
    }

    @Test
    @DisplayName("PSYNC with null list should throw exception")
    void testPsyncWithNullList() {
        assertThrows(IllegalArgumentException.class, () -> {
            psyncHandler.process(testSocket, null);
        });
    }

    @Test
    @DisplayName("PSYNC with empty list should throw exception")
    void testPsyncWithEmptyList() {
        assertThrows(IllegalArgumentException.class, () -> {
            psyncHandler.process(testSocket, List.of());
        });
    }

    @Test
    @DisplayName("PSYNC handler should be registered correctly")
    void testHandlerRegistration() {
        assertTrue(handler.command.CommandHandler.HANDLER_MAP.containsKey("psync"));
        assertSame(psyncHandler, handler.command.CommandHandler.HANDLER_MAP.get("psync"));
    }

    @Test
    @DisplayName("PSYNC should register job for replica")
    void testPsyncRegistersJob() {
        psyncHandler.process(testSocket, List.of("?", "-1"));
        
        String jobId = "127.0.0.1::12345";
        assertTrue(JobHandler.JOB_MAP.containsKey(jobId));
    }

    @Test
    @DisplayName("Multiple PSYNC from different replicas should work")
    void testMultiplePsyncFromDifferentReplicas() throws IOException {
        // First replica
        String result1 = psyncHandler.process(testSocket, List.of("?", "-1"));
        assertTrue(result1.startsWith("+FULLRESYNC"));
        
        // Second replica - use another test socket
        Socket testSocket2 = TestHelper.createTestSocket();
        String result2 = psyncHandler.process(testSocket2, List.of("?", "-1"));
        assertTrue(result2.startsWith("+FULLRESYNC"));
    }

    @Test
    @DisplayName("PSYNC response should use consistent master_replid")
    void testPsyncConsistentMasterReplId() throws IOException {
        String result1 = psyncHandler.process(testSocket, List.of("?", "-1"));
        
        // Create second socket
        Socket testSocket2 = TestHelper.createTestSocket();
        String result2 = psyncHandler.process(testSocket2, List.of("?", "-1"));
        
        // Extract master_replid from both responses
        String replId1 = result1.replace("+", "").replace("\r\n", "").split(" ")[1];
        String replId2 = result2.replace("+", "").replace("\r\n", "").split(" ")[1];
        
        // Should be the same master_replid
        assertEquals(replId1, replId2);
    }

    @Test
    @DisplayName("PSYNC with specific replication ID should work")
    void testPsyncWithSpecificReplId() {
        // PSYNC with specific replication ID (not ?)
        String result = psyncHandler.process(testSocket, List.of("abc123", "0"));
        
        assertTrue(result.startsWith("+FULLRESYNC"));
    }

    @Test
    @DisplayName("PSYNC offset should be 0 for initial sync")
    void testPsyncOffsetIsZero() {
        String result = psyncHandler.process(testSocket, List.of("?", "-1"));
        
        String[] parts = result.replace("+", "").replace("\r\n", "").split(" ");
        assertEquals("0", parts[2]);
    }
}
