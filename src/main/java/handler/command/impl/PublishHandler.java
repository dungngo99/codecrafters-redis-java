package handler.command.impl;

import dto.SubscriberDto;
import enums.CommandType;
import handler.command.CommandHandler;
import service.RESPUtils;
import service.RedisLocalMap;

import java.net.Socket;
import java.util.List;
import java.util.Map;

public class PublishHandler implements CommandHandler {
    @Override
    public void register() {
        CommandHandler.HANDLER_MAP.put(CommandType.PUBLISH.getAlias(), this);
    }

    @Override
    public String process(Socket clientSocket, List list) {
        if (list == null || list.size() < 2) {
            throw new RuntimeException("invalid param");
        }

        String channelName = (String) list.get(0);
        String message = (String) list.get(1);

        Map<String, SubscriberDto> channel = RedisLocalMap.CHANNEL_MAP.get(channelName);
        if (channel == null || channel.isEmpty()) {
            return RESPUtils.toSimpleInt(0);
        }

        return RESPUtils.toSimpleInt(channel.size());
    }
}
