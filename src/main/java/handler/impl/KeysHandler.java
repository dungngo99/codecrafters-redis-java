package handler.impl;

import constants.OutputConstants;
import dto.Cache;
import enums.Command;
import handler.CommandHandler;
import service.RESPUtils;
import service.RedisLocalMap;

import java.util.List;
import java.util.Map;
import java.util.StringJoiner;

public class KeysHandler implements CommandHandler {

    @Override
    public void register() {
        CommandHandler.HANDLER_MAP.put(Command.KEYS.name().toLowerCase(), this);
    }

    @Override
    public String process(List list) {
        if (list == null || list.isEmpty()) {
            return "";
        }
        String arg = (String) list.get(0);
        List<String> ffix = parseFix(arg);
        if (ffix.isEmpty()) {
            return "";
        }
        if (ffix.size() == 1) {
            return handleSingleKey(ffix);
        }
        if (ffix.size() == 2) {
            return handlePreSuffix(ffix);
        }
        return "";
    }

    private List<String> parseFix(String str) {
        if (str == null || str.isBlank()) {
            return List.of();
        }
        if (!str.contains(OutputConstants.KEYS_DELIMITER)) {
            return List.of(str);
        }
        int i = str.indexOf(OutputConstants.KEYS_DELIMITER);
        return List.of(str.substring(0, i), str.substring(i+1));
    }

    private String handleSingleKey(List<String> ffix) {
        return CommandHandler.HANDLER_MAP.get(Command.GET.name().toLowerCase()).process(ffix);
    }

    private String handlePreSuffix(List<String> ffix) {
        String prefix = ffix.get(0);
        String suffix = ffix.get(1);
        List<Map.Entry<String, Cache>> caches = RedisLocalMap.LOCAL_MAP.entrySet().stream().toList();
        if (!prefix.isBlank()) {
            caches = caches.stream()
                    .filter(e -> e.getKey().startsWith(prefix))
                    .toList();
        }
        if (!suffix.isBlank()) {
            caches = caches.stream()
                    .filter(e -> e.getKey().endsWith(suffix))
                    .toList();
        }
        if (caches.isEmpty()) {
            return "";
        }
        List<String> keys = caches.stream()
                .map(Map.Entry::getKey)
                .toList();
        return RESPUtils.toArray(keys);
    }
}
