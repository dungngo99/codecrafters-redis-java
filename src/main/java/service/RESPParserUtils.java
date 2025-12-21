package service;

import constants.OutputConstants;
import domain.AclConfigDto;
import domain.JobDto;
import domain.ParserDto;
import enums.CommandType;
import handler.command.CommandHandler;
import handler.command.impl.AclHandler;
import handler.command.impl.MultiHandler;
import handler.job.JobHandler;
import replication.MasterManager;

import java.net.Socket;
import java.util.List;
import java.util.Objects;

public class RESPParserUtils {

    public static String convertStr2Str(ParserDto<String> parserDto) {
        return parserDto.getValue();
    }

    public static String convertList2Str(ParserDto<List<String>> parserDto) {
        // init
        List<String> list = parserDto.getValue();
        Socket clientSocket = parserDto.getSocket();
        String userName = parserDto.getUserName();

        // pre-check
        if (list.isEmpty()) {
            return "";
        }

        if (Objects.equals(parserDto.getNoProcessCommandHandler(), Boolean.TRUE)) {
            return String.join(OutputConstants.COMMA_DELIMITER, list);
        }

        AclConfigDto aclConfigDto = (AclConfigDto) RedisLocalMap.ACL_MAP.get(userName);
        String clientSocketId = ServerUtils.formatIdFromSocket(clientSocket);
        if (Objects.nonNull(aclConfigDto)
                && StringUtils.isNotBlank(aclConfigDto.getPasswordHash())
                && !RedisLocalMap.AUTHENTICATED_CONNECTION_SET.contains(clientSocketId)
                && !AclHandler.isAclSetUserPassword(list)) {
            return RESPUtils.toSimpleError(OutputConstants.ERROR_MESSAGE_NOAUTH_AUTHENTICATION);
        }

        String alias = list.get(0);
        CommandHandler commandHandler = CommandHandler.HANDLER_MAP.getOrDefault(alias.toLowerCase(), null);
        if (commandHandler == null) {
            return "";
        }

        // handle queueing commands per multi
        String jobId = ServerUtils.formatIdFromSocket(clientSocket);
        JobDto jobDto = JobHandler.JOB_MAP.get(jobId);
        if (jobDto.isCommandAtomic()
                && !Objects.equals(CommandType.fromAlias(alias), CommandType.EXEC)
                && !Objects.equals(CommandType.fromAlias(alias), CommandType.DISCARD)) {
            return MultiHandler.queueCommand(clientSocket, list);
        }

        // check if commands are in subscribed mode
        Boolean isSubscribeMode = RedisLocalMap.SUBSCRIBE_MODE_SET.contains(clientSocketId);
        if (Objects.equals(Boolean.TRUE, isSubscribeMode) && !CommandType.isAllowedCommandInSubscribedMode(alias)) {
            return RESPUtils.getErrorMessageCommandInSubscribeMode(alias);
        }

        // process command
        CommandType command = CommandType.fromAlias(alias);
        List args = list.subList(1, list.size());
        String val = commandHandler.process(clientSocket, args);

        // handle command propagate
        if (command != null
                && command.isWrite()
                && MasterManager.isMasterNode()) {
            new Thread(() -> {
                MasterManager.propagate(list);
                MasterManager.setHasWriteReplicas();
            }).start();
        }

        // return
        return val != null ? val : RESPUtils.getBulkNullString();
    }
}
