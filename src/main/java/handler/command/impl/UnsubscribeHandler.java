package handler.command.impl;

import dto.ChannelDto;
import dto.SubscriberDto;
import enums.CommandType;
import handler.command.CommandHandler;
import service.RESPUtils;
import service.RedisLocalMap;
import service.ServerUtils;

import java.net.Socket;
import java.util.List;
import java.util.Map;

public class UnsubscribeHandler implements CommandHandler {

    @Override
    public void register() {
        CommandHandler.HANDLER_MAP.put(CommandType.UNSUBSCRIBE.getAlias(), this);
    }

    @Override
    public String process(Socket clientSocket, List list) {
        if (list == null || list.isEmpty()) {
            throw new RuntimeException("invalid param");
        }

        String channelName = (String) list.get(0);
        String subscriberId = ServerUtils.formatIdFromSocket(clientSocket);

        // update CHANNEL_MAP
        Map<String, SubscriberDto> subscriberMap = RedisLocalMap.CHANNEL_MAP.getOrDefault(channelName, null);
        if (subscriberMap == null) {
            List<Object> responseList = List.of(
                    CommandType.UNSUBSCRIBE.getAlias(),
                    channelName,
                    0);
            return RESPUtils.toBulkStringFromNestedList(responseList);
        }
        subscriberMap.remove(subscriberId);

        // update SUBSCRIBER_MAP
        Map<String, ChannelDto> channelMap = RedisLocalMap.SUBSCRIBER_MAP.getOrDefault(subscriberId, null);
        if (channelMap != null) {
            channelMap.remove(channelName);
        }

        List<Object> responseList = List.of(
                CommandType.UNSUBSCRIBE.getAlias(),
                channelName,
                subscriberMap.size());
        return RESPUtils.toBulkStringFromNestedList(responseList);
    }
}
