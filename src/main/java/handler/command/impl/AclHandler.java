package handler.command.impl;

import domain.AclConfigDto;
import enums.CommandType;
import handler.command.CommandHandler;
import service.*;

import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.logging.Logger;

import static constants.OutputConstants.*;

public class AclHandler implements CommandHandler {
    private static final Logger logger = Logger.getLogger(AclHandler.class.getName());

    public static boolean isAclSetUserPassword(List<String> args) {
        return args.size() == 4
                && CommandType.ACL.getAlias().equalsIgnoreCase(args.get(0))
                && SET_USER_SUBCOMMAND.equalsIgnoreCase(args.get(1))
                && WHOAMI_USER_NAME_DEFAULT.equalsIgnoreCase(args.get(2))
                && args.get(3).startsWith(PASSWORDS_RULE);
    }

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
            String clientSocketId = ServerUtils.formatIdFromSocket(clientSocket);
            logger.info("AclHandler: processing SETUSER sub-cmd with userName=" + userName);
            return handleSetUserSubcommand(userName, configurationValue, clientSocketId);
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

    private String handleSetUserSubcommand(String userName, String configurationValue, String clientSocketId) {
        if (configurationValue.startsWith(PASSWORDS_RULE)) {
            String password = configurationValue.substring(1);
            AclConfigDto configurationDto = (AclConfigDto) RedisLocalMap.ACL_MAP.computeIfAbsent(userName, (k) -> new AclConfigDto());
            configurationDto.setPasswordHash(HashUtils.convertToSHA256(password));
            RedisLocalMap.AUTHENTICATED_CONNECTION_SET.add(clientSocketId);
        }
        return RESPUtils.getRESPOk();
    }
}
