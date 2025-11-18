package handler.command.impl;

import dto.CacheDto;
import enums.CommandType;
import enums.ValueType;
import handler.command.CommandHandler;
import service.RESPUtils;
import service.RedisLocalMap;

import java.net.Socket;
import java.util.List;

public class LPopHandler implements CommandHandler {
    @Override
    public void register() {
        CommandHandler.HANDLER_MAP.put(CommandType.LPOP.getAlias(), this);
    }

    @Override
    public String process(Socket clientSocket, List list) {
        if (list == null || list.isEmpty()) {
            throw new RuntimeException("invalid param");
        }

        String key = (String) list.get(0);

        if (!RedisLocalMap.LOCAL_MAP.containsKey(key)) {
            return RESPUtils.getBulkNullString();
        }

        CacheDto cache = RedisLocalMap.LOCAL_MAP.get(key);
        if (!ValueType.isList(cache.getValueType()) || !(cache.getValue() instanceof List<?> cacheValue)) {
            throw new RuntimeException("LLenHandler: command not applied to stored value");
        }

        if (cacheValue.isEmpty()) {
            return RESPUtils.getBulkNullString();
        }

        String removedValue = (String) cacheValue.removeFirst();
        return RESPUtils.toBulkString(removedValue);
    }
}
