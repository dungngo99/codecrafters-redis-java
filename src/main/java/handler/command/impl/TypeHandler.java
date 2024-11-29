package handler.command.impl;

import constants.OutputConstants;
import enums.CommandType;
import handler.command.CommandHandler;
import service.RESPUtils;
import service.RedisLocalMap;

import java.net.Socket;
import java.util.List;

public class TypeHandler implements CommandHandler {
    @Override
    public void register() {
        CommandHandler.HANDLER_MAP.put(CommandType.TYPE.getAlias(), this);
    }

    @Override
    public String process(Socket clientSocket, List list) {
        if (list == null || list.isEmpty()) {
            throw new RuntimeException("invalid param");
        }
        String key = (String) list.getFirst();
        if (!RedisLocalMap.LOCAL_MAP.containsKey(key)) {
            return RESPUtils.toSimpleString(OutputConstants.NONE_COMMAND_TYPE_FOR_MISSING_KEY);
        }
        return RESPUtils.toSimpleString(OutputConstants.STRING_COMMAND_TYPE);
    }
}
