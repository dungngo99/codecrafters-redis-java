package handler.impl;

import constants.OutputConstants;
import enums.Command;
import handler.CommandHandler;

import java.util.List;
import java.util.StringJoiner;

public class EchoHandler implements CommandHandler {
    @Override
    public void register() {
        CommandHandler.HANDLER_MAP.put(Command.ECHO.name().toLowerCase(), this);
    }

    @Override
    public String process(List list) {
        if (list == null || list.isEmpty()) {
            return "";
        }
        StringJoiner joiner = new StringJoiner(OutputConstants.CRLF, "+", OutputConstants.CRLF);
        joiner.add((String) list.getFirst());
        return joiner.toString();
    }
}
