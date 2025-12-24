package handler.command.impl;

import handler.command.impl.core.*;
import handler.command.impl.stream.XAddHandler;
import handler.command.impl.transaction.DiscardHandler;
import handler.command.impl.transaction.ExecHandler;
import handler.command.impl.transaction.MultiHandler;
import handler.job.JobHandler;
import domain.JobDto;
import enums.JobType;
import org.junit.jupiter.api.*;
import service.RedisLocalMap;
import service.ServerUtils;

import java.net.Socket;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for CommandHandler implementations
 * 
 * Tests cover multi-step scenarios from the user's examples:
 * - SET and GET operations
 * - SET with PX expiry
 * - INFO replication
 * - TYPE command
 * - INCR operations
 * - MULTI/EXEC/DISCARD transactions (using MultiHandler.queueCommand())
 * 
 * Note: Transaction queueing is done via MultiHandler.queueCommand(),
 * not by calling individual handlers directly.
 */
@DisplayName("CommandHandler Integration Tests")
class CommandHandlerIntegrationTest {

    private SetHandler setHandler;
    private GetHandler getHandler;
    private IncrHandler incrHandler;
    private TypeHandler typeHandler;
    private InfoHandler infoHandler;
    private XAddHandler xAddHandler;
    private MultiHandler multiHandler;
    private ExecHandler execHandler;
    private DiscardHandler discardHandler;
    private Socket testSocket;

    @BeforeEach
    void setUp() {
        setHandler = new SetHandler();
        getHandler = new GetHandler();
        incrHandler = new IncrHandler();
        typeHandler = new TypeHandler();
        infoHandler = new InfoHandler();
        xAddHandler = new XAddHandler();
        multiHandler = new MultiHandler();
        execHandler = new ExecHandler();
        discardHandler = new DiscardHandler();
        
        setHandler.register();
        getHandler.register();
        incrHandler.register();
        typeHandler.register();
        infoHandler.register();
        xAddHandler.register();
        multiHandler.register();
        execHandler.register();
        discardHandler.register();
        
        testSocket = TestHelper.createTestSocket();
        
        // Setup job for transaction tests using Builder
        String jobId = ServerUtils.formatIdFromSocket(testSocket);
        JobDto jobDto = new JobDto.Builder(JobType.RESP)
            .addSocket(testSocket)
            .addCommandDtoList()
            .build();
        JobHandler.JOB_MAP.put(jobId, jobDto);
        
        RedisLocalMap.LOCAL_MAP.clear();
    }

    @AfterEach
    void tearDown() {
        RedisLocalMap.LOCAL_MAP.clear();
        String jobId = ServerUtils.formatIdFromSocket(testSocket);
        JobHandler.JOB_MAP.remove(jobId);
    }

    // ==================== SET and GET Integration ====================
    
    @Test
    @DisplayName("SET foo bar then GET foo should return bar")
    void testSetThenGet() {
        // SET foo bar
        String setResult = setHandler.process(testSocket, List.of("foo", "bar"));
        assertEquals("+OK\r\n", setResult);
        
        // GET foo
        String getResult = getHandler.process(testSocket, List.of("foo"));
        assertEquals("$3\r\nbar\r\n", getResult);
    }

    @Test
    @DisplayName("SET grape orange then GET grape should return orange")
    void testSetThenGetAnotherValue() {
        String setResult = setHandler.process(testSocket, List.of("grape", "orange"));
        assertEquals("+OK\r\n", setResult);
        
        String getResult = getHandler.process(testSocket, List.of("grape"));
        assertEquals("$6\r\norange\r\n", getResult);
    }

    @Test
    @DisplayName("SET with PX expiry - GET immediately should return value")
    void testSetWithExpiryThenGetImmediately() {
        // SET foo bar px 1000 (1 second)
        String setResult = setHandler.process(testSocket, List.of("foo", "bar", "px", "1000"));
        assertEquals("+OK\r\n", setResult);
        
        // GET foo immediately
        String getResult = getHandler.process(testSocket, List.of("foo"));
        assertEquals("$3\r\nbar\r\n", getResult);
    }

