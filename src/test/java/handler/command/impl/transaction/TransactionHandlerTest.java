package handler.command.impl.transaction;

import domain.JobDto;
import enums.JobType;
import handler.command.impl.TestHelper;
import handler.command.impl.core.IncrHandler;
import handler.command.impl.core.SetHandler;
import handler.job.JobHandler;
import org.junit.jupiter.api.*;
import service.RedisLocalMap;
import service.ServerUtils;

import java.net.Socket;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for Transaction Handlers (MultiHandler, ExecHandler, DiscardHandler)
 * 
 * Tests cover:
 * - MULTI starts a transaction (sets commandAtomic flag)
 * - MultiHandler.queueCommand() queues commands
 * - EXEC executes queued commands
 * - EXEC without MULTI returns error
 * - DISCARD discards queued commands
 * - DISCARD without MULTI returns error
 * 
 * Note: Individual handlers (SetHandler, IncrHandler, etc.) do NOT check for
 * transaction mode - they always execute immediately. Transaction behavior
 * is implemented by:
 * 1. MULTI sets the commandAtomic flag
 * 2. Commands are queued via MultiHandler.queueCommand() 
 * 3. EXEC processes the queue and executes commands
 */
@DisplayName("Transaction Handler Tests")
class TransactionHandlerTest {

    private MultiHandler multiHandler;
    private ExecHandler execHandler;
    private DiscardHandler discardHandler;
    private SetHandler setHandler;
    private IncrHandler incrHandler;
    private Socket testSocket;

    @BeforeEach
    void setUp() {
        multiHandler = new MultiHandler();
        execHandler = new ExecHandler();
        discardHandler = new DiscardHandler();
        setHandler = new SetHandler();
        incrHandler = new IncrHandler();
        
        multiHandler.register();
        execHandler.register();
        discardHandler.register();
        setHandler.register();
        incrHandler.register();
        
        testSocket = TestHelper.createTestSocket();
        
        // Register job for the socket
        String jobId = ServerUtils.formatIdFromSocket(testSocket);
        JobDto jobDto = new JobDto.Builder(JobType.RESP)
                .addSocket(testSocket)
                .addCommandDtoList()
                .build();
        JobHandler.JOB_MAP.put(jobId, jobDto);
        
        RedisLocalMap.LOCAL_MAP.clear();
    }

    @AfterEach
    void tearDown() throws Exception {
        RedisLocalMap.LOCAL_MAP.clear();
        JobHandler.JOB_MAP.clear();
    }

    @Test
    @DisplayName("MULTI should return OK")
    void testMultiReturnsOk() {
        String result = multiHandler.process(testSocket, List.of());
        
        assertEquals("+OK\r\n", result);
    }

    @Test
    @DisplayName("MULTI should set command atomic flag to true")
    void testMultiSetsAtomicFlag() {
        multiHandler.process(testSocket, List.of());
        
        String jobId = ServerUtils.formatIdFromSocket(testSocket);
        JobDto jobDto = JobHandler.JOB_MAP.get(jobId);
        assertTrue(jobDto.isCommandAtomic());
    }

    @Test
    @DisplayName("EXEC without MULTI should return error")
    void testExecWithoutMulti() {
        String result = execHandler.process(testSocket, List.of());
        
        assertTrue(result.contains("ERR EXEC without MULTI"));
    }

    @Test
    @DisplayName("DISCARD without MULTI should return error")
    void testDiscardWithoutMulti() {
        String result = discardHandler.process(testSocket, List.of());
        
        assertTrue(result.contains("ERR DISCARD without MULTI"));
    }

    @Test
    @DisplayName("MULTI then EXEC with empty queue should return empty array")
    void testMultiExecEmptyQueue() {
        multiHandler.process(testSocket, List.of());
        String result = execHandler.process(testSocket, List.of());
        
        assertEquals("*0\r\n", result);
    }

    @Test
    @DisplayName("MultiHandler.queueCommand() should return QUEUED")
    void testQueueCommandReturnsQueued() {
        // Start transaction
        multiHandler.process(testSocket, List.of());
        
        // Queue command
        String queueResult = MultiHandler.queueCommand(testSocket, List.of("set", "foo", "bar"));
        
        assertEquals("+QUEUED\r\n", queueResult);
    }

    @Test
    @DisplayName("MULTI, queue SET, queue INCR, EXEC should execute all commands")
    void testMultiExecWithQueuedCommands() {
        // Start transaction
        multiHandler.process(testSocket, List.of());
        
        // Queue commands using MultiHandler.queueCommand()
        String queueResult1 = MultiHandler.queueCommand(testSocket, List.of("set", "foo", "41"));
        String queueResult2 = MultiHandler.queueCommand(testSocket, List.of("incr", "foo"));
        
        assertEquals("+QUEUED\r\n", queueResult1);
        assertEquals("+QUEUED\r\n", queueResult2);
        
        // Execute transaction
        String result = execHandler.process(testSocket, List.of());
        
        // Result should contain results from both commands
        assertTrue(result.startsWith("*2\r\n"));
    }

