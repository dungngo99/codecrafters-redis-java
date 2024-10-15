package handler.impl;

import constants.OutputConstants;
import enums.Command;
import handler.CommandHandler;
import service.LocalMap;

import java.util.List;
import java.util.StringJoiner;

public class SetHandler implements CommandHandler {
    @Override
    public void register() {
        CommandHandler.HANDLER_MAP.put(Command.SET.name().toLowerCase(), this);
    }

    @Override
    public String process(List list) {
        if (list == null || list.isEmpty() || list.size() < 2) {
            return "";
        }
        String key = (String) list.get(0);
        Object value = list.get(1);
        LocalMap.LOCAL_MAP.put(key, value);
        StringJoiner joiner = new StringJoiner("\r\n", "+", "\r\n");
        joiner.add(OutputConstants.OK);
        return joiner.toString();
    }
}
