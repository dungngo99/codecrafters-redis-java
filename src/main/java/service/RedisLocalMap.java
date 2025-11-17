package service;

import dto.CacheDto;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class RedisLocalMap {

    public static final Map<String, CacheDto> LOCAL_MAP = new ConcurrentHashMap<>();
}