    @Test
    @DisplayName("SET with PX expiry stores expireTime in cache")
    void testSetWithExpiryStoresExpireTime() {
        // SET foo bar px 1000 (1 second)
        String setResult = setHandler.process(testSocket, List.of("foo", "bar", "px", "1000"));
        assertEquals("+OK\r\n", setResult);
        
        // Verify the cache has an expireTime set
        domain.CacheDto cache = RedisLocalMap.LOCAL_MAP.get("foo");
        assertNotNull(cache);
        assertNotNull(cache.getExpireTime());
        assertTrue(cache.getExpireTime() > System.currentTimeMillis());
    }

    // ==================== INFO Replication ====================
    
    @Test
    @DisplayName("INFO replication should return role information")
    void testInfoReplicationMaster() {
        String result = infoHandler.process(testSocket, List.of("replication"));
        
        assertTrue(result.contains("role:master") || result.contains("role:slave"));
    }

    // ==================== TYPE Command ====================
    
    @Test
    @DisplayName("SET foo bar then TYPE foo should return string")
    void testSetThenTypeString() {
        setHandler.process(testSocket, List.of("foo", "bar"));
        
        String typeResult = typeHandler.process(testSocket, List.of("foo"));
        assertEquals("+string\r\n", typeResult);
    }

    @Test
    @DisplayName("XADD stream * field value then TYPE stream should return stream")
    void testXAddThenTypeStream() {
        xAddHandler.process(testSocket, List.of("mystream", "*", "field", "value"));
        
        String typeResult = typeHandler.process(testSocket, List.of("mystream"));
        assertEquals("+stream\r\n", typeResult);
    }

    @Test
    @DisplayName("TYPE on non-existent key should return none")
    void testTypeNonExistentKey() {
        String typeResult = typeHandler.process(testSocket, List.of("nonexistent"));
        assertEquals("+none\r\n", typeResult);
    }

    // ==================== INCR Operations ====================
    
    @Test
    @DisplayName("SET foo 100 then INCR foo should return 101")
    void testIncrOnNumber() {
        setHandler.process(testSocket, List.of("foo", "100"));
        
        String incrResult = incrHandler.process(testSocket, List.of("foo"));
        assertEquals(":101\r\n", incrResult);
    }

    @Test
    @DisplayName("Multiple INCR operations")
    void testMultipleIncr() {
        setHandler.process(testSocket, List.of("counter", "0"));
        
        assertEquals(":1\r\n", incrHandler.process(testSocket, List.of("counter")));
        assertEquals(":2\r\n", incrHandler.process(testSocket, List.of("counter")));
        assertEquals(":3\r\n", incrHandler.process(testSocket, List.of("counter")));
    }

    @Test
    @DisplayName("INCR on non-numeric value should return error")
    void testIncrOnNonNumber() {
        setHandler.process(testSocket, List.of("foo", "bar"));
        
        String incrResult = incrHandler.process(testSocket, List.of("foo"));
        assertTrue(incrResult.startsWith("-ERR"));
    }

    @Test
    @DisplayName("INCR on non-existent key should initialize to 1")
    void testIncrOnNonExistentKey() {
        String incrResult = incrHandler.process(testSocket, List.of("newcounter"));
        assertEquals(":1\r\n", incrResult);
    }

    // ==================== Transaction Tests ====================
    
    @Test
    @DisplayName("MULTI then queue SET, queue INCR, then EXEC")
    void testMultiSetIncrExec() {
        // MULTI
        String multiResult = multiHandler.process(testSocket, List.of());
        assertEquals("+OK\r\n", multiResult);
        
        // Queue commands using MultiHandler.queueCommand()
        String queueResult1 = MultiHandler.queueCommand(testSocket, List.of("set", "foo", "5"));
        assertEquals("+QUEUED\r\n", queueResult1);
        
        String queueResult2 = MultiHandler.queueCommand(testSocket, List.of("incr", "foo"));
        assertEquals("+QUEUED\r\n", queueResult2);
        
        // EXEC
        String execResult = execHandler.process(testSocket, List.of());
        
        // Result should be an array with 2 results
        assertTrue(execResult.startsWith("*2\r\n"));
    }

