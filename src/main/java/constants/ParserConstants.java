package constants;

public class ParserConstants {
    public static final byte SIMPLE_STRING_PLUS = '+';
    public static final byte SIMPLE_ERROR_MINUS = '-';
    public static final byte INTEGER_COLON = ':';
    public static final byte BULK_STRING_DOLLAR_SIGN = '$';
    public static final byte ARRAY_ASTERISK = '*';
    public static final byte TERMINATOR = -1;
    public static final byte ZERO_TERMINATOR = 0;
    public static final byte CR = '\r';
    public static final byte LF = '\n';
    public static final String MAGIC_NUMBER = "REDIS";
    public static final Integer REDIS_RDB_MAGIC_NUMBER_BYTE_LENGTH = 5;
    public static final Integer REDIS_RDB_VERSION_BYTE_LENGTH = 4;
    public static final Integer REDIS_RDB_VERSION_MAX_VALUE = 100;
    public static final int MASK_TO_8_BIT_INT = 0xff;
    public static final String REDIS_RDB_DB_ID = "db.id";

    // REDIS RDB SECTION INDICATOR
    public static final int REDIS_RDB_OPCODE_MODULE_AUX = 247;
    public static final int REDIS_RDB_OPCODE_IDLE = 248;
    public static final int REDIS_RDB_OPCODE_FREQ = 249;
    public static final int REDIS_RDB_OPCODE_AUX = 250;
    public static final int REDIS_RDB_OPCODE_RESIZE_DB = 251;
    public static final int REDIS_RDB_OPCODE_EXPIRE_TIME_MS = 252;
    public static final int REDIS_RDB_OPCODE_EXPIRE_TIME_SC = 253;
    public static final int REDIS_RDB_OPCODE_SELECT_DB = 254;
    public static final int REDIS_RDB_OPCODE_EOF = 255;

    // REDIS RDB VALUE TYPE
    public static final int REDIS_RDB_TYPE_STRING = 0;
    public static final int REDIS_RDB_TYPE_LIST = 1;
    public static final int REDIS_RDB_TYPE_SET = 2;
    public static final int REDIS_RDB_TYPE_ZSET = 3;
    public static final int REDIS_RDB_TYPE_HASH = 4;
    public static final int REDIS_RDB_TYPE_ZSET_2 = 5;
    public static final int REDIS_RDB_TYPE_MODULE = 6;
    public static final int REDIS_RDB_TYPE_MODULE_2 = 7;
    public static final int REDIS_RDB_TYPE_HASH_ZIPMAP = 9;
    public static final int REDIS_RDB_TYPE_LIST_ZIPLIST = 10;
    public static final int REDIS_RDB_TYPE_SET_INTSET = 11;
    public static final int REDIS_RDB_TYPE_ZSET_ZIPLIST = 12;

    // REDIS RDB ENCODING TYPE
    public static final int REDIS_RDB_ENC_INT8 = 0;
    public static final int REDIS_RDB_ENC_INT16 = 1;
    public static final int REDIS_RDB_ENC_INT32 = 2;
    public static final int REDIS_RDB_ENC_LZF = 3;

}
