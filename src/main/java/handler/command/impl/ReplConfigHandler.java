package handler.command.impl;

import constants.OutputConstants;
import enums.CommandType;
import handler.command.CommandHandler;
import handler.job.impl.PropagateHandler;
import service.RESPUtils;
import service.ServerUtils;

import java.net.Socket;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

public class ReplConfigHandler implements CommandHandler {

    private static final Map<String, Function<List<Object>, String>> SUB_COMMANDS = new HashMap<>();

    @Override
    public void register() {
        CommandHandler.HANDLER_MAP.put(CommandType.REPLCONF.name().toLowerCase(), this);
        SUB_COMMANDS.put(CommandType.LISTENING_PORT.getAlias().toLowerCase(), this::handleListeningPort);
        SUB_COMMANDS.put(CommandType.CAPA.name().toLowerCase(), this::handleCapa);
        SUB_COMMANDS.put(CommandType.GETACK.name().toLowerCase(), this::handleGetAck);
        SUB_COMMANDS.put(CommandType.ACK.name().toLowerCase(), this::handleAck);
    }

    @Override
    public String process(Socket clientSocket, List list) {
        if (list == null || list.size() < 2) {
            throw new RuntimeException("invalid param");
        }
        String subcommand = ((String) list.get(0)).toLowerCase();
        if (!SUB_COMMANDS.containsKey(subcommand)) {
            throw new RuntimeException("invalid param");
        }
        return SUB_COMMANDS.get(subcommand).apply(list.subList(1, list.size()));
    }

    /**
     * Master handles listening port from Replica
     * @param list subcommands
     * @return
     */
    private String handleListeningPort(List list) {
        return RESPUtils.getRESPOk();
    }

    /**
     * Master handles capacity from Replica
     * @param list subcommands
     * @return
     */
    private String handleCapa(List list) {
        return RESPUtils.getRESPOk();
    }

    /**
     * Replica handles GETACK from Master
     * @param list subcommands
     * @return
     */
    private String handleGetAck(List list) {
        return RESPUtils.respondRESPReplConfAckWithDefaultOffset();
    }

    /**
     * Master handles ACK from Replica
     * @param list
     * @return
     */
    private String handleAck(List list) {
        System.out.printf("received ACK=%s from replica\n", list);
        return OutputConstants.EMPTY;
    }
}