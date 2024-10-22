package service;

import constants.OutputConstants;
import net.whitbeck.rdbparser.*;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class RDBParserUtils {

    public static Map<String, String[]> parseBySDK(File file) throws IOException {
        RdbParser rdbParser = new RdbParser(file);
        Map<String, String[]> map = new HashMap<>();
        Entry e;
        while ((e = rdbParser.readNext()) != null) {
            switch(e.getType()) {
                case SELECT_DB -> {
                    SelectDb selectDb = (SelectDb) e;
                    System.out.println("Select DB: " + selectDb.getId());
                }
                case EOF -> {
                    Eof eof = (Eof) e;
                    StringJoiner joiner = new StringJoiner("");
                    for (byte b: eof.getChecksum()) {
                        joiner.add(String.valueOf(b & 0xff));
                    }
                    System.out.println("Checksum EOF: " + joiner);
                }
                case KEY_VALUE_PAIR -> {
                    KeyValuePair kvPair = (KeyValuePair) e;
                    String key = new String(kvPair.getKey(), StandardCharsets.US_ASCII);
                    StringJoiner joiner = new StringJoiner(OutputConstants.SPACE_DELIMITER);
                    for (byte[] bytes: kvPair.getValues()) {
                        joiner.add(new String(bytes, StandardCharsets.US_ASCII));
                    }
                    String value = joiner.toString();
                    Long px = kvPair.getExpireTime();
                    map.put(key, new String[]{value, String.valueOf(px)});
                }
            }
        }
        return map;
    }
}
