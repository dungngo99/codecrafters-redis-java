package handler.command.impl.auth;

import domain.AclConfigDto;
import handler.command.impl.TestHelper;
import org.junit.jupiter.api.*;
import service.HashUtils;
import service.RedisLocalMap;

import java.net.Socket;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for AuthHandler
 * 
 * Tests cover:
 * - AUTH with correct username/password
 * - AUTH with incorrect password
 * - AUTH when user doesn't exist in ACL
 * 
 * Note: AuthHandler expects (username, password) as parameters and validates
 * against ACL_MAP, not system properties.
 */
@DisplayName("AuthHandler Tests")
class AuthHandlerTest {

    private AuthHandler authHandler;
    private Socket testSocket;

    @BeforeEach
    void setUp() {
        authHandler = new AuthHandler();
        authHandler.register();
        
        testSocket = TestHelper.createTestSocket();
        
        RedisLocalMap.LOCAL_MAP.clear();
        RedisLocalMap.ACL_MAP.clear();
        RedisLocalMap.AUTHENTICATED_CONNECTION_SET.clear();
    }

    @AfterEach
    void tearDown() {
        RedisLocalMap.LOCAL_MAP.clear();
        RedisLocalMap.ACL_MAP.clear();
        RedisLocalMap.AUTHENTICATED_CONNECTION_SET.clear();
    }

    @Test
    @DisplayName("AUTH handler should be registered correctly")
    void testHandlerRegistration() {
        assertTrue(handler.command.CommandHandler.HANDLER_MAP.containsKey("auth"));
        assertSame(authHandler, handler.command.CommandHandler.HANDLER_MAP.get("auth"));
    }

    @Test
    @DisplayName("AUTH with correct username/password should return OK")
    void testAuthCorrectPassword() {
        // Setup ACL with user
        String username = "testuser";
        String password = "secret123";
        AclConfigDto aclConfig = new AclConfigDto();
        aclConfig.setPasswordHash(HashUtils.convertToSHA256(password));
        RedisLocalMap.ACL_MAP.put(username, aclConfig);
        
        String result = authHandler.process(testSocket, List.of(username, password));
        
        assertEquals("+OK\r\n", result);
    }

    @Test
    @DisplayName("AUTH with incorrect password should return error")
    void testAuthIncorrectPassword() {
        // Setup ACL with user
        String username = "testuser";
        String password = "secret123";
        AclConfigDto aclConfig = new AclConfigDto();
        aclConfig.setPasswordHash(HashUtils.convertToSHA256(password));
        RedisLocalMap.ACL_MAP.put(username, aclConfig);
        
        String result = authHandler.process(testSocket, List.of(username, "wrongpassword"));
        
        assertTrue(result.startsWith("-WRONGPASS"));
    }

    @Test
    @DisplayName("AUTH with null list should throw exception")
    void testAuthWithNullList() {
        assertThrows(RuntimeException.class, () -> {
            authHandler.process(testSocket, null);
        });
    }

    @Test
    @DisplayName("AUTH with insufficient params (only 1) should throw exception")
    void testAuthWithInsufficientParams() {
        assertThrows(RuntimeException.class, () -> {
            authHandler.process(testSocket, List.of("onlyone"));
        });
    }

    @Test
    @DisplayName("AUTH when user not in ACL should return error")
    void testAuthUserNotInAcl() {
        String result = authHandler.process(testSocket, List.of("unknownuser", "anypassword"));
        
        assertTrue(result.startsWith("-WRONGPASS"));
    }

    @Test
    @DisplayName("AUTH should add connection to AUTHENTICATED_CONNECTION_SET on success")
    void testAuthAddsToAuthenticatedSet() {
        // Setup ACL with user
        String username = "testuser";
        String password = "secret123";
        AclConfigDto aclConfig = new AclConfigDto();
        aclConfig.setPasswordHash(HashUtils.convertToSHA256(password));
        RedisLocalMap.ACL_MAP.put(username, aclConfig);
        
        authHandler.process(testSocket, List.of(username, password));
        
        assertFalse(RedisLocalMap.AUTHENTICATED_CONNECTION_SET.isEmpty());
    }
}
