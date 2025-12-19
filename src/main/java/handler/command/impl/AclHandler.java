package handler.command.impl;

import enums.CommandType;
import handler.command.CommandHandler;
import service.RESPUtils;

import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

public class AclHandler implements CommandHandler {
    private static final Logger logger = Logger.getLogger(AclHandler.class.getName());
    private static final String WHOAMI_SUBCOMMAND = "whoami";
    private static final String GET_USER_SUBCOMMAND = "getuser";
    private static final String WHOAMI_DEFAULT_UNAUTHENTICATED_USER = "default";
    private static final String GET_USER_FLAGS = "flags";
    private static final String NO_PASS_FLAG = "nopass";
    private static final String GET_USER_PASSWORDS = "passwords";

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
            logger.info("AclHandler: processing WHOAMI sub-cmd");
            return handleWhoAmISubcommand();
        }
        if (GET_USER_SUBCOMMAND.compareToIgnoreCase(subcommand) == 0) {
            if (list.size() < 2) {
                throw new RuntimeException("invalid param");
            }
            String userName = ((String) list.get(1)).toLowerCase();
            logger.info("AclHandler: processing GETUSER sub-cmd with userName=" + userName);
            return handleGetUserSubcommand(userName);
        }
        throw new RuntimeException("this sub-command has not been implemented yet");
    }

    private String handleWhoAmISubcommand() {
        return RESPUtils.toBulkString(WHOAMI_DEFAULT_UNAUTHENTICATED_USER);
    }

    private String handleGetUserSubcommand(String userName) {
        List<Object> respObjList = new ArrayList<>();
        if (WHOAMI_DEFAULT_UNAUTHENTICATED_USER.equalsIgnoreCase(userName)) {
            respObjList.add(GET_USER_FLAGS);
            respObjList.add(new ArrayList<>(List.of(NO_PASS_FLAG)));
            respObjList.add(GET_USER_PASSWORDS);
            respObjList.add(new ArrayList<>());
            return RESPUtils.toBulkStringFromNestedList(respObjList);
        }
        return RESPUtils.getEmptyArray();
    }
}
