package handler.command.impl;

import constants.OutputConstants;
import enums.CommandType;
import handler.command.CommandHandler;
import replication.MasterManager;
import service.RESPUtils;

import java.net.Socket;
import java.util.List;

public class WaitHandler implements CommandHandler {
    @Override
    public void register() {
        CommandHandler.HANDLER_MAP.put(CommandType.WAIT.getAlias(), this);
    }

    @Override
    public String process(Socket clientSocket, List list) {
        if (list == null || list.isEmpty()) {
            throw new RuntimeException("invalid param");
        }
        try {
            int num = MasterManager.getNumConnectedReplica();
            return RESPUtils.toSimpleInt(num);
        } catch (Exception e) {
            System.out.println("failed to get num connected replicas due to " + e.getMessage());
            return RESPUtils.toSimpleInt(OutputConstants.DEFAULT_NO_REPLICA_CONNECTION);
        }
    }
}
