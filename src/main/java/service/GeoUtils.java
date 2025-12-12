package service;

public class GeoUtils {
    public static final Double LONGITUDE_LOWER_BOUND = -180.0;
    public static final Double LONGITUDE_UPPER_BOUND = 180.0;
    public static final Double LATITUDE_LOWER_BOUND = -85.05112878;
    public static final Double LATITUDE_UPPER_BOUND = 85.05112878;
    private static final Double LATITUDE_RANGE = LATITUDE_UPPER_BOUND - LATITUDE_LOWER_BOUND;
    private static final Double LONGITUDE_RANGE = LONGITUDE_UPPER_BOUND - LONGITUDE_LOWER_BOUND;

    public static boolean isValidLongitude(Double longitude) {
        return LONGITUDE_LOWER_BOUND <= longitude && longitude <= LONGITUDE_UPPER_BOUND;
    }

    public static boolean isValidLatitude(Double latitude) {
        return LATITUDE_LOWER_BOUND <= latitude && latitude <= LATITUDE_UPPER_BOUND;
    }

    /**
     * Refer to the implementation at
     * {@link <a href="https://github.com/codecrafters-io/redis-geocoding-algorithm/blob/main/java/Encode.java">Geo Encoding Algorithm</a>}
     * @param longitude
     * @param latitude
     * @return
     */
    public static double calculateZSetScore(Double longitude, Double latitude) {
        // normalize to the range 0-2^26
        double normalizedLatitude = Math.pow(2, 26) * (latitude - LATITUDE_LOWER_BOUND) / LATITUDE_RANGE;
        double normalizedLongitude = Math.pow(2, 26) * (longitude - LONGITUDE_LOWER_BOUND) / LONGITUDE_RANGE;

        // truncate to integers
        int latitudeInt = (int) normalizedLatitude;
        int longitudeInt = (int) normalizedLongitude;

        return interleave(latitudeInt, longitudeInt);
    }

    private static long spreadInt32ToInt64(int v) {
        long result = v & 0xFFFFFFFFL;
        result = (result | (result << 16)) & 0x0000FFFF0000FFFFL;
        result = (result | (result << 8)) & 0x00FF00FF00FF00FFL;
        result = (result | (result << 4)) & 0x0F0F0F0F0F0F0F0FL;
        result = (result | (result << 2)) & 0x3333333333333333L;
        result = (result | (result << 1)) & 0x5555555555555555L;
        return result;
    }

    private static long interleave(int x, int y) {
        long xSpread = spreadInt32ToInt64(x);
        long ySpread = spreadInt32ToInt64(y);
        long yShifted = ySpread << 1;
        return xSpread | yShifted;
    }
}
