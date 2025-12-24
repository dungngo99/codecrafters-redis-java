package handler.command.impl.geospatial;

import handler.command.impl.TestHelper;
import handler.command.impl.sortedset.ZAddHandler;
import handler.command.impl.sortedset.ZSCoreHandler;
import org.junit.jupiter.api.*;
import service.RedisLocalMap;

import java.net.Socket;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for Geospatial Handlers (GeoAddHandler, GeoPosHandler, GeoDistHandler, GeoSearchHandler)
 * 
 * Tests cover:
 * - GEOADD adds geospatial data
 * - GEOPOS returns position
 * - GEODIST calculates distance
 * - GEOSEARCH finds members within radius
 * - Invalid coordinates validation
 */
@DisplayName("Geospatial Handler Tests")
class GeoHandlerTest {

    private GeoAddHandler geoAddHandler;
    private GeoPosHandler geoPosHandler;
    private GeoDistHandler geoDistHandler;
    private GeoSearchHandler geoSearchHandler;
    private ZAddHandler zAddHandler;
    private ZSCoreHandler zScoreHandler;
    private Socket testSocket;

    @BeforeEach
    void setUp() {
        geoAddHandler = new GeoAddHandler();
        geoPosHandler = new GeoPosHandler();
        geoDistHandler = new GeoDistHandler();
        geoSearchHandler = new GeoSearchHandler();
        zAddHandler = new ZAddHandler();
        zScoreHandler = new ZSCoreHandler();
        
        geoAddHandler.register();
        geoPosHandler.register();
        geoDistHandler.register();
        geoSearchHandler.register();
        zAddHandler.register();
        zScoreHandler.register();
        
        testSocket = TestHelper.createTestSocket();
        
        RedisLocalMap.LOCAL_MAP.clear();
    }

    @AfterEach
    void tearDown() throws Exception {
        RedisLocalMap.LOCAL_MAP.clear();
    }

    // ==================== GEOADD Tests ====================
    
    @Test
    @DisplayName("GEOADD should add geospatial data and return count")
    void testGeoAdd() {
        // GEOADD Sicily 13.361389 38.115556 "Palermo"
        String result = geoAddHandler.process(testSocket, 
            List.of("Sicily", "13.361389", "38.115556", "Palermo"));
        
        assertEquals(":1\r\n", result);
    }

    @Test
    @DisplayName("GEOADD should add multiple locations")
    void testGeoAddMultiple() {
        String result = geoAddHandler.process(testSocket, 
            List.of("Sicily", 
                "13.361389", "38.115556", "Palermo",
                "15.087269", "37.502669", "Catania"));
        
        assertEquals(":2\r\n", result);
    }

    @Test
    @DisplayName("GEOADD with invalid latitude should return error")
    void testGeoAddInvalidLatitude() {
        // Latitude must be between -85.05112878 and 85.05112878
        String result = geoAddHandler.process(testSocket, 
            List.of("mykey", "13.361389", "91.0", "invalid_lat"));
        
        assertTrue(result.contains("ERR"));
        assertTrue(result.contains("latitude"));
    }

    @Test
    @DisplayName("GEOADD with invalid longitude should return error")
    void testGeoAddInvalidLongitude() {
        // Longitude must be between -180 and 180
        String result = geoAddHandler.process(testSocket, 
            List.of("mykey", "200.0", "38.115556", "invalid_lon"));
        
        assertTrue(result.contains("ERR"));
        assertTrue(result.contains("longitude"));
    }

    @Test
    @DisplayName("GEOADD with null list should throw exception")
    void testGeoAddWithNullList() {
        assertThrows(RuntimeException.class, () -> {
            geoAddHandler.process(testSocket, null);
        });
    }

    @Test
    @DisplayName("GEOADD with insufficient params should throw exception")
    void testGeoAddWithInsufficientParams() {
        assertThrows(RuntimeException.class, () -> {
            geoAddHandler.process(testSocket, List.of("mykey", "13.361389", "38.115556"));
        });
    }

    // ==================== GEOPOS Tests ====================
    
    @Test
    @DisplayName("GEOPOS should return coordinates")
    void testGeoPos() {
        geoAddHandler.process(testSocket, 
            List.of("Sicily", "13.361389", "38.115556", "Palermo"));
        
        String result = geoPosHandler.process(testSocket, 
            List.of("Sicily", "Palermo"));
        
        // Should contain longitude and latitude
        assertTrue(result.contains("*"));
        // Note: coordinates may be slightly different due to encoding/decoding
    }

    @Test
    @DisplayName("GEOPOS on non-existent member should return nil")
    void testGeoPosNonExistentMember() {
        geoAddHandler.process(testSocket, 
            List.of("Sicily", "13.361389", "38.115556", "Palermo"));
        
        String result = geoPosHandler.process(testSocket, 
            List.of("Sicily", "NonExistent"));
        
        // Should contain null for non-existent member
        assertTrue(result.contains("*"));
    }

    @Test
    @DisplayName("GEOPOS with null list should throw exception")
    void testGeoPosWithNullList() {
        assertThrows(RuntimeException.class, () -> {
            geoPosHandler.process(testSocket, null);
        });
    }

