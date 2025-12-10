package service;

public class GeoUtils {
    public static final Double LONGITUDE_LOWER_BOUND = -180.0;
    public static final Double LONGITUDE_UPPER_BOUND = 180.0;
    public static final Double LATITUDE_LOWER_BOUND = -85.05112878;
    public static final Double LATITUDE_UPPER_BOUND = 85.05112878;

    public static boolean isValidLongitude(Double longitude) {
        return LONGITUDE_LOWER_BOUND <= longitude && longitude <= LONGITUDE_UPPER_BOUND;
    }

    public static boolean isValidLatitude(Double latitude) {
        return LATITUDE_LOWER_BOUND <= latitude && latitude <= LATITUDE_UPPER_BOUND;
    }
}
