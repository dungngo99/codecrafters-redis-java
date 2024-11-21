package enums;

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
    ACK("ack", false);

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

    public String getAlias() {
        return alias;
    }

    public boolean isWrite() {
        return isWrite;
    }
}
