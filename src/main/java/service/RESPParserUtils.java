package service;

import constants.OutputConstants;

import java.util.List;
import java.util.StringJoiner;

public class RESPParserUtils {

    public static String toRESPString(List<String> list) {
        StringJoiner joiner = new StringJoiner("\r\n", "", "\r\n");
        joiner.add(OutputConstants.ASTERISK + list.size());
        for (String str: list) {
            joiner.add(OutputConstants.DOLLAR_SIZE + str.length());
            joiner.add(str);
        }
        return joiner.toString();
    }
}
