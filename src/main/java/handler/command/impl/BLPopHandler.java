package handler.command.impl;

import constants.OutputConstants;
import domain.BlockListDto;
import domain.CacheDto;
import enums.CommandType;
import enums.ValueType;
import handler.command.CommandHandler;
import service.RESPUtils;
import service.RedisLocalMap;

import java.net.Socket;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

public class BLPopHandler implements CommandHandler {
    private static final Logger logger = Logger.getLogger(BLPopHandler.class.getName());

    @Override
    public void register() {
        CommandHandler.HANDLER_MAP.put(CommandType.BLPOP.getAlias(), this);
    }

    @Override
    public String process(Socket clientSocket, List list) {
        if (list == null || list.isEmpty() || list.size() < 2) {
            throw new RuntimeException("invalid param");
        }

        String key = (String) list.get(0);
        double timeoutInSec = Double.parseDouble((String) list.get(1));

        CacheDto cache;
        if (RedisLocalMap.LOCAL_MAP.containsKey(key)) {
            cache = RedisLocalMap.LOCAL_MAP.get(key);
            if (!ValueType.isList(cache.getValueType()) || !(cache.getValue() instanceof Collection<?>)) {
                throw new RuntimeException("RPushHandler: command not applied to stored value");
            }
        } else {
            cache = new CacheDto();
            cache.setValueType(ValueType.LIST);
            cache.setValue(new LinkedBlockingDeque<>());
            RedisLocalMap.LOCAL_MAP.put(key, cache);
        }

        if (timeoutInSec > 0) {
            // let the current thread process
            LinkedBlockingDeque<Object> cacheValue = (LinkedBlockingDeque<Object>) cache.getValue();
            return processWithNonzeroTimeout(key, cacheValue, timeoutInSec);
        } else {
            // let the BLPop child thread process
            return processWithZeroTimeout(clientSocket, key);
        }
    }

    private String processWithZeroTimeout(Socket clientSocket, String key) {
        CacheDto cache = RedisLocalMap.LOCAL_MAP.get(key);
        LinkedBlockingDeque<Object> storedList = (LinkedBlockingDeque<Object>) cache.getValue();
        if (storedList != null && !storedList.isEmpty()) {
            try {
                String value = (String) storedList.takeFirst();
                List<String> resultList = new ArrayList<>(List.of(key, value));
                return RESPUtils.toArray(resultList);
            } catch (Exception ex) {
                logger.warning("processWithZeroTimeout0: failed due to " + ex.getMessage());
                return OutputConstants.EMPTY;
            }
        } else {
            BlockListDto blockListDto = new BlockListDto();
            blockListDto.setSocket(clientSocket);
            blockListDto.setKey(key);
            RedisLocalMap.BLPOP_CLIENT_BLOCK_QUEUE.addLast(blockListDto);
            logger.info("processWithZeroTimeout: BLPOP client block queue size=" + RedisLocalMap.BLPOP_CLIENT_BLOCK_QUEUE.size());
            return OutputConstants.EMPTY;
        }
    }

    private String processWithNonzeroTimeout(String key, LinkedBlockingDeque<Object> cacheValue, double timeoutInSec) {
        String value = null;
        try {
            long timeInMillis = Double.valueOf(timeoutInSec * OutputConstants.SECOND_TO_MILLISECOND).longValue();
            value = (String) cacheValue.pollFirst(timeInMillis, TimeUnit.MILLISECONDS);
        } catch (InterruptedException ex) {
            logger.warning("processWithNonzeroTimeout: failed due to " + ex.getMessage());
        }

        logger.info("processWithNonzeroTimeout: done, return value.");
        if (value == null) {
            return RESPUtils.getBulkNullArray();
        } else {
            return RESPUtils.toArray(List.of(key, value));
        }
    }
}