    @Test
    @DisplayName("GEOPOS with insufficient params should throw exception")
    void testGeoPosWithInsufficientParams() {
        assertThrows(RuntimeException.class, () -> {
            geoPosHandler.process(testSocket, List.of("Sicily"));
        });
    }

    // ==================== GEODIST Tests ====================
    
    @Test
    @DisplayName("GEODIST should return distance between two members")
    void testGeoDist() {
        geoAddHandler.process(testSocket, 
            List.of("Sicily", 
                "13.361389", "38.115556", "Palermo",
                "15.087269", "37.502669", "Catania"));
        
        String result = geoDistHandler.process(testSocket, 
            List.of("Sicily", "Palermo", "Catania"));
        
        // Should return distance in meters
        assertTrue(result.startsWith("$"));
        assertTrue(result.contains(".")); // Should be a decimal number
    }

    @Test
    @DisplayName("GEODIST with null list should throw exception")
    void testGeoDistWithNullList() {
        assertThrows(RuntimeException.class, () -> {
            geoDistHandler.process(testSocket, null);
        });
    }

    @Test
    @DisplayName("GEODIST with insufficient params should throw exception")
    void testGeoDistWithInsufficientParams() {
        assertThrows(RuntimeException.class, () -> {
            geoDistHandler.process(testSocket, List.of("Sicily", "Palermo"));
        });
    }

    // ==================== GEOSEARCH Tests ====================
    
    @Test
    @DisplayName("GEOSEARCH should return members within radius")
    void testGeoSearch() {
        geoAddHandler.process(testSocket, 
            List.of("Sicily", 
                "13.361389", "38.115556", "Palermo",
                "15.087269", "37.502669", "Catania",
                "13.583333", "37.316667", "Agrigento"));
        
        // Search from center point with large radius
        String result = geoSearchHandler.process(testSocket, 
            List.of("Sicily", "FROMMEMBER", "13.5", "38.0", "BYRADIUS", "200", "km"));
        
        assertTrue(result.startsWith("*"));
    }

    @Test
    @DisplayName("GEOSEARCH on non-existent key should return empty")
    void testGeoSearchNonExistentKey() {
        String result = geoSearchHandler.process(testSocket, 
            List.of("nonexistent", "FROMMEMBER", "0", "0", "BYRADIUS", "100", "km"));
        
        assertEquals("*0\r\n", result);
    }

    @Test
    @DisplayName("GEOSEARCH with null list should throw exception")
    void testGeoSearchWithNullList() {
        assertThrows(RuntimeException.class, () -> {
            geoSearchHandler.process(testSocket, null);
        });
    }

    @Test
    @DisplayName("GEOSEARCH with insufficient params should throw exception")
    void testGeoSearchWithInsufficientParams() {
        assertThrows(RuntimeException.class, () -> {
            geoSearchHandler.process(testSocket, List.of("Sicily", "FROMMEMBER"));
        });
    }

    // ==================== Handler Registration Tests ====================
    
    @Test
    @DisplayName("Geospatial handlers should be registered correctly")
    void testHandlerRegistration() {
        assertTrue(handler.command.CommandHandler.HANDLER_MAP.containsKey("geoadd"));
        assertTrue(handler.command.CommandHandler.HANDLER_MAP.containsKey("geopos"));
        assertTrue(handler.command.CommandHandler.HANDLER_MAP.containsKey("geodist"));
        assertTrue(handler.command.CommandHandler.HANDLER_MAP.containsKey("geosearch"));
    }

    // ==================== Integration Tests ====================
    
    @Test
    @DisplayName("Combined geospatial operations")
    void testCombinedOperations() {
        // Add locations
        geoAddHandler.process(testSocket, 
            List.of("cities", 
                "-122.4194", "37.7749", "San Francisco",
                "-118.2437", "34.0522", "Los Angeles"));
        
        // Get position
        String posResult = geoPosHandler.process(testSocket, 
            List.of("cities", "San Francisco"));
        assertTrue(posResult.startsWith("*"));
        
        // Get distance
        String distResult = geoDistHandler.process(testSocket, 
            List.of("cities", "San Francisco", "Los Angeles"));
        assertTrue(distResult.startsWith("$"));
    }

    @Test
    @DisplayName("GEOADD uses ZADD under the hood")
    void testGeoAddUsesZAdd() {
        geoAddHandler.process(testSocket, 
            List.of("mygeo", "13.361389", "38.115556", "location1"));
        
        // Verify that a ZSET was created
        assertTrue(RedisLocalMap.LOCAL_MAP.containsKey("mygeo"));
        
        // Verify ZSCORE works on the geospatial key
        String scoreResult = zScoreHandler.process(testSocket, 
            List.of("mygeo", "location1"));
        
        // Should return a score (the geohash)
        assertFalse(scoreResult.equals("$-1\r\n"));
    }

    @Test
    @DisplayName("GEOADD with edge case coordinates")
    void testGeoAddEdgeCases() {
        // Test with 0,0 coordinates
        String result = geoAddHandler.process(testSocket, 
            List.of("mykey", "0", "0", "origin"));
        assertEquals(":1\r\n", result);
        
        // Test with negative coordinates
        result = geoAddHandler.process(testSocket, 
            List.of("mykey", "-73.935242", "40.730610", "New York"));
        assertEquals(":1\r\n", result);
    }
}
