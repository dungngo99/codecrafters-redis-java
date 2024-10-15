package handler.impl;

import constants.OutputConstants;
import constants.ParserConstants;
import enums.Command;
import handler.CommandHandler;
import service.LocalMap;
import service.Parser;

import java.util.List;
import java.util.StringJoiner;

public class GetHandler implements CommandHandler {
    @Override
    public void register() {
        CommandHandler.HANDLER_MAP.put(Command.GET.name().toLowerCase(), this);
    }

    @Override
    public String process(List list) {
        if (list == null || list.isEmpty()) {
            return "";
        }
        String key = (String) list.get(0);
        StringJoiner joiner = new StringJoiner("\r\n", "", "\r\n");
        if (!LocalMap.LOCAL_MAP.containsKey(key)) {
            joiner
                    .add(OutputConstants.DOLLAR_SIZE + OutputConstants.NULL_BULK);
        } else {
            String value = (String) LocalMap.LOCAL_MAP.get(key);
            joiner
                    .add(OutputConstants.DOLLAR_SIZE + value.length())
                    .add(value);
        }
        return joiner.toString();
    }
}
