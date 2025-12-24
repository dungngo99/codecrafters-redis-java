package handler.command.impl.auth;

import domain.AclConfigDto;
import enums.CommandType;
import handler.command.CommandHandler;
import service.*;

import java.net.Socket;
import java.util.List;
import java.util.Objects;
import java.util.logging.Logger;

import static constants.OutputConstants.WHOAMI_USER_NAME_DEFAULT;

public class AuthHandler implements CommandHandler {
    private static final Logger logger = Logger.getLogger(AuthHandler.class.getName());
    private static final String WRONGPASS_ERROR_MESSAGE = "WRONGPASS invalid username-password pair or user is disabled.";

    public static boolean isAuth(List<String> args) {
        return args.size() == 3
                && CommandType.AUTH.getAlias().equalsIgnoreCase(args.get(0))
                && WHOAMI_USER_NAME_DEFAULT.equalsIgnoreCase(args.get(1));
    }

    @Override
    public void register() {
        CommandHandler.HANDLER_MAP.put(CommandType.AUTH.getAlias(), this);
    }

    @Override
    public String process(Socket clientSocket, List list) {
        if (list == null || list.size() < 2) {
            throw new RuntimeException("invalid param");
        }
        String userName = (String) list.get(0);
        String password = (String) list.get(1);
        String inputHashPassword = HashUtils.convertToSHA256(password);

        logger.info("AuthHandler: begin to authenticate userName=" + userName + " from ACL config.");
        AclConfigDto aclConfigDto = (AclConfigDto) RedisLocalMap.ACL_MAP.get(userName);
        if (aclConfigDto == null || StringUtils.isBlank(aclConfigDto.getPasswordHash())) {
            return RESPUtils.toSimpleError(WRONGPASS_ERROR_MESSAGE);
        }
        String cachedHashPassword = aclConfigDto.getPasswordHash();

        if (!Objects.equals(inputHashPassword, cachedHashPassword)) {
            return RESPUtils.toSimpleError(WRONGPASS_ERROR_MESSAGE);
        }

        String clientSocketId = ServerUtils.formatIdFromSocket(clientSocket);
        RedisLocalMap.AUTHENTICATED_CONNECTION_SET.add(clientSocketId);

        return RESPUtils.getRESPOk();
    }
}
