package handler.impl;

import constants.OutputConstants;
import enums.Command;
import handler.CommandHandler;
import replication.MasterManager;
import service.RESPUtils;
import service.SystemPropHelper;

import java.net.Socket;
import java.util.List;

public class PsyncHandler implements CommandHandler {
    @Override
    public void register() {
        CommandHandler.HANDLER_MAP.put(Command.PSYNC.name().toLowerCase(), this);
    }

    @Override
    public String process(Socket clientSocket, List list) {
        if (list == null || list.size() < 1) {
            throw new IllegalArgumentException("invalid param");
        }
        handleReplicaNodeConnection(clientSocket);
        String masterReplicationID = SystemPropHelper.getSetMasterReplId();
        String str = String.format("%s %s %s", OutputConstants.REPLICA_FULL_RESYNC, masterReplicationID, OutputConstants.MASTER_REPL_OFFSET_DEFAULT);
        return RESPUtils.toSimpleString(str);
    }

    private void handleReplicaNodeConnection(Socket clientSocket) {
        new Thread(() -> {
            try {
                Thread.sleep(OutputConstants.THREAD_SLEEP_100_MILLIS);
                MasterManager.transferEmptyRDBFile(clientSocket);
                MasterManager.registerReplicaNodeConnection(clientSocket);
            } catch (Exception e) {
                System.out.println("failed to handle replica node connection due to " + e.getMessage());
            }
        }).start();
    }
}
