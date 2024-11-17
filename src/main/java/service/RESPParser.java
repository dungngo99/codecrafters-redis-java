package service;

import enums.Command;
import handler.CommandHandler;
import stream.RedisInputStream;

import java.io.IOException;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static constants.ParserConstants.*;

public class RESPParser {

    public static class Builder {
        private Socket clientSocket;
        private Integer bufferSize;

        public Builder addClientSocket(Socket clientSocket) {
            this.clientSocket = clientSocket;
            return this;
        }

        public Builder addBufferSize(Integer bufferSize) {
            this.bufferSize = bufferSize;
            return this;
        }

        public RESPParser build() throws IOException {
            RESPParser parser = new RESPParser();
            parser.setClientSocket(this.clientSocket);
            parser.setInputStream(new RedisInputStream(this.clientSocket.getInputStream(), this.bufferSize));
            return parser;
        }
    }

    public String process() throws IOException {
        byte b = (byte) inputStream.read();
        Object obj;
        switch(b) {
            case ARRAY_ASTERISK:
                obj = processNextArray();
                break;
            case BULK_STRING_DOLLAR_SIGN:
                obj = processNextString();
                break;
            case SIMPLE_STRING_PLUS:
                obj = processSimpleString();
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

    private String convert(Object object) {
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

    private String convertList(Object object) {
        List list = (List) object;
        if (list.isEmpty()) {
            return "";
        }
        String alias = (String) list.get(0);
        CommandHandler commandHandler = CommandHandler.HANDLER_MAP.getOrDefault(alias.toLowerCase(), null);
        if (commandHandler == null) {
            return "";
        }
        Command command = Command.fromAlias(alias);
        List args = list.subList(1, list.size());
        String val = commandHandler.process(clientSocket, args);
        if (command != null && command.isWrite()) {
            new Thread(() -> commandHandler.propagate(list)).start();
        }
        return val != null && !val.isBlank() ? val : RESPUtils.getBulkNull();
    }

    private List<Object> processNextArray() throws IOException {
        int size = processNextInt(inputStream);
        inputStream.skipCRLF();
        List<Object> ans = new ArrayList<>(size);
        for (int i=0; i<size; i++) {
            ans.add(process());
        }
        return ans;
    }

    private String processNextString() throws IOException {
        int size = processNextInt(inputStream);
        inputStream.skipCRLF();
        return processNextString0(inputStream, size);
    }

    private String processSimpleString() throws IOException {
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

    private String processNextString0(RedisInputStream inputStream, int size) throws IOException {
        byte[] bytes = inputStream.readNBytes(size);
        inputStream.skipCRLF();
        return new String(bytes, StandardCharsets.UTF_8);
    }

    private int processNextInt(RedisInputStream inputStream) throws IOException {
        List<Integer> digits = new ArrayList<>();
        while (inputStream.peekCurrentByte() != CR) {
            digits.add(inputStream.read() - ((int) '0'));
        }
        return Integer.parseInt(digits.stream().map(String::valueOf).collect(Collectors.joining("")));
    }

    private Socket clientSocket;
    private RedisInputStream inputStream;

    public Socket getClientSocket() {
        return clientSocket;
    }

    public void setClientSocket(Socket clientSocket) {
        this.clientSocket = clientSocket;
    }

    public RedisInputStream getInputStream() {
        return inputStream;
    }

    public void setInputStream(RedisInputStream inputStream) {
        this.inputStream = inputStream;
    }
}
