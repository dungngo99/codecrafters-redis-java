package constants;

public class OutputConstants {
    public static final String CRLF = "\r\n";
    public static final String PING = "PING";
    public static final String PONG = "PONG";
    public static final String OK = "OK";
    public static final String NULL_BULK = "-1";
    public static final String DOLLAR_SIZE = "$";
    public static final String ASTERISK = "*";
    public static final String PLUS = "+";
    public static final String EMPTY = "";
    public static final String DIR = "dir";
    public static final String DB_FILENAME = "dbfilename";
    public static final String KEYS_DELIMITER = "*";
    public static final String SPACE_DELIMITER = " ";
    public static final String REDIS_RDB_VERSION = "redis.rdb.version";
    public static final String DEFAULT_REDSI_SERVER_HOST = "localhost";
    public static final int DEFAULT_REDIS_MASTER_SERVER_PORT = 6379;
    public static final String REDIS_SERVER_HOST_KEY = "host";
    public static final String REDIS_SERVER_PORT_KEY = "port";
    public static final String REDIS_SERVER_ROLE_TYPE = "role";
    public static final String COLON_DELIMITER = ":";
    public static final String DASH_DELIMITER = "-";
    public static final String REDIS_SERVER_REPLICA_OF = "replicaof";
    public static final int MASTER_REPLID_LENGTH = 40;
    public static final String MASTER_REPLID = "master_replid";
    public static final String MASTER_NODE_ID = "master_node_id";
    public static final int MASTER_REPL_OFFSET_DEFAULT = 0;
    public static final int CLIENT_REPL_OFFSET_DEFAULT = 0;
    public static final String MASTER_REPL_OFFSET = "master_repl_offset";
    public static final Integer RESP_PING_ARRAY_LENGTH = 1;
    public static final String REPLICA_PSYNC2 = "psync2";
    public static final String REPLICA_FULL_RESYNC = "FULLRESYNC";
    public static final String QUESTION_MARK = "?";
    public static final Integer THREAD_SLEEP_100_MICROS = 100;
    public static final Integer THREAD_SLEEP_100000_MICROS = 100000;
    public static final String SERVER_NODE_ID_FORMAT = "%s::%s";
    public static final String EMPTY_RDB_FILE_CONTENT_HEX = "524544495330303131fa0972656469732d76657205372e322e30fa0a72656469732d62697473c040fa056374696d65c26d08bc65fa08757365642d6d656dc2b0c41000fa08616f662d62617365c000fff06e3bfec0ff5aa2";
    public static final Integer DEFAULT_NO_REPLICA_CONNECTED = 0;
    public static final Integer DEFAULT_INVALID_TASK_DTO_ID = -1;
    public static final String NONE_COMMAND_TYPE_FOR_MISSING_KEY = "none";
    public static final String STREAM_EVENT_ID_SMALLER_OR_EQUAL_THAN_TOP_EVENT_ID_ERROR = "ERR The ID specified in XADD is equal or smaller than the target stream top item";
    public static final String STREAM_EVENT_ID_SMALLER_OR_EQUAL_THAN_0_ERROR = "ERR The ID specified in XADD must be greater than 0-0";
}
