package handler.command.impl;

import dto.CacheDto;
import enums.CommandType;
import handler.command.CommandHandler;
import replication.MasterManager;
import service.RESPUtils;
import service.RedisLocalMap;

import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

public class SetHandler implements CommandHandler {
    @Override
    public void register() {
        CommandHandler.HANDLER_MAP.put(CommandType.SET.name().toLowerCase(), this);
    }

    @Override
    public String process(Socket clientSocket, List list) {
        if (list == null || list.isEmpty() || list.size() < 2) {
            throw new RuntimeException("invalid param");
        }
        String key = (String) list.get(0);
        Object value = list.get(1);

        Long expiry = null;
        if (list.size() > 2) {
            String px = (String) list.get(2);
            if (CommandType.PX.name().toLowerCase().equalsIgnoreCase(px)) {
                expiry = Long.parseLong((String) list.get(3));
            }
        }

        CacheDto cache = new CacheDto();
        cache.setValue((String) value);
        if (expiry != null) {
            cache.setExpireTime(System.currentTimeMillis() + expiry);
        }
        RedisLocalMap.LOCAL_MAP.put(key, cache);
        return RESPUtils.getRESPOk();
    }

    @Override
    public void propagate(List list) {
        if (list == null || list.isEmpty() || list.size() < 2) {
            throw new RuntimeException("invalid param");
        }
        List<String> strings = new ArrayList<>();
        list.forEach(e -> strings.add((String) e));
        String command = RESPUtils.toArray(strings);
        MasterManager.registerCommand(command);
    }
}
