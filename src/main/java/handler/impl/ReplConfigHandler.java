package handler.impl;

import enums.Command;
import handler.CommandHandler;
import service.RESPUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

public class ReplConfigHandler implements CommandHandler {

    private static final Map<String, Function<List<Object>, String>> SUB_COMMANDS = new HashMap<>();

    @Override
    public void register() {
        CommandHandler.HANDLER_MAP.put(Command.REPLCONF.name().toLowerCase(), this);
        SUB_COMMANDS.put(Command.LISTENING_PORT.getAlias().toLowerCase(), this::handleListeningPort);
        SUB_COMMANDS.put(Command.CAPA.name().toLowerCase(), this::handleCapa);
    }

    @Override
    public String process(List list) {
        if (list == null || list.size() < 2) {
            throw new RuntimeException("invalid param");
        }
        String subcommand = ((String) list.get(0)).toLowerCase();
        if (!SUB_COMMANDS.containsKey(subcommand)) {
            throw new RuntimeException("invalid param");
        }
        return SUB_COMMANDS.get(subcommand).apply(list.subList(1, list.size()));
    }

    private String handleListeningPort(List list) {
        return RESPUtils.getRESPOk();
    }

    private String handleCapa(List list) {
        return RESPUtils.getRESPOk();
    }
}
