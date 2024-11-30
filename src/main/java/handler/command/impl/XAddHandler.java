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
import java.util.LinkedHashMap;
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

        // step 1: fill cacheDto (if not exist) and streamDto (if not exist)
        CacheDto cacheDto = RedisLocalMap.LOCAL_MAP.get(streamKey);
        StreamDto streamDto;
        if (Objects.nonNull(cacheDto) && Objects.equals(cacheDto.getValueType(), ValueType.STREAM)) {
            streamDto = (StreamDto) cacheDto.getValue();
        } else {
            cacheDto = new CacheDto();
            streamDto = new StreamDto();
            cacheDto.setValueType(ValueType.STREAM);
            cacheDto.setValue(streamDto);
            RedisLocalMap.LOCAL_MAP.put(streamKey, cacheDto);
        }

        // step 2: parse stream key
        String eventId = (String) list.get(1);
        Long[] parsedEventIdArr = parseEventId(streamDto, eventId);


        // step 3: pre-check stream key
        String error = validateEntryId(streamDto, parsedEventIdArr);
        if (!error.isEmpty()) {
            return error;
        }

        // step 4: populate kv pairs to streamDto
        String finalEventId = formatEventId(parsedEventIdArr);
        StreamDto.EntryDto entryDto = new StreamDto.EntryDto(finalEventId);
        for (int i=2; i<list.size(); i+=2) {
            String key = (String) list.get(i);
            String value = (String) list.get(i+1);
            entryDto.getKvPair().put(key, value);
        }
        streamDto.getStreamMap().put(finalEventId, entryDto);

        // step 5: return RESP
        return RESPUtils.toBulkString(finalEventId);
    }

    private Long[] parseEventId(StreamDto streamDto, String eventId) {
        if (Objects.equals(eventId, OutputConstants.ASTERISK)) {
            // todo: fully-generated time part and sequence number
            return new Long[]{0L, 0L};
        }

        String[] arr = eventId.split(OutputConstants.DASH_DELIMITER);
        if (arr.length < 2 || arr[0].isEmpty() || arr[1].isEmpty()) {
            throw new RuntimeException("invalid param");
        }

        LinkedHashMap<String, StreamDto.EntryDto> streamMap = streamDto.getStreamMap();
        String timePartStr = arr[0];
        long timePart;
        if (Objects.equals(timePartStr, OutputConstants.ASTERISK)) {
            if (streamMap.isEmpty()) {
                timePart = OutputConstants.DEFAULT_TIME_PART_OF_ENTRY_ID;
            } else {
                Long[] parsedTopEventIdArr = parseTopEventIdFromStreamMap(streamMap);
                timePart = parsedTopEventIdArr[0];
            }
        } else {
            timePart = Long.parseLong(timePartStr);
        }

        String sequenceNumStr = arr[1];
        long sequenceNum;
        if (Objects.equals(sequenceNumStr, OutputConstants.ASTERISK)) {
            if (streamMap.isEmpty()) {
                sequenceNum = OutputConstants.DEFAULT_SEQUENCE_NUMBER_OF_ENTRY_ID;
                if (timePart == 0L) {
                    sequenceNum += 1;
                }
            } else {
                Long[] parsedTopEventIdArr = parseTopEventIdFromStreamMap(streamMap);
                Long topTimePart = parsedTopEventIdArr[0];
                Long topSequenceNum = parsedTopEventIdArr[1];
                // note: timePart < topTimePart is not possible
                if (timePart == topTimePart) {
                    sequenceNum = topSequenceNum + 1;
                } else {
                    sequenceNum = OutputConstants.DEFAULT_SEQUENCE_NUMBER_OF_ENTRY_ID;
                }
            }
        } else {
            sequenceNum = Long.parseLong(sequenceNumStr);
        }
        return new Long[]{timePart, sequenceNum};
    }

    /**
     * assume stream map has at least 1 entry so there is no empty check in below impl
     * @param streamMap stream of events
     * @return array of time part and sequence number
     */
    private Long[] parseTopEventIdFromStreamMap(LinkedHashMap<String, StreamDto.EntryDto> streamMap) {
        Map.Entry<String, StreamDto.EntryDto> entryDtoEntry = streamMap.lastEntry();
        String eventId = entryDtoEntry.getKey();
        String[] arr = eventId.split(OutputConstants.DASH_DELIMITER);
        return new Long[]{Long.parseLong(arr[0]), Long.parseLong(arr[1])};
    }

    private String validateEntryId(StreamDto streamDto, Long[] eventIdArr) {
        long timeInMS = eventIdArr[0];
        long sequenceNumber = eventIdArr[1];

        // 0-0 check
        if (timeInMS <= 0 && sequenceNumber <= 0) {
            return RESPUtils.toSimpleError(OutputConstants.STREAM_EVENT_ID_SMALLER_OR_EQUAL_THAN_0_ERROR);
        }

        // incremental id check
        LinkedHashMap<String, StreamDto.EntryDto> streamMap = streamDto.getStreamMap();
        if (streamMap.isEmpty()) {
            return OutputConstants.EMPTY;
        }
        Long[] parsedTopEventIds = parseTopEventIdFromStreamMap(streamMap);
        long topTimeInMS = parsedTopEventIds[0];
        long topSequenceNumber = parsedTopEventIds[1];
        if ((timeInMS < topTimeInMS) || (timeInMS == topTimeInMS && sequenceNumber <= topSequenceNumber)) {
            return RESPUtils.toSimpleError(OutputConstants.STREAM_EVENT_ID_SMALLER_OR_EQUAL_THAN_TOP_EVENT_ID_ERROR);
        }
        return OutputConstants.EMPTY;
    }

    private String formatEventId(Long[] eventIdArr) {
        return eventIdArr[0] + OutputConstants.DASH_DELIMITER + eventIdArr[1];
    }
}
