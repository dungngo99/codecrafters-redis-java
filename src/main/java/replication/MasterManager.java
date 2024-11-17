package replication;

import constants.OutputConstants;
import dto.MasterNode;
import dto.PropagateTask;
import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.binary.Hex;
import service.RESPUtils;
import service.SystemPropHelper;

import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentLinkedQueue;

public class MasterManager {

    private static final Map<String, MasterNode> MASTER_NODE_MAP = new HashMap<>();

    public static void registerReplicaNodeConnection(Socket socket) {
        if (socket == null) {
            return;
        }
        String masterNodeId = SystemPropHelper.getSetMasterNodeId();
        MasterNode masterNode = MASTER_NODE_MAP.get(masterNodeId);
        if (masterNode == null) {
            return;
        }
        masterNode.getReplicaNodeSocketList().add(socket);
    }

    public static void registerMasterManager(String masterNodeId, String host, int port) {
        MASTER_NODE_MAP.put(masterNodeId, new MasterNode(host, port));
    }

    public static void registerPropagateTaskHandler(String masterNodeId) {
        MasterNode masterNode = MASTER_NODE_MAP.get(masterNodeId);
        if (masterNode == null) {
            return;
        }
        new Thread(() -> MasterManager.registerPropagateTaskHandler0(masterNode)).start();
    }

    private static void registerPropagateTaskHandler0(MasterNode masterNode) {
        try {
            while (true) {
                ConcurrentLinkedQueue<PropagateTask> taskQueue = masterNode.getTaskQueue();
                if (!taskQueue.isEmpty()) {
                    PropagateTask task = taskQueue.poll();
                    System.out.println("begin to propagate task=" + task);
                    Socket socket = task.getSocket();
                    String command = task.getCommand();
                    OutputStream outputStream = socket.getOutputStream();
                    outputStream.write(command.getBytes(StandardCharsets.UTF_8));
                    outputStream.flush();
                }
                Thread.sleep(Duration.of(100, ChronoUnit.MICROS));
            }
        } catch(IOException | InterruptedException e) {
            System.out.println("failed to run propagate task handler due to " + e.getMessage());
        }
    }

    public static void transferEmptyRDBFile(Socket clientSocket) throws IOException, InterruptedException {
        if (clientSocket == null) {
            return;
        }
        byte[] bytes;
        try {
            bytes = Hex.decodeHex(OutputConstants.EMPTY_RDB_FILE_CONTENT_HEX);
        } catch(DecoderException e) {
            System.out.println("failed to transfer empty rdb file, ignore transfering");
            return;
        }
        Thread.sleep(OutputConstants.THREAD_SLEEP_100_MILLIS);
        String emptyRDBPrefix = RESPUtils.toByteStreamWithCRLF(bytes);
        OutputStream outputStream = clientSocket.getOutputStream();
        byte[] prefixBytes = emptyRDBPrefix.getBytes(StandardCharsets.UTF_8);
        byte[] finalBytes = RESPUtils.combine2Bytes(prefixBytes, bytes);
        outputStream.write(finalBytes);
        outputStream.flush();
    }

    /**
     * Solution 1: command will be put in concurrent queue for later processing
     * @param command RESP command
     */
    public static void registerCommand(String command) {
        String masterNodeId = SystemPropHelper.getSetMasterNodeId();
        MasterNode masterNode = MASTER_NODE_MAP.get(masterNodeId);
        if (masterNode == null) {
            return;
        }
        for (Socket socket: masterNode.getReplicaNodeSocketList()) {
            if (Objects.isNull(socket)) {
                continue;
            }
            PropagateTask task = new PropagateTask();
            task.setSocket(socket);
            task.setCommand(command);
            masterNode.getTaskQueue().add(task);
        }
    }

    /**
     * Solution 2: command will be processed immediately
     * @param command
     */
    public static void propagateCommand(String command) {
        String masterNodeId = SystemPropHelper.getSetMasterNodeId();
        MasterNode masterNode = MASTER_NODE_MAP.get(masterNodeId);
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
