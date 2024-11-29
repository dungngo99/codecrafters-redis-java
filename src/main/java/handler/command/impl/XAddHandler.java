package handler.command.impl;

import dto.CacheDto;
import dto.StreamDto;
import enums.CommandType;
import enums.ValueType;
import handler.command.CommandHandler;
import service.RESPUtils;
import service.RedisLocalMap;

import java.net.Socket;
import java.util.List;
import java.util.Objects;

public class XAddHandler implements CommandHandler {
    @Override
    public void register() {
        CommandHandler.HANDLER_MAP.put(CommandType.XADD.getAlias(), this);
    }

    @Override
    public String process(Socket clientSocket, List list) {
        if (list == null || list.size() < 2) {
            throw new RuntimeException("invalid param");
        }
        String streamKey = (String) list.get(0);
        String eventId = (String) list.get(1);
        CacheDto cacheDto = RedisLocalMap.LOCAL_MAP.get(streamKey);
        StreamDto streamDto;
        if (Objects.nonNull(cacheDto) && Objects.equals(cacheDto.getValueType(), ValueType.STREAM)) {
            streamDto = (StreamDto) cacheDto.getValue();
        } else {
            cacheDto = new CacheDto();
            streamDto = new StreamDto();
            cacheDto.setValueType(ValueType.STREAM);
            cacheDto.setValue(streamDto);
        }

        StreamDto.EntryDto entryDto = new StreamDto.EntryDto(eventId);
        for (int i=2; i<list.size(); i+=2) {
            String key = (String) list.get(i);
            String value = (String) list.get(i+1);
            entryDto.getKvPair().put(key, value);
        }

        streamDto.getStreamMap().put(eventId, entryDto);
        RedisLocalMap.LOCAL_MAP.put(streamKey, cacheDto);
        return RESPUtils.toBulkString(eventId);
    }
}
