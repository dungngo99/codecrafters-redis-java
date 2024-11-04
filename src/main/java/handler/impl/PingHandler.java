package handler.impl;

import constants.OutputConstants;
import enums.Command;
import handler.CommandHandler;

import java.util.List;
import java.util.StringJoiner;

public class PingHandler implements CommandHandler {

    @Override
    public void register() {
        CommandHandler.HANDLER_MAP.put(Command.PING.name().toLowerCase(), this);
    }

    @Override
    public String process(List list) {
        StringJoiner joiner = new StringJoiner(OutputConstants.CRLF, "+", OutputConstants.CRLF);
        joiner.add(OutputConstants.PONG);
        return joiner.toString();
    }
}
