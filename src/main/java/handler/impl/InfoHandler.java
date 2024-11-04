package handler.impl;

import constants.OutputConstants;
import enums.Command;
import handler.CommandHandler;
import service.SystemPropHelper;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringJoiner;
import java.util.function.Function;

public class InfoHandler implements CommandHandler {

    private static final Map<String, Function<List<Object>, String>> SUB_COMMANDS = new HashMap<>();

    @Override
    public void register() {
        CommandHandler.HANDLER_MAP.put(Command.INFO.name().toLowerCase(), this);
        SUB_COMMANDS.put(
                Command.REPLICATION.name().toLowerCase(), this::processReplication
        );
    }

    @Override
    public String process(List list) {
        if (list == null || list.isEmpty()) {
            throw new RuntimeException("invalid param");
        }
        String subCommand = ((String) list.get(0)).toLowerCase();
        if (!SUB_COMMANDS.containsKey(subCommand)) {
            throw new RuntimeException("invalid param");
        }
        List subList = list.subList(1, list.size());
        return SUB_COMMANDS.get(subCommand).apply(subList);
    }


    private String processReplication(List list) {
        if (!list.isEmpty()) {
            throw new RuntimeException("invalid param");
        }
        String role = SystemPropHelper.getSetServerRoleOrDefault();
        String bulkString = new StringJoiner(OutputConstants.COLON_DELIMITER)
                .add(OutputConstants.REDIS_SERVER_ROLE_TYPE)
                .add(role)
                .toString();
        StringJoiner joiner = new StringJoiner("\r\n", OutputConstants.DOLLAR_SIZE, "\r\n");
        joiner.add(String.valueOf(bulkString.length()));
        joiner.add(bulkString);
        return joiner.toString();
    }
}
