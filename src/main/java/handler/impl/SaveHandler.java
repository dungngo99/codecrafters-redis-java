package handler.impl;

import enums.Command;
import handler.CommandHandler;
import service.RDBLoaderUtils;
import service.RESPUtils;

import java.net.Socket;
import java.util.List;

public class SaveHandler implements CommandHandler {

    @Override
    public void register() {
        CommandHandler.HANDLER_MAP.put(Command.SAVE.name().toLowerCase(), this);
    }

    @Override
    public String process(Socket clientSocket, List list) {
        if (list == null || list.isEmpty()) {
            return "";
        }
        String command = (String) list.get(0);
        if (!Command.SAVE.name().equalsIgnoreCase(command)) {
            return "";
        }
        RDBLoaderUtils.load();
        return RESPUtils.getRESPOk();
    }
}
