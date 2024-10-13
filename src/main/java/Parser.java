import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.StringJoiner;

public class Parser {

    private static final byte SIMPLE_STRING_PLUS = '+';
    private static final byte SIMPLE_ERROR_MINUS = '-';
    private static final byte INTEGER_COLON = ':';
    private static final byte BULK_STRING_DOLLAR_SIGN = '$';
    private static final byte ARRAY_ASTERISK = '*';
    private static final byte TERMINATOR = -1;
    private static final byte ZERO_TERMINATOR = 0;

    public static String process(RedisInputStream inputStream) throws IOException {
        byte b = (byte) inputStream.read();
        switch(b) {
            case ARRAY_ASTERISK:
                Object ans = processNextArray(inputStream);
                return convert(ans);
            case BULK_STRING_DOLLAR_SIGN:
                return processNextString(inputStream);
            case TERMINATOR:
            case ZERO_TERMINATOR:
                return "";
            default:
                throw new RuntimeException("error here");
        }
    }

    private static String convert(Object object) {
        if (object == null) {
            return "";
        }
        if (object instanceof String) {
            return (String) object;
        }
        if (object instanceof List) {
            StringJoiner joiner = new StringJoiner("\r\n", "+", "\r\n");
            List list = (List) object;
            // the first arg as redis command
            if (list.size() == 1) {
                String command = (String) list.get(0);
                if (command.equalsIgnoreCase("ping")) {
                    joiner.add("PONG");
                } else {
                    return "";
                }
            } else {
                for (int i=1; i<list.size(); i++) {
                    joiner.add(convert(list.get(i)));
                }
            }
            return joiner.toString();
        }
        return String.valueOf(object);
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

    private static String processNextString0(RedisInputStream inputStream, int size) throws IOException {
        byte[] bytes = inputStream.readNBytes(size);
        inputStream.skipCRLF();
        return new String(bytes, StandardCharsets.UTF_8);
    }

    private static int processNextInt(RedisInputStream inputStream) throws IOException {
        return inputStream.read() - ((int) '0');
    }
}
