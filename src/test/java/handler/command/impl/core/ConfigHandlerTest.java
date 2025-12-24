package handler.command.impl.core;

import constants.OutputConstants;
import handler.command.impl.TestHelper;
import org.junit.jupiter.api.*;

import java.net.Socket;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for ConfigHandler
 * 
 * Tests cover:
 * - CONFIG GET dir
 * - CONFIG GET dbfilename
 * - Invalid subcommands
 * - Invalid parameters
 */
@DisplayName("ConfigHandler Tests")
class ConfigHandlerTest {

    private ConfigHandler configHandler;
    private Socket testSocket;

    @BeforeEach
    void setUp() {
        configHandler = new ConfigHandler();
        configHandler.register();
        testSocket = TestHelper.createTestSocket();
    }

    @AfterEach
    void tearDown() {
        System.clearProperty(OutputConstants.DIR);
        System.clearProperty(OutputConstants.DB_FILENAME);
    }

    @Test
    @DisplayName("CONFIG GET dir should return directory when set")
    void testConfigGetDir() {
        System.setProperty(OutputConstants.DIR, "/tmp/redis");
        
        String result = configHandler.process(testSocket, List.of("get", "dir"));
        
        assertTrue(result.contains("dir"));
        assertTrue(result.contains("/tmp/redis"));
    }

    @Test
    @DisplayName("CONFIG GET dbfilename should return filename when set")
    void testConfigGetDbFilename() {
        System.setProperty(OutputConstants.DB_FILENAME, "dump.rdb");
        
        String result = configHandler.process(testSocket, List.of("get", "dbfilename"));
        
        assertTrue(result.contains("dbfilename"));
        assertTrue(result.contains("dump.rdb"));
    }

    @Test
    @DisplayName("CONFIG GET with unset property should return empty")
    void testConfigGetUnsetProperty() {
        System.clearProperty(OutputConstants.DIR);
        
        String result = configHandler.process(testSocket, List.of("get", "dir"));
        
        assertEquals("", result);
    }

    @Test
    @DisplayName("CONFIG with null list should throw exception")
    void testConfigWithNullList() {
        assertThrows(RuntimeException.class, () -> {
            configHandler.process(testSocket, null);
        });
    }

    @Test
    @DisplayName("CONFIG with insufficient params should throw exception")
    void testConfigWithInsufficientParams() {
        assertThrows(RuntimeException.class, () -> {
            configHandler.process(testSocket, List.of("get"));
        });
    }

    @Test
    @DisplayName("CONFIG with invalid subcommand should throw exception")
    void testConfigWithInvalidSubcommand() {
        assertThrows(RuntimeException.class, () -> {
            configHandler.process(testSocket, List.of("invalid", "param"));
        });
    }

    @Test
    @DisplayName("CONFIG GET is case insensitive")
    void testConfigGetCaseInsensitive() {
        System.setProperty(OutputConstants.DIR, "/tmp/redis");
        
        String result1 = configHandler.process(testSocket, List.of("GET", "dir"));
        String result2 = configHandler.process(testSocket, List.of("Get", "dir"));
        String result3 = configHandler.process(testSocket, List.of("get", "DIR"));
        
        assertTrue(result1.contains("/tmp/redis"));
        assertTrue(result2.contains("/tmp/redis"));
        assertTrue(result3.contains("/tmp/redis"));
    }

    @Test
    @DisplayName("CONFIG handler should be registered correctly")
    void testHandlerRegistration() {
        assertTrue(handler.command.CommandHandler.HANDLER_MAP.containsKey("config"));
        assertSame(configHandler, handler.command.CommandHandler.HANDLER_MAP.get("config"));
    }

    @Test
    @DisplayName("CONFIG GET unknown parameter should return empty")
    void testConfigGetUnknownParameter() {
        String result = configHandler.process(testSocket, List.of("get", "unknown_param"));
        
        assertEquals("", result);
    }
}
