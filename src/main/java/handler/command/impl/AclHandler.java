package handler.command.impl;

import enums.CommandType;
import handler.command.CommandHandler;
import service.RESPUtils;

import java.net.Socket;
import java.util.List;

public class AclHandler implements CommandHandler {
    private static final String WHOAMI_SUBCOMMAND = "whoami";
    private static final String WHOAMI_DEFAULT_UNAUTHENTICATED_USER = "default";

    @Override
    public void register() {
        CommandHandler.HANDLER_MAP.put(CommandType.ACL.getAlias(), this);
    }

    @Override
    public String process(Socket clientSocket, List list) {
        if (list == null || list.isEmpty()) {
            throw new RuntimeException("invalid param");
        }
        String subcommand = (String) list.get(0);
        if (WHOAMI_SUBCOMMAND.compareToIgnoreCase(subcommand) == 0) {
            return handleWhoAmISubcommand();
        }
        throw new RuntimeException("this sub-command has not been implemented yet");
    }

    private String handleWhoAmISubcommand() {
        return RESPUtils.toBulkString(WHOAMI_DEFAULT_UNAUTHENTICATED_USER);
    }
}
