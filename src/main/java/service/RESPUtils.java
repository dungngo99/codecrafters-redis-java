package service;

import constants.OutputConstants;

import java.util.List;
import java.util.Objects;
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

    public static String toByteStreamWithCRLF(byte[] bytes) {
        StringJoiner joiner = new StringJoiner(OutputConstants.CRLF, OutputConstants.EMPTY, OutputConstants.CRLF);
        joiner.add(OutputConstants.DOLLAR_SIZE + bytes.length);
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

    public static byte[] fromByteList(List<Byte> list) {
        int l = list.size();
        byte[] array = new byte[l];
        for (int i=0; i<l; i++) {
            array[i] = list.get(i);
        }
        return array;
    }

    public static byte[] combine2Bytes(byte[] b1, byte[] b2) {
        byte[] ans = new byte[b1.length + b2.length];
        for (int i=0; i<b1.length; i++) {
            ans[i] = b1[i];
        }
        for (int i=0; i<b2.length; i++) {
            ans[i+b1.length] = b2[i];
        }
        return ans;
    }

    public static boolean isValidRESPResponse(String resp) {
        return Objects.nonNull(resp) &&
                (resp.startsWith(OutputConstants.DOLLAR_SIZE)
                        || resp.startsWith(OutputConstants.NULL_BULK)
                        || resp.startsWith(OutputConstants.ASTERISK)
                        || resp.startsWith(OutputConstants.PLUS));
    }

    public static boolean isValidHandshakeReplicationSimpleString(String resp) {
        return Objects.nonNull(resp) &&
                (resp.startsWith(OutputConstants.OK)
                        || resp.startsWith(OutputConstants.REPLICA_FULL_RESYNC)
                        || resp.startsWith(OutputConstants.PONG));
    }
}
