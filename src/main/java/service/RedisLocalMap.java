package service;

import dto.CacheDto;
import dto.ChannelDto;
import dto.SubscriberDto;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class RedisLocalMap {

    public static final Map<String, CacheDto> LOCAL_MAP = new ConcurrentHashMap<>();

    /**
     * Map<key=channelName, value=Map<key=subscriberId, value=subscriberDto>>
     */
    public static final Map<String, Map<String, SubscriberDto>> CHANNEL_MAP = new ConcurrentHashMap<>();

    /**
     * Map<key=subscriberId, value=Map<key=channelName, value=ChannelDto>>
     */
    public static final Map<String, Map<String, ChannelDto>> SUBSCRIBER_MAP = new ConcurrentHashMap<>();
}
