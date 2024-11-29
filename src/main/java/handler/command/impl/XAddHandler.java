package handler.command.impl;

import constants.OutputConstants;
import dto.CacheDto;
import dto.StreamDto;
import enums.CommandType;
import enums.ValueType;
import handler.command.CommandHandler;
import service.RESPUtils;
import service.RedisLocalMap;

import java.net.Socket;
import java.util.List;
import java.util.Map;
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
        String error = validateEntryId(streamKey, eventId);
        if (!error.isEmpty()) {
            return error;
        }

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

    private String validateEntryId(String streamKey, String eventId) {
        if (Objects.equals(eventId, OutputConstants.ASTERISK)) {
            return OutputConstants.EMPTY;
        }
        long[] parsedEventIds = validateThenParseEventId(eventId);
        long timeInMS = parsedEventIds[0];
        long sequenceNumber = parsedEventIds[1];

        // 0-0 check
        if (timeInMS <= 0 && sequenceNumber <= 0) {
            return RESPUtils.toSimpleError(OutputConstants.STREAM_EVENT_ID_SMALLER_OR_EQUAL_THAN_0_ERROR);
        }

        // incremental id check
        CacheDto cacheDto = RedisLocalMap.LOCAL_MAP.get(streamKey);
        if (Objects.nonNull(cacheDto) && Objects.equals(cacheDto.getValueType(), ValueType.STREAM)) {
            StreamDto streamDto = (StreamDto) cacheDto.getValue();
            if (streamDto.getStreamMap().isEmpty() && timeInMS <= 0) {
                return RESPUtils.toSimpleError(OutputConstants.STREAM_EVENT_ID_SMALLER_OR_EQUAL_THAN_0_ERROR);
            }
            Map.Entry<String, StreamDto.EntryDto> entryDtoEntry = streamDto.getStreamMap().lastEntry();
            long[] parsedTopEventIds = validateThenParseEventId(entryDtoEntry.getKey());
            long topTimeInMS = parsedTopEventIds[0];
            long topSequenceNumber = parsedTopEventIds[1];
            if ((timeInMS < topTimeInMS) || (timeInMS == topTimeInMS && sequenceNumber <= topSequenceNumber)) {
                return RESPUtils.toSimpleError(OutputConstants.STREAM_EVENT_ID_SMALLER_OR_EQUAL_THAN_TOP_EVENT_ID_ERROR);
            }
        }
        return OutputConstants.EMPTY;
    }

    private long[] validateThenParseEventId(String eventId) {
        if (Objects.isNull(eventId) || eventId.isEmpty()) {
            throw new RuntimeException("invalid param");
        }
        String[] arr = eventId.split(OutputConstants.DASH_DELIMITER);
        if (arr.length < 2 || arr[0].isEmpty() || arr[1].isEmpty()) {
            throw new RuntimeException("invalid param");
        }
        long timeInMS = Long.parseLong(arr[0]);
        long sequenceNumber = Long.parseLong(arr[1]);
        return new long[]{timeInMS, sequenceNumber};
    }
}
