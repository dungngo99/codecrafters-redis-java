package service;

import handler.CommandHandler;
import stream.RedisInputStream;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static constants.ParserConstants.*;

public class RESPParser {

    public static String process(RedisInputStream inputStream) throws IOException {
        byte b = (byte) inputStream.read();
        Object obj;
        switch(b) {
            case ARRAY_ASTERISK:
                obj = processNextArray(inputStream);
                break;
            case BULK_STRING_DOLLAR_SIGN:
                obj = processNextString(inputStream);
                break;
            case SIMPLE_STRING_PLUS:
                obj = processSimpleString(inputStream);
                break;
            case TERMINATOR:
            case ZERO_TERMINATOR:
                obj = "";
                break;
            default:
                throw new RuntimeException("unsupported RESP indicator b=" + b);
        }
        return convert(obj);
    }

    private static String convert(Object object) {
        if (object == null) {
            return "";
        }
        if (object instanceof String) {
            return (String) object;
        }
        if (object instanceof List) {
            return convertList(object);
        }
        return String.valueOf(object);
    }

    private static String convertList(Object object) {
        List list = (List) object;
        if (list.isEmpty()) {
            return "";
        }
        String command = (String) list.get(0);
        CommandHandler commandHandler = CommandHandler.HANDLER_MAP.getOrDefault(command.toLowerCase(), null);
        if (commandHandler == null) {
            return "";
        }
        List args = list.subList(1, list.size());
        String val = commandHandler.process(args);
        return val != null && !val.isBlank() ? val : RESPUtils.getBulkNull();
    }

    private static List<Object> processNextArray(RedisInputStream inputStream) throws IOException {
        int size = processNextInt(inputStream);
        inputStream.skipCRLF();
        List<Object> ans = new ArrayList<>(size);
        for (int i=0; i<size; i++) {
            ans.add(process(inputStream));
        }
        return ans;
    }

    private static String processNextString(RedisInputStream inputStream) throws IOException {
        int size = processNextInt(inputStream);
        inputStream.skipCRLF();
        return processNextString0(inputStream, size);
    }

    private static String processSimpleString(RedisInputStream inputStream) throws IOException {
        List<Byte> byteList = new ArrayList<>();
        while (true) {
            byte b = inputStream.peekCurrentByte();
            if (b == '\r' || b == '\n') {
                inputStream.skipNByte(CRLF_LENGTH);
                break;
            }
            byteList.add(inputStream.readByte());
        }
        byte[] bytes = RESPUtils.fromByteList(byteList);
        return new String(bytes, StandardCharsets.UTF_8);
    }

    private static String processNextString0(RedisInputStream inputStream, int size) throws IOException {
        byte[] bytes = inputStream.readNBytes(size);
        inputStream.skipCRLF();
        return new String(bytes, StandardCharsets.UTF_8);
    }

    private static int processNextInt(RedisInputStream inputStream) throws IOException {
        List<Integer> digits = new ArrayList<>();
        while (inputStream.peekCurrentByte() != CR) {
            digits.add(inputStream.read() - ((int) '0'));
        }
        return Integer.parseInt(digits.stream().map(String::valueOf).collect(Collectors.joining("")));
    }
}
