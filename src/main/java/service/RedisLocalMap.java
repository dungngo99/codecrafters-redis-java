package service;

import domain.BlockListDto;
import domain.CacheDto;
import domain.ChannelDto;
import domain.SubscriberDto;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingDeque;

public class RedisLocalMap {

    public static final Map<String, CacheDto> LOCAL_MAP = new ConcurrentHashMap<>();

    public static final LinkedBlockingDeque<BlockListDto> BLPOP_CLIENT_BLOCK_QUEUE = new LinkedBlockingDeque<>();

    /**
     * Map<key=channelName, value=Map<key=subscriberId, value=subscriberDto>>
     */
    public static final Map<String, Map<String, SubscriberDto>> CHANNEL_MAP = new ConcurrentHashMap<>();

    /**
     * Map<key=subscriberId, value=Map<key=channelName, value=ChannelDto>>
     */
    public static final Map<String, Map<String, ChannelDto>> SUBSCRIBER_MAP = new ConcurrentHashMap<>();

    public static final Set<String> SUBSCRIBE_MODE_SET = new HashSet<>();
}
