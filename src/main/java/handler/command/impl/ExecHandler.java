package handler.command.impl;

import constants.OutputConstants;
import dto.JobDto;
import enums.CommandType;
import handler.command.CommandHandler;
import handler.job.JobHandler;
import service.RESPUtils;
import service.ServerUtils;

import java.net.Socket;
import java.util.List;

public class ExecHandler implements CommandHandler {
    @Override
    public void register() {
        CommandHandler.HANDLER_MAP.put(CommandType.EXEC.getAlias(), this);
    }

    @Override
    public String process(Socket clientSocket, List list) {
        if (list == null) {
            throw new RuntimeException("invalid param");
        }
        String jobId = ServerUtils.formatIdFromSocket(clientSocket);
        JobDto jobDto = JobHandler.JOB_MAP.get(jobId);
        if (jobDto == null || !jobDto.isCommandAtomic()) {
            return RESPUtils.toSimpleError(OutputConstants.EXEC_WITHOUT_COMMAND_ERROR);
        }
        return null;
    }
}
