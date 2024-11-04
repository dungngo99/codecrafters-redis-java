package service;

import constants.OutputConstants;

import java.util.Objects;

public class SystemPropHelper {

    public static void setNewEnvProperty(String key, String value) {
        System.setProperty(key, value);
    }

    public static int getServerPortOrDefault() {
        String ans = System.getProperty(OutputConstants.REDIS_SERVER_PORT_KEY);
        return Objects.nonNull(ans) ? Integer.parseInt(ans) : OutputConstants.DEFAULT_REDIS_SERVER_PORT;
    }

    public static String getSetServerRoleOrDefault() {
        String role = System.getProperty(OutputConstants.REDIS_SERVER_ROLE_TYPE);
        if (Objects.nonNull(role)) {
            return role;
        }
        System.setProperty(OutputConstants.REDIS_SERVER_ROLE_TYPE, OutputConstants.REDIS_SERVER_DEFAULT_ROLE);
        return OutputConstants.REDIS_SERVER_DEFAULT_ROLE;
    }
}
