package handler.command.impl;

import dto.CacheDto;
import enums.CommandType;
import enums.ValueType;
import handler.command.CommandHandler;
import service.RESPUtils;
import service.RedisLocalMap;

import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

public class RPushHandler implements CommandHandler {
    @Override
    public void register() {
        CommandHandler.HANDLER_MAP.put(CommandType.RPUSH.getAlias(), this);
    }

    @Override
    public String process(Socket clientSocket, List list) {
        if (list == null || list.isEmpty() || list.size() < 2) {
            throw new RuntimeException("invalid param");
        }

        String key = (String) list.get(0);
        Object value = list.get(1);

        CacheDto cache;
        if (RedisLocalMap.LOCAL_MAP.containsKey(key)) {
            cache = RedisLocalMap.LOCAL_MAP.get(key);
            if (!ValueType.isList(cache.getValueType()) || !(cache.getValue() instanceof List<?>)) {
                throw new RuntimeException("RPushHandler: command not applied to stored value");
            }
        } else {
            cache = new CacheDto();
            cache.setValueType(ValueType.LIST);
            cache.setValue(new ArrayList<>());
        }

        List<Object> storedList = (List<Object>) cache.getValue();
        storedList.add(value);

        return RESPUtils.toSimpleInt(storedList.size());
    }
}
