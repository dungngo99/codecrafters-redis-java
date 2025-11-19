package handler.command.impl;

import constants.OutputConstants;
import dto.BlockListDto;
import dto.CacheDto;
import enums.CommandType;
import enums.ValueType;
import handler.command.CommandHandler;
import service.RESPUtils;
import service.RedisLocalMap;
import service.ServerUtils;

import java.io.IOException;
import java.net.Socket;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Logger;

public class BLPopHandler implements CommandHandler {
    private static final Logger logger = Logger.getLogger(BLPopHandler.class.getName());
    private static final AtomicBoolean IS_SPAWN_CHILD_THREAD = new AtomicBoolean(Boolean.FALSE);
    private static final LinkedBlockingDeque<BlockListDto> BLOCK_LIST_QUEUE = new LinkedBlockingDeque<>();

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
        if (!IS_SPAWN_CHILD_THREAD.get()) {
            new Thread(this::processWithZeroTimeout0).start();
            IS_SPAWN_CHILD_THREAD.set(Boolean.TRUE);
        }
        BlockListDto blockListDto = new BlockListDto();
        blockListDto.setSocket(clientSocket);
        blockListDto.setKey(key);
        BLOCK_LIST_QUEUE.add(blockListDto);
        return OutputConstants.EMPTY;
    }

    private void processWithZeroTimeout0() {
        if (!IS_SPAWN_CHILD_THREAD.get()) {
            return;
        }

        while (true) {
            try {
                logger.info("processWithZeroTimeout0: begin waiting block-list queue to take first conn");
                BlockListDto blockListDto = BLOCK_LIST_QUEUE.takeFirst();
                Socket socket = blockListDto.getSocket();
                String key = blockListDto.getKey();

                CacheDto cache = RedisLocalMap.LOCAL_MAP.get(key);
                LinkedBlockingDeque<Object> storedList = (LinkedBlockingDeque<Object>) cache.getValue();
                logger.info("processWithZeroTimeout0: begin waiting cached linked-block queue to take first value with key=" + key);
                String value = (String) storedList.takeFirst();

                ServerUtils.writeThenFlushString(socket, RESPUtils.toArray(List.of(key, value)));

                if (BLOCK_LIST_QUEUE.isEmpty()) {
                    break;
                }
            } catch (InterruptedException | IOException ex) {
                logger.warning("processWithZeroTimeout0: failed due to " + ex.getMessage());
            }
        }

        logger.info("processWithZeroTimeout0: done, removing child thread.");
        IS_SPAWN_CHILD_THREAD.set(Boolean.FALSE);
    }

    private String processWithNonzeroTimeout(String key, LinkedBlockingDeque<Object> cacheValue, double timeoutInSec) {
        String value = null;
        try {
            long timeInMillis = Double.valueOf(timeoutInSec*OutputConstants.SECOND_TO_MILLISECOND).longValue();
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
