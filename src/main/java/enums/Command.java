package enums;

import java.util.Objects;

public enum Command {
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
    PSYNC("psync", false);

    private String alias;
    private boolean isWrite;

    Command(String alias, boolean isWrite) {
        this.alias = alias;
        this.isWrite = isWrite;
    }

    public static Command fromAlias(String alias) {
        for (Command command: values()) {
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
