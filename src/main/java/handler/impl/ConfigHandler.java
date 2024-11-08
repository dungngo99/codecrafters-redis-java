package handler.impl;

import constants.OutputConstants;
import enums.Command;
import handler.CommandHandler;
import service.RESPParserUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

public class ConfigHandler implements CommandHandler {

    private static final Map<String, Function<List<Object>, String>> SUB_COMMANDS = new HashMap<>();

    @Override
    public void register() {
        CommandHandler.HANDLER_MAP.put(Command.CONFIG.name().toLowerCase(), this);
        SUB_COMMANDS.put(
                Command.GET.name().toLowerCase(), this::handleGet
        );
    }

    @Override
    public String process(List list) {
        if (list == null || list.size() < 2) {
            throw new RuntimeException("invalid param");
        }
        String subcommand = ((String) list.get(0)).toLowerCase();
        if (!isValidSubCommand(subcommand)) {
            throw new RuntimeException("invalid param");
        }
        return SUB_COMMANDS.get(subcommand).apply(list.subList(1, list.size()));
    }

    private boolean isValidSubCommand(String subcommand) {
        return subcommand != null && SUB_COMMANDS.containsKey(subcommand);
    }

    private String handleGet(List<Object> params) {
        if (params == null || params.isEmpty()) {
            return "";
        }
        String param1 = ((String) params.get(0)).toLowerCase();
        if (OutputConstants.DIR.equalsIgnoreCase(param1) || OutputConstants.DB_FILENAME.equalsIgnoreCase(param1)) {
            String val = System.getProperty(param1);
            if (val != null) {
                return RESPParserUtils.toRESPString(List.of(param1, val));
            }
        }
        return "";
    }
}
