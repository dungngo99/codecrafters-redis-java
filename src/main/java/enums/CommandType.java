package enums;

import java.util.Objects;

public enum CommandType {
    PING("ping", false),
    ECHO("echo", false),
    GET("get", false),
    SET("set", true),
    PX("px", true),
    CONFIG("config", false),
    SAVE("save", false),
    KEYS("keys", false),
    INFO("info", false),
    REPLICATION("replication", false),
    REPLCONF("replconf", false),
    LISTENING_PORT("listening-port", false),
    CAPA("capa", false),
    PSYNC("psync", false),
    GETACK("getack", false),
    ACK("ack", false),
    WAIT("wait", false),
    TYPE("type", false),
    XADD("xadd", true),
    XRANGE("xrange", false),
    XREAD("xread", false),
    BLOCK("block", false),
    INCR("incr", true);

    private final String alias;
    private final boolean isWrite;

    CommandType(String alias, boolean isWrite) {
        this.alias = alias;
        this.isWrite = isWrite;
    }

    public static CommandType fromAlias(String alias) {
        for (CommandType command: values()) {
            if (command.getAlias().equalsIgnoreCase(alias)) {
                return command;
            }
        }
        return null;
    }

    public static boolean isPingCommand(String command) {
        return Objects.nonNull(command)
                && command.toLowerCase().contains(PING.getAlias());
    }

    public static boolean isReplConfListeningPort(String command) {
        return Objects.nonNull(command)
                && command.toLowerCase().contains(REPLCONF.getAlias())
                && command.toLowerCase().contains(LISTENING_PORT.getAlias());
    }

    public static boolean isReplConfCapa(String command) {
        return Objects.nonNull(command)
                && command.toLowerCase().contains(REPLCONF.getAlias())
                && command.toLowerCase().contains(CAPA.getAlias());
    }

    public static boolean isPsync(String command) {
        return Objects.nonNull(command)
                && command.toLowerCase().contains(PSYNC.getAlias());
    }

    public String getAlias() {
        return alias;
    }

    public boolean isWrite() {
        return isWrite;
    }
}
