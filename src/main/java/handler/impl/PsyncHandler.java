package handler.impl;

import constants.OutputConstants;
import enums.Command;
import handler.CommandHandler;
import service.RESPUtils;
import service.SystemPropHelper;

import java.util.List;

public class PsyncHandler implements CommandHandler {
    @Override
    public void register() {
        CommandHandler.HANDLER_MAP.put(Command.PSYNC.name().toLowerCase(), this);
    }

    @Override
    public String process(List list) {
        if (list == null || list.size() < 1) {
            throw new IllegalArgumentException("invalid param");
        }
        String masterReplicationID = SystemPropHelper.getSetMasterReplId();
        String str = String.format("%s %s %s", OutputConstants.REPLICA_FULL_RESYNC, masterReplicationID, OutputConstants.MASTER_REPL_OFFSET_DEFAULT);
        return RESPUtils.toSimpleString(str);
    }
}
