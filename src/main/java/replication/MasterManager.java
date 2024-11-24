package replication;

import constants.OutputConstants;
import dto.MasterNodeDto;
import dto.TaskDto;
import enums.JobType;
import handler.job.impl.PropagateHandler;
import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.binary.Hex;
import service.RESPUtils;
import service.ServerUtils;
import service.SystemPropHelper;

import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class MasterManager {

    private static final Map<String, MasterNodeDto> MASTER_NODE_MAP = new HashMap<>();

    public static void registerReplicaNodeConnection(Socket socket) {
        if (socket == null) {
            throw new RuntimeException("invalid param");
        }
        String masterNodeId = SystemPropHelper.getSetMasterNodeId();
        MasterNodeDto masterNode = MASTER_NODE_MAP.get(masterNodeId);
        if (masterNode == null) {
            throw new RuntimeException("master node not found (non-master node should not call this method)");
        }
        masterNode.getReplicaNodeSocketList().add(socket);
    }

    public static void registerMasterManager(String masterNodeId, String host, int port) {
        MASTER_NODE_MAP.put(masterNodeId, new MasterNodeDto(host, port));
    }

    public static byte[] getTransferEmptyRDBFile() {
        byte[] bytes;
        try {
            bytes = Hex.decodeHex(OutputConstants.EMPTY_RDB_FILE_CONTENT_HEX);
        } catch(DecoderException e) {
            System.out.println("failed to transfer empty rdb file, ignore transferring");
            return null;
        }
        String emptyRDBPrefix = RESPUtils.toByteStreamWithCRLF(bytes);
        byte[] prefixBytes = emptyRDBPrefix.getBytes(StandardCharsets.UTF_8);
        return RESPUtils.combine2Bytes(prefixBytes, bytes);
    }

    public static String getRequestACKFromReplica() {
        return RESPUtils.requestRESPReplConfAck();
    }

    public static void incrementConnectedReplica() {
        String masterNodeId = SystemPropHelper.getSetMasterNodeId();
        MasterNodeDto masterNode = MASTER_NODE_MAP.get(masterNodeId);
        if (masterNode == null) {
            throw new RuntimeException("master node not found (non-master node should not call this method)");
        }
        masterNode.setNumReplicas(masterNode.getNumReplicas()+1);
        MASTER_NODE_MAP.put(masterNodeId, masterNode);
    }

    public static int getNumConnectedReplica() {
        String masterNodeId = SystemPropHelper.getSetMasterNodeId();
        MasterNodeDto masterNode = MASTER_NODE_MAP.get(masterNodeId);
        if (masterNode == null) {
            throw new RuntimeException("master node not found (non-master node should not call this method)");
        }
        return masterNode.getNumReplicas();
    }

    /**
     * Solution 1: command will be put in concurrent queue for later processing
     * @param command RESP command
     */
    public static void registerCommand(String command) {
        String masterNodeId = SystemPropHelper.getSetMasterNodeId();
        MasterNodeDto masterNode = MASTER_NODE_MAP.get(masterNodeId);
        if (masterNode == null) {
            return;
        }
        for (Socket socket: masterNode.getReplicaNodeSocketList()) {
            if (Objects.isNull(socket)) {
                continue;
            }
            String jobId = ServerUtils.formatIdFromSocket(socket);
            TaskDto taskDto = new TaskDto.Builder()
                    .addSocket(socket)
                    .addCommandStr(command)
                    .addJobType(JobType.PROPAGATE)
                    .addFreq(OutputConstants.THREAD_SLEEP_100_MICROS)
                    .addInputByteRead(0)
                    .build(); // n/a
            PropagateHandler.registerTask(jobId, taskDto);
        }
    }

    /**
     * Solution 2: command will be processed immediately
     * @param command RESP command
     */
    public static void propagateCommand(String command) {
        String masterNodeId = SystemPropHelper.getSetMasterNodeId();
        MasterNodeDto masterNode = MASTER_NODE_MAP.get(masterNodeId);
        if (masterNode == null) {
            return;
        }
        for (Socket socket: masterNode.getReplicaNodeSocketList()) {
            if (socket == null) {
                continue;
            }
            try {
                OutputStream outputStream = socket.getOutputStream();
                outputStream.write(command.getBytes(StandardCharsets.UTF_8));
                outputStream.flush();
            } catch (IOException e) {
                System.out.println("failed to propagate command from master to replica due to " + e.getMessage());
            }
        }
    }
}
