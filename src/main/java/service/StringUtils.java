package service;

public class StringUtils {

    public static boolean isNotBlank(String string) {
        return string != null && !string.isEmpty();
    }

    public static boolean isBlank(String string) {
        return string == null || string.isBlank();
    }
}
