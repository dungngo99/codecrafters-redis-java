package handler.impl;

import enums.Command;
import handler.CommandHandler;
import service.RESPUtils;

import java.net.Socket;
import java.util.List;

public class EchoHandler implements CommandHandler {
    @Override
    public void register() {
        CommandHandler.HANDLER_MAP.put(Command.ECHO.name().toLowerCase(), this);
    }

    @Override
    public String process(Socket clientSocket, List list) {
        if (list == null || list.isEmpty()) {
            return "";
        }
        return RESPUtils.getRESPEcho((String) list.getFirst());
    }
}
