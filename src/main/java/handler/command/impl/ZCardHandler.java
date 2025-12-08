package handler.command.impl;

import domain.CacheDto;
import domain.ZSet;
import enums.CommandType;
import enums.ValueType;
import handler.command.CommandHandler;
import service.RESPUtils;
import service.RedisLocalMap;

import java.net.Socket;
import java.util.List;
import java.util.Objects;

public class ZCardHandler implements CommandHandler {
    @Override
    public void register() {
        CommandHandler.HANDLER_MAP.put(CommandType.ZCARD.getAlias(), this);
    }

    @Override
    public String process(Socket clientSocket, List list) {
        if (list == null || list.isEmpty()) {
            throw new RuntimeException("invalid param");
        }

        String zSetKey = (String) list.get(0);

        CacheDto cache = RedisLocalMap.LOCAL_MAP.get(zSetKey);
        if (Objects.isNull(cache)) {
            return RESPUtils.toSimpleInt(0);
        }
        if (!ValueType.isZSet(cache.getValueType()) || !(cache.getValue() instanceof ZSet zSet)) {
            throw new RuntimeException("ZAddHandler: command not applied to stored value");
        }

        int cardinality = zSet.getZSET_SCORE_MAP().size();
        return RESPUtils.toSimpleInt(cardinality);
    }
}
