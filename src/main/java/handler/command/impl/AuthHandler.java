package handler.command.impl;

import domain.AclConfigDto;
import enums.CommandType;
import handler.command.CommandHandler;
import service.HashUtils;
import service.RESPUtils;
import service.RedisLocalMap;
import service.StringUtils;

import java.net.Socket;
import java.util.List;
import java.util.Objects;
import java.util.logging.Logger;

public class AuthHandler implements CommandHandler {
    private static final Logger logger = Logger.getLogger(AuthHandler.class.getName());
    private static final String WRONGPASS_ERROR_MESSAGE = "WRONGPASS invalid username-password pair or user is disabled.";

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

        return RESPUtils.getRESPOk();
    }
}
