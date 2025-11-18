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

public class LRangeHandler implements CommandHandler {
    @Override
    public void register() {
        CommandHandler.HANDLER_MAP.put(CommandType.LRANGE.getAlias(), this);
    }

    @Override
    public String process(Socket clientSocket, List list) {
        if (list == null || list.isEmpty() || list.size() < 3) {
            throw new RuntimeException("invalid param");
        }

        String key = (String) list.get(0);
        int start = Integer.parseInt((String) list.get(1));
        int end = Integer.parseInt((String) list.get(2));

        if (!RedisLocalMap.LOCAL_MAP.containsKey(key)) {
            return RESPUtils.getEmptyArray();
        }

        CacheDto cacheDto = RedisLocalMap.LOCAL_MAP.get(key);
        if (!ValueType.isList(cacheDto.getValueType()) || !(cacheDto.getValue() instanceof List<?>)) {
            throw new RuntimeException("LRangeHandler: command not applied to stored value");
        }

        List<Object> storedList = (List<Object>) cacheDto.getValue();
        int storedListLength = storedList.size();

        start = start >= 0 ? start : (Math.abs(start) >= storedListLength ? 0 : storedListLength + start);
        end = end >= 0 ? end : (Math.abs(end) > storedListLength ? 0 : storedListLength + end);
        if (storedList.size() <= start || end < start) {
            return RESPUtils.getEmptyArray();
        }
        List<Object> storedSubList = storedList.subList(start, Math.min(end+1, storedListLength));

        List<String> valueList = new ArrayList<>();
        for (Object o: storedSubList) {
            valueList.add((String) o);
        }

        return RESPUtils.toArray(valueList);
    }
}
