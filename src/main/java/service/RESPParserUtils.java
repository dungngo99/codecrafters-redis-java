package service;

import dto.JobDto;
import dto.ParserDto;
import enums.CommandType;
import handler.command.CommandHandler;
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

    public static String convertList2Str(ParserDto<List<Object>> parserDto) {
        // init
        List<Object> list = parserDto.getValue();
        Socket clientSocket = parserDto.getSocket();

        // pre-check
        if (list.isEmpty() || clientSocket == null) {
            return "";
        }
        String alias = (String) list.get(0);
        CommandHandler commandHandler = CommandHandler.HANDLER_MAP.getOrDefault(alias.toLowerCase(), null);
        if (commandHandler == null) {
            return "";
        }

        // handle queueing commands per multi
        String jobId = ServerUtils.formatIdFromSocket(clientSocket);
        JobDto jobDto = JobHandler.JOB_MAP.get(jobId);
        if (jobDto.isCommandAtomic() && !Objects.equals(CommandType.fromAlias(alias), CommandType.EXEC)) {
            return MultiHandler.queueCommand(clientSocket, list);
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
        return val != null ? val : RESPUtils.getBulkNull();
    }
}
