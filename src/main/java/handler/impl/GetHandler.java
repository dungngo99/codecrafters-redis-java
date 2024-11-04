package handler.impl;

import constants.OutputConstants;
import dto.Cache;
import enums.Command;
import handler.CommandHandler;
import service.RedisLocalMap;

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
        StringJoiner joiner = new StringJoiner(OutputConstants.CRLF, "", OutputConstants.CRLF);
        if (!RedisLocalMap.LOCAL_MAP.containsKey(key)) {
            joiner
                    .add(OutputConstants.DOLLAR_SIZE + OutputConstants.NULL_BULK);
        } else {
            Cache cache = RedisLocalMap.LOCAL_MAP.get(key);
            String value = cache.getValue();
            joiner
                    .add(OutputConstants.DOLLAR_SIZE + value.length())
                    .add(value);
        }
        return joiner.toString();
    }
}