    @Test
    @DisplayName("MULTI then EXEC with no commands should return empty array")
    void testMultiExecEmpty() {
        multiHandler.process(testSocket, List.of());
        
        String execResult = execHandler.process(testSocket, List.of());
        
        assertEquals("*0\r\n", execResult);
    }

    @Test
    @DisplayName("MULTI then queue SET then DISCARD should discard transaction")
    void testMultiSetDiscard() {
        multiHandler.process(testSocket, List.of());
        MultiHandler.queueCommand(testSocket, List.of("set", "foo", "bar"));
        
        String discardResult = discardHandler.process(testSocket, List.of());
        assertEquals("+OK\r\n", discardResult);
        
        // Value should not have been set
        String getResult = getHandler.process(testSocket, List.of("foo"));
        assertEquals("$-1\r\n", getResult);
    }

    @Test
    @DisplayName("EXEC without MULTI should return error")
    void testExecWithoutMulti() {
        // Make sure the job is NOT in transaction mode
        String jobId = ServerUtils.formatIdFromSocket(testSocket);
        JobDto jobDto = JobHandler.JOB_MAP.get(jobId);
        jobDto.setCommandAtomic(false);
        
        String execResult = execHandler.process(testSocket, List.of());
        
        assertTrue(execResult.startsWith("-"));
    }

    @Test
    @DisplayName("DISCARD without MULTI should return error")
    void testDiscardWithoutMulti() {
        // Make sure the job is NOT in transaction mode
        String jobId = ServerUtils.formatIdFromSocket(testSocket);
        JobDto jobDto = JobHandler.JOB_MAP.get(jobId);
        jobDto.setCommandAtomic(false);
        
        String discardResult = discardHandler.process(testSocket, List.of());
        
        assertTrue(discardResult.startsWith("-"));
    }

    // ==================== Combined Operations ====================
    
    @Test
    @DisplayName("Multiple key-value operations")
    void testMultipleKVOperations() {
        // Set multiple values
        setHandler.process(testSocket, List.of("key1", "value1"));
        setHandler.process(testSocket, List.of("key2", "value2"));
        setHandler.process(testSocket, List.of("key3", "value3"));
        
        // Get all values
        assertEquals("$6\r\nvalue1\r\n", getHandler.process(testSocket, List.of("key1")));
        assertEquals("$6\r\nvalue2\r\n", getHandler.process(testSocket, List.of("key2")));
        assertEquals("$6\r\nvalue3\r\n", getHandler.process(testSocket, List.of("key3")));
        
        // Verify types
        assertEquals("+string\r\n", typeHandler.process(testSocket, List.of("key1")));
        assertEquals("+string\r\n", typeHandler.process(testSocket, List.of("key2")));
        assertEquals("+string\r\n", typeHandler.process(testSocket, List.of("key3")));
    }

    @Test
    @DisplayName("Overwrite existing key")
    void testOverwriteExistingKey() {
        setHandler.process(testSocket, List.of("mykey", "initial"));
        assertEquals("$7\r\ninitial\r\n", getHandler.process(testSocket, List.of("mykey")));
        
        setHandler.process(testSocket, List.of("mykey", "updated"));
        assertEquals("$7\r\nupdated\r\n", getHandler.process(testSocket, List.of("mykey")));
    }

    @Test
    @DisplayName("Stream operations with TYPE")
    void testStreamOperationsWithType() {
        // Create stream
        xAddHandler.process(testSocket, List.of("events", "*", "action", "login"));
        xAddHandler.process(testSocket, List.of("events", "*", "action", "logout"));
        
        // Verify type
        assertEquals("+stream\r\n", typeHandler.process(testSocket, List.of("events")));
    }

    @Test
    @DisplayName("INCR creates integer-storing string")
    void testIncrCreatesIntegerString() {
        // INCR on non-existent key
        incrHandler.process(testSocket, List.of("newkey"));
        
        // TYPE should be string
        assertEquals("+string\r\n", typeHandler.process(testSocket, List.of("newkey")));
        
        // GET should return "1"
        assertEquals("$1\r\n1\r\n", getHandler.process(testSocket, List.of("newkey")));
    }
}
