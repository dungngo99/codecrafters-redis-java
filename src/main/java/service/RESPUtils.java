package service;

import constants.OutputConstants;
import dto.Cache;

import java.util.List;
import java.util.StringJoiner;

public class RESPUtils {

    public static String toArray(List<String> list) {
        StringJoiner joiner = new StringJoiner(OutputConstants.CRLF, OutputConstants.EMPTY, OutputConstants.CRLF);
        joiner.add(OutputConstants.ASTERISK + list.size());
        for (String str: list) {
            joiner.add(OutputConstants.DOLLAR_SIZE + str.length());
            joiner.add(str);
        }
        return joiner.toString();
    }

    public static String toBulkString(String str) {
        StringJoiner joiner = new StringJoiner(OutputConstants.CRLF, OutputConstants.EMPTY, OutputConstants.CRLF);
        joiner.add(OutputConstants.DOLLAR_SIZE + str.length());
        joiner.add(str);
        return joiner.toString();
    }

    public static String toSimpleString(String str) {
        StringJoiner joiner = new StringJoiner(OutputConstants.CRLF, OutputConstants.PLUS, OutputConstants.CRLF);
        joiner.add(str);
         return joiner.toString();
    }

    public static String getRESPPing() {
        StringJoiner joiner = new StringJoiner(OutputConstants.CRLF, OutputConstants.ASTERISK, OutputConstants.CRLF);
        joiner.add(String.valueOf(OutputConstants.RESP_PING_ARRAY_LENGTH));
        joiner.add(OutputConstants.DOLLAR_SIZE + OutputConstants.PING.length());
        joiner.add(OutputConstants.PING);
        return joiner.toString();
    }

    public static String getRESPEcho(String echo) {
        return toSimpleString(echo);
    }

    public static String getBulkNull() {
        StringJoiner joiner = new StringJoiner(OutputConstants.CRLF, OutputConstants.EMPTY, OutputConstants.CRLF);
        joiner.add(OutputConstants.DOLLAR_SIZE + OutputConstants.NULL_BULK);
        return joiner.toString();
    }

    public static String getRESPOk() {
        return toSimpleString(OutputConstants.OK);
    }
}
