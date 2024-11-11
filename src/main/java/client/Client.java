package client;

import constants.OutputConstants;

import java.util.StringJoiner;

public class Client {

    public static String getRESPPing() {
        StringJoiner joiner = new StringJoiner(OutputConstants.CRLF, OutputConstants.ASTERISK, OutputConstants.CRLF);
        joiner.add(String.valueOf(OutputConstants.RESP_PING_ARRAY_LENGTH));
        joiner.add(OutputConstants.DOLLAR_SIZE + OutputConstants.PING.length());
        joiner.add(OutputConstants.PING);
        return joiner.toString();
    }
}
