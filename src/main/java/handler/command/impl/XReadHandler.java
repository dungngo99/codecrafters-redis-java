package handler.command.impl;

import constants.OutputConstants;
import enums.CommandType;
import handler.command.CommandHandler;
import service.RESPUtils;
import service.StreamUtils;

import java.net.Socket;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

public class XReadHandler implements CommandHandler {
    @Override
    public void register() {
        CommandHandler.HANDLER_MAP.put(CommandType.XREAD.getAlias(), this);
    }

    @Override
    public String process(Socket clientSocket, List list) {
        if (list == null || list.size() < 3) {
            throw new RuntimeException("invalid param");
        }

        // step 1: check command params if it has block
        boolean hasBlockCommand;
        int offset;
        if (Objects.equals(list.get(0), CommandType.BLOCK.getAlias())) {
            hasBlockCommand = true;
            offset = OutputConstants.XREAD_COMMAND_PARAM_OFFSET_WITH_BLOCKING;
        } else {
            hasBlockCommand = false;
            offset = OutputConstants.XREAD_COMMAND_PARAM_OFFSET_WITHOUT_BLOCKING;
        }

        // step 2: parse stream key list and entry id list from command params
        List<String> streamKeyList = parseStreamKeyList(list, offset);
        List<String> entryIdList = parseEntryIdList(list, offset+streamKeyList.size(), streamKeyList.size());

        // step 3: separate logic to handle either block or no block
        List<Object> objList = new ArrayList<>();
        if (hasBlockCommand) {
            handleXReadWithBlocking(list, streamKeyList, entryIdList, objList);
        } else {
            handleXReadWithoutBlocking(streamKeyList, entryIdList, objList);
        }

        // step 4: convert to RESP format then return
        return RESPUtils.toBulkStringFromNestedList(objList);
    }

    private List<String> parseStreamKeyList(List list, int offset) {
        List<String> streamKeyList = new ArrayList<>();
        int i = offset;
        while (i < list.size()) {
            String entryId = (String) list.get(i++);
            if (StreamUtils.isValidEntryId(entryId)) {
                break;
            }
            streamKeyList.add(entryId);
        }
        return streamKeyList;
    }

    private List<String> parseEntryIdList(List list, int offset, int size) {
        List<String> entryIdList = new ArrayList<>(size);
        for (int i=offset; i<offset+size; i++) {
            entryIdList.add((String) list.get(i));
        }
        return entryIdList;
    }

    private void handleXReadWithBlocking(List list,
                                         List<String> streamKeyList,
                                         List<String> entryIdList,
                                         List<Object> objList) {
        Map<Object, Object> orderMap = Collections.synchronizedMap(new LinkedHashMap<>());
        ExecutorService executor = Executors.newSingleThreadExecutor();
        Runnable task = () -> {
           try {
               while (true) {
                   for (int i=0; i<streamKeyList.size(); i++) {
                       String streamKey = streamKeyList.get(i);
                       String entryId = entryIdList.get(i);
                       Long[] parsedStartEventIds = StreamUtils.parseStartEventId(entryId);
                       Long[] parsedEndEventIds = StreamUtils.parseEndEventId(OutputConstants.DEFAULT_END_EVENT_ID);

                       // workaround to make sure query by range is exclusive
                       StreamUtils.incrementEventIdSequenceNumber(parsedStartEventIds);
                       List<Object> streamListByRange = StreamUtils.getStreamListByRange(streamKey, parsedStartEventIds, parsedEndEventIds);
                       if (streamListByRange != null) {
                           orderMap.remove(streamKey);
                           orderMap.put(streamKey, streamListByRange);
                       }
                   }
                   Thread.sleep(Duration.of(OutputConstants.THREAD_SLEEP_100_MICROS, ChronoUnit.MICROS));
               }
           } catch (InterruptedException ignore) {
               // ignore handling exception
           }
        };

        Future<?> future = executor.submit(task);
        try {
            long timeout = Long.parseLong((String) list.get(1));
            if (timeout > 0) {
                future.get(timeout, TimeUnit.MILLISECONDS);
            } else {
                future.get(OutputConstants.DEFAULT_XREAD_COMMAND_WAIT_TIME_WITHOUT_BLOCKING_MS, TimeUnit.MILLISECONDS);
            }
        } catch (Exception ignore) {
            // ignore handling exception
        }

        orderMap.forEach((k,v) -> {
            String castK = (String) k;
            List<Object> castV = (List<Object>) v;
            if (Objects.nonNull(castK) && Objects.nonNull(castV) && !castV.isEmpty()) {
                List<Object> subObjList = new ArrayList<>();
                subObjList.add(castK);
                subObjList.add(castV);
                objList.add(subObjList);
            }
        });
    }

    private void handleXReadWithoutBlocking(List<String> streamKeyList,
                                            List<String> entryIdList,
                                            List<Object> objList) {
        for (int i=0; i<streamKeyList.size(); i++) {
            String streamKey = streamKeyList.get(i);
            String entryId = entryIdList.get(i);
            Long[] parsedStartEventIds = StreamUtils.parseStartEventId(entryId);
            Long[] parsedEndEventIds = StreamUtils.parseEndEventId(OutputConstants.DEFAULT_END_EVENT_ID);

            // workaround to make sure query by range is exclusive
            StreamUtils.incrementEventIdSequenceNumber(parsedStartEventIds);
            List<Object> streamListByRange = StreamUtils.getStreamListByRange(streamKey, parsedStartEventIds, parsedEndEventIds);
            if (streamListByRange != null && !streamListByRange.isEmpty()) {
                List<Object> subObjList = new ArrayList<>();
                subObjList.add(streamKey);
                subObjList.add(streamListByRange);
                objList.add(subObjList);
            }
        }
    }
}
