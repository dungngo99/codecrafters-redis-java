package handler.command.impl;

import dto.CacheDto;
import enums.CommandType;
import handler.command.CommandHandler;
import service.RESPUtils;
import service.RedisLocalMap;

import java.net.Socket;
import java.util.List;

public class GetHandler implements CommandHandler {
    @Override
    public void register() {
        CommandHandler.HANDLER_MAP.put(CommandType.GET.name().toLowerCase(), this);
    }

    @Override
    public String process(Socket clientSocket, List list) {
        if (list == null || list.isEmpty()) {
            return "";
        }
        String key = (String) list.get(0);
        if (!RedisLocalMap.LOCAL_MAP.containsKey(key)) {
            return RESPUtils.getBulkNull();
        } else {
            CacheDto cache = RedisLocalMap.LOCAL_MAP.get(key);
            String value = cache.getValue();
            return RESPUtils.toBulkString(value);
        }
    }
}
