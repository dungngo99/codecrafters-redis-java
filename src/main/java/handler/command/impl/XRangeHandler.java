package handler.command.impl;

import dto.CacheDto;
import dto.StreamDto;
import enums.CommandType;
import enums.ValueType;
import handler.command.CommandHandler;
import service.RESPUtils;
import service.RedisLocalMap;
import service.StreamUtils;

import java.net.Socket;
import java.util.List;
import java.util.Objects;

public class XRangeHandler implements CommandHandler {
    @Override
    public void register() {
        CommandHandler.HANDLER_MAP.put(CommandType.XRANGE.getAlias(), this);
    }

    /**
     * assume each event ID will follow format <time part in MS>-<sequence number>
     * @param clientSocket client socket
     * @param list list of commands
     * @return xrange result in RESP format
     */
    @Override
    public String process(Socket clientSocket, List list) {
        if (list == null || list.size() < 3) {
            throw new RuntimeException("invalid param");
        }

        String streamKey = (String) list.get(0);
        CacheDto cacheDto = RedisLocalMap.LOCAL_MAP.get(streamKey);
        if (Objects.isNull(cacheDto)
                || !Objects.equals(cacheDto.getValueType(), ValueType.STREAM)
                || Objects.isNull(cacheDto.getValue())) {
            return RESPUtils.getBulkNull();
        }

        List<StreamDto.EntryDto> streamList = ((StreamDto) cacheDto.getValue()).getStreamList();
        if (streamList.isEmpty()) {
            return RESPUtils.getBulkNull();
        }

        String startEventId = (String) list.get(1);
        String endEventId = (String) list.get(2);
        Long[] parsedStartEventIds = StreamUtils.parseEventId(startEventId);
        Long[] parsedEndEventIds = StreamUtils.parseEventId(endEventId);
        int startEventIndex = StreamUtils.getIndexFromStream(streamList, parsedStartEventIds, false);
        int endEventIndex = StreamUtils.getIndexFromStream(streamList, parsedEndEventIds, true);
        List<StreamDto.EntryDto> rangeStreamList = streamList.subList(startEventIndex, endEventIndex);
        List<Object> listObj = StreamUtils.fromStreamEntryDto(rangeStreamList);
        return RESPUtils.toBulkStringFromNestedList(listObj);
    }
}
