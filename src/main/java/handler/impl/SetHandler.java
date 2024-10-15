package handler.impl;

import constants.OutputConstants;
import dto.Cache;
import enums.Command;
import handler.CommandHandler;
import service.LocalMap;

import java.util.List;
import java.util.Objects;
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

        Long expiry = null;
        if (list.size() > 2) {
            String px = (String) list.get(2);
            if (Command.PX.name().toLowerCase().equalsIgnoreCase(px)) {
                expiry = Long.parseLong((String) list.get(3));
            }
        }

        Cache cache = new Cache();
        cache.setValue((String) value);
        cache.setPx(expiry);
        cache.setStartTime(System.currentTimeMillis());
        LocalMap.LOCAL_MAP.put(key, cache);

        StringJoiner joiner = new StringJoiner("\r\n", "+", "\r\n");
        joiner.add(OutputConstants.OK);
        return joiner.toString();
    }
}