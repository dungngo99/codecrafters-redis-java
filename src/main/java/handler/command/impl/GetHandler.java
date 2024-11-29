package handler.command.impl;

import dto.CacheDto;
import enums.CommandType;
import enums.ValueType;
import handler.command.CommandHandler;
import service.RESPUtils;
import service.RedisLocalMap;

import java.net.Socket;
import java.util.List;
import java.util.Objects;

public class GetHandler implements CommandHandler {
    @Override
    public void register() {
        CommandHandler.HANDLER_MAP.put(CommandType.GET.name().toLowerCase(), this);
    }

    @Override
    public String process(Socket clientSocket, List list) {
        if (list == null || list.isEmpty()) {
            throw new RuntimeException("invalid param");
        }
        String key = (String) list.get(0);
        if (!RedisLocalMap.LOCAL_MAP.containsKey(key)) {
            return RESPUtils.getBulkNull();
        } else {
            CacheDto cache = RedisLocalMap.LOCAL_MAP.get(key);
            if (!Objects.equals(cache.getValueType(), ValueType.STRING)) {
                return RESPUtils.getBulkNull();
            }
            String value = (String) cache.getValue();
            return RESPUtils.toBulkString(value);
        }
    }
}
