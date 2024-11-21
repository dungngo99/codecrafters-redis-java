package handler.command.impl;

import enums.CommandType;
import handler.command.CommandHandler;
import service.RDBLoaderUtils;
import service.RESPUtils;

import java.net.Socket;
import java.util.List;

public class SaveHandler implements CommandHandler {

    @Override
    public void register() {
        CommandHandler.HANDLER_MAP.put(CommandType.SAVE.name().toLowerCase(), this);
    }

    @Override
    public String process(Socket clientSocket, List list) {
        if (list == null || list.isEmpty()) {
            return "";
        }
        String command = (String) list.get(0);
        if (!CommandType.SAVE.name().equalsIgnoreCase(command)) {
            return "";
        }
        RDBLoaderUtils.load();
        return RESPUtils.getRESPOk();
    }
}