    @Test
    @DisplayName("MULTI, queue SET, DISCARD should discard commands")
    void testMultiSetDiscard() {
        // Start transaction
        multiHandler.process(testSocket, List.of());
        
        // Queue command
        MultiHandler.queueCommand(testSocket, List.of("set", "foo", "bar"));
        
        // Discard
        String result = discardHandler.process(testSocket, List.of());
        
        assertEquals("+OK\r\n", result);
        
        // Verify atomic flag is reset
        String jobId = ServerUtils.formatIdFromSocket(testSocket);
        JobDto jobDto = JobHandler.JOB_MAP.get(jobId);
        assertFalse(jobDto.isCommandAtomic());
        assertTrue(jobDto.getCommandDtoList().isEmpty());
    }

    @Test
    @DisplayName("DISCARD after DISCARD should return error")
    void testDoubleDiscard() {
        // Start transaction
        multiHandler.process(testSocket, List.of());
        
        // Queue command
        MultiHandler.queueCommand(testSocket, List.of("set", "foo", "bar"));
        
        // First discard
        discardHandler.process(testSocket, List.of());
        
        // Second discard should fail
        String result = discardHandler.process(testSocket, List.of());
        
        assertTrue(result.contains("ERR DISCARD without MULTI"));
    }

    @Test
    @DisplayName("MULTI with null list should throw exception")
    void testMultiWithNullList() {
        assertThrows(RuntimeException.class, () -> {
            multiHandler.process(testSocket, null);
        });
    }

    @Test
    @DisplayName("EXEC with null list should throw exception")
    void testExecWithNullList() {
        multiHandler.process(testSocket, List.of());
        assertThrows(RuntimeException.class, () -> {
            execHandler.process(testSocket, null);
        });
    }

    @Test
    @DisplayName("DISCARD with null list should throw exception")
    void testDiscardWithNullList() {
        multiHandler.process(testSocket, List.of());
        assertThrows(RuntimeException.class, () -> {
            discardHandler.process(testSocket, null);
        });
    }

    @Test
    @DisplayName("Handlers should be registered correctly")
    void testHandlerRegistration() {
        assertTrue(handler.command.CommandHandler.HANDLER_MAP.containsKey("multi"));
        assertTrue(handler.command.CommandHandler.HANDLER_MAP.containsKey("exec"));
        assertTrue(handler.command.CommandHandler.HANDLER_MAP.containsKey("discard"));
    }

    @Test
    @DisplayName("EXEC should reset atomic flag after execution")
    void testExecResetsAtomicFlag() {
        // Start transaction
        multiHandler.process(testSocket, List.of());
        
        // Execute empty transaction
        execHandler.process(testSocket, List.of());
        
        // Verify atomic flag is reset
        String jobId = ServerUtils.formatIdFromSocket(testSocket);
        JobDto jobDto = JobHandler.JOB_MAP.get(jobId);
        assertFalse(jobDto.isCommandAtomic());
    }

    @Test
    @DisplayName("Multiple transactions in sequence should work correctly")
    void testMultipleTransactions() {
        // First transaction
        multiHandler.process(testSocket, List.of());
        MultiHandler.queueCommand(testSocket, List.of("set", "key1", "value1"));
        execHandler.process(testSocket, List.of());
        
        // Second transaction
        multiHandler.process(testSocket, List.of());
        MultiHandler.queueCommand(testSocket, List.of("set", "key2", "value2"));
        execHandler.process(testSocket, List.of());
        
        // Both keys should be set
        assertTrue(RedisLocalMap.LOCAL_MAP.containsKey("key1"));
        assertTrue(RedisLocalMap.LOCAL_MAP.containsKey("key2"));
    }

    @Test
    @DisplayName("Direct handler calls do NOT queue (they execute immediately)")
    void testDirectHandlerCallsExecuteImmediately() {
        // Start transaction
        multiHandler.process(testSocket, List.of());
        
        // Calling handler directly still executes (doesn't queue)
        String setResult = setHandler.process(testSocket, List.of("foo", "bar"));
        
        // Returns +OK, not +QUEUED (individual handlers don't check for transaction mode)
        assertEquals("+OK\r\n", setResult);
        
        // Value was immediately set
        assertTrue(RedisLocalMap.LOCAL_MAP.containsKey("foo"));
    }
}
