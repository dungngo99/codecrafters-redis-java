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
    public static final String DIR_DEFAULT_VALUE = "/tmp/redis-files";
    public static final String DB_FILENAME = "dbfilename";
    public static final String DB_FILENAME_DEFAULT_VALUE = "dump.rdb";
    public static final String KEYS_DELIMITER = "*";
    public static final String SPACE_DELIMITER = " ";
    public static final String REDIS_RDB_VERSION = "redis.rdb.version";
    public static final int DEFAULT_REDIS_MASTER_SERVER_PORT = 6379;
    public static final String REDIS_SERVER_PORT_KEY = "port";
    public static final String REDIS_SERVER_ROLE_TYPE = "role";
    public static final String COLON_DELIMITER = ":";
    public static final String REDIS_SERVER_REPLICA_OF = "replicaof";
    public static final int MASTER_REPLID_LENGTH = 40;
    public static final String MASTER_REPLID = "master_replid";
    public static final int MASTER_REPL_OFFSET_DEFAULT = 0;
    public static final String MASTER_REPL_OFFSET = "master_repl_offset";
    public static final Integer RESP_PING_ARRAY_LENGTH = 1;
    public static final String REPLICA_PSYNC2 = "psync2";
}
