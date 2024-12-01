package handler.command.impl;

import constants.OutputConstants;
import enums.CommandType;
import handler.command.CommandHandler;
import service.RESPUtils;
import service.StreamUtils;

import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

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
        List<String> streamKeyList = parseStreamKeyList(list);
        int streamKeySize = streamKeyList.size();
        List<String> entryIdList = parseEntryIdList(list, 1+streamKeySize, streamKeySize);

        List<Object> objList = new ArrayList<>();
        for (int i=0; i<streamKeySize; i++) {
            String streamKey = streamKeyList.get(i);
            String entryId = entryIdList.get(i);
            Long[] parsedStartEventIds = StreamUtils.parseStartEventId(entryId);
            Long[] parsedEndEventIds = StreamUtils.parseEndEventId(OutputConstants.DEFAULT_END_EVENT_ID);

            List<Object> streamListByRange = StreamUtils.getStreamListByRange(streamKey, parsedStartEventIds, parsedEndEventIds);
            if (streamListByRange != null) {
                List<Object> subObjList = new ArrayList<>();
                subObjList.add(streamKey);
                subObjList.add(streamListByRange);
                objList.add(subObjList);
            }
        }
        return RESPUtils.toBulkStringFromNestedList(objList);
    }

    private List<String> parseStreamKeyList(List list) {
        List<String> streamKeyList = new ArrayList<>();
        int i = 1;
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
}
