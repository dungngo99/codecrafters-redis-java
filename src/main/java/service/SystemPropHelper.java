package service;

import constants.OutputConstants;
import dto.Master;
import enums.RoleType;

import java.util.Objects;

public class SystemPropHelper {

    public static void setNewEnvProperty(String key, String value) {
        System.setProperty(key, value);
    }

    public static int getServerPortOrDefault() {
        String ans = System.getProperty(OutputConstants.REDIS_SERVER_PORT_KEY);
        return Objects.nonNull(ans) ? Integer.parseInt(ans) : OutputConstants.DEFAULT_REDIS_SERVER_PORT;
    }

    public static String getSetServerRoleOrDefault() {
        String role = System.getProperty(OutputConstants.REDIS_SERVER_ROLE_TYPE);
        if (Objects.nonNull(role)) {
            return role;
        }
        String replicaOf = System.getProperty(OutputConstants.REDIS_SERVER_REPLICA_OF);
        if (Objects.nonNull(replicaOf) && !replicaOf.isEmpty()) {
            setNewEnvProperty(OutputConstants.REDIS_SERVER_ROLE_TYPE, RoleType.SLAVE.name().toLowerCase());
        } else {
            setNewEnvProperty(OutputConstants.REDIS_SERVER_ROLE_TYPE, RoleType.MASTER.name().toLowerCase());
        }
        return System.getProperty(OutputConstants.REDIS_SERVER_ROLE_TYPE);
    }

    public static Master getServerMaster() {
        String masterVal = System.getProperty(OutputConstants.REDIS_SERVER_REPLICA_OF);
        if (Objects.isNull(masterVal) || masterVal.isEmpty()) {
            return null;
        }
        String[] masterValues = masterVal.split(OutputConstants.SPACE_DELIMITER);
        if (masterValues.length < 2) {
            return null;
        }
        return new Master(masterValues[0], Integer.parseInt(masterValues[1]));
    }

    public static String getSetMasterReplId() {
        String value = System.getProperty(OutputConstants.MASTER_REPLID);
        if (Objects.nonNull(value)) {
            return value;
        }
        value = RandomUtils.randomLowerAlphaNumericByLength(OutputConstants.MASTER_REPLID_LENGTH);
        setNewEnvProperty(OutputConstants.MASTER_REPLID, value);
        return value;
    }
}
