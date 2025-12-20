package handler.command.impl;

import domain.AclConfigDto;
import enums.CommandType;
import handler.command.CommandHandler;
import service.HashUtils;
import service.RESPUtils;
import service.RedisLocalMap;
import service.StringUtils;

import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.logging.Logger;

public class AclHandler implements CommandHandler {
    private static final Logger logger = Logger.getLogger(AclHandler.class.getName());
    /** SUBCOMMANDS */
    private static final String WHOAMI_SUBCOMMAND = "whoami";
    private static final String GET_USER_SUBCOMMAND = "getuser";
    private static final String SET_USER_SUBCOMMAND = "setuser";
    /** USER NAMES */
    private static final String WHOAMI_USER_NAME_DEFAULT = "default";
    /** ACL CONFIGURATION KEYS */
    private static final String FLAGS_KEY = "flags";
    private static final String PASSWORDS_KEY = "passwords";
    /** ACL CONFIGURATION VALUES */
    private static final String NO_PASS_FLAGS_VALUE = "nopass";
    /** ACL RULES */
    private static final String PASSWORDS_RULE = ">";

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
        if (SET_USER_SUBCOMMAND.compareToIgnoreCase(subcommand) == 0) {
            if (list.size() < 3) {
                throw new RuntimeException("invalid param");
            }
            String userName = ((String) list.get(1)).toLowerCase();
            String configurationValue = (String) list.get(2);
            logger.info("AclHandler: processing SETUSER sub-cmd with userName=" + userName);
            return handleSetUserSubcommand(userName, configurationValue);
        }
        throw new RuntimeException("this sub-command has not been implemented yet");
    }

    private String handleWhoAmISubcommand() {
        return RESPUtils.toBulkString(WHOAMI_USER_NAME_DEFAULT);
    }

    private String handleGetUserSubcommand(String userName) {
        List<Object> respObjList = new ArrayList<>();
        AclConfigDto aclConfigDto = (AclConfigDto) RedisLocalMap.ACL_MAP.get(userName);
        if (WHOAMI_USER_NAME_DEFAULT.equalsIgnoreCase(userName)) {
            respObjList.add(FLAGS_KEY);
            if (Objects.nonNull(aclConfigDto) && StringUtils.isNotBlank(aclConfigDto.getPasswordHash())) {
                respObjList.add(new ArrayList<>());
            } else {
                respObjList.add(new ArrayList<>(List.of(NO_PASS_FLAGS_VALUE)));
            }
            respObjList.add(PASSWORDS_KEY);
            if (Objects.nonNull(aclConfigDto)) {
                List<String> passwordValues = new ArrayList<>(List.of(aclConfigDto.getPasswordHash()));
                respObjList.add(passwordValues);
            } else {
                respObjList.add(new ArrayList<>());
            }
            return RESPUtils.toBulkStringFromNestedList(respObjList);
        }
        return RESPUtils.getEmptyArray();
    }

    private String handleSetUserSubcommand(String userName, String configurationValue) {
        if (configurationValue.startsWith(PASSWORDS_RULE)) {
            String password = configurationValue.substring(1);
            AclConfigDto configurationDto = (AclConfigDto) RedisLocalMap.ACL_MAP.computeIfAbsent(userName, (k) -> new AclConfigDto());
            configurationDto.setPasswordHash(HashUtils.convertToSHA256(password));
        }
        return RESPUtils.getRESPOk();
    }
}
