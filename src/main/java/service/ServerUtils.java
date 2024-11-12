package service;

import constants.OutputConstants;

public class ServerUtils {

    public static String formatId(String host, int port) {
        return String.format(OutputConstants.SERVER_NODE_ID_FORMAT, host, port);
    }

}
