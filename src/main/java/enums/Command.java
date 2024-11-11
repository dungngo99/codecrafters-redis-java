package enums;

public enum Command {
    PING("ping"),
    ECHO("echo"),
    GET("get"),
    SET("set"),
    PX("px"),
    CONFIG("config"),
    SAVE("save"),
    KEYS("keys"),
    INFO("info"),
    REPLICATION("replication"),
    REPLCONF("replconf"),
    LISTENING_PORT("listening-port"),
    CAPA("capa"),
    PSYNC("psync")
    ;

    private String alias;

    Command(String alias) {
        this.alias = alias;
    }

    public String getAlias() {
        return alias;
    }
}
