package replication;

import constants.OutputConstants;
import constants.ParserConstants;
import dto.MasterNode;
import dto.RESPResult;
import enums.Command;
import enums.RESPResultType;
import service.RESPParser;
import service.RESPUtils;
import service.SystemPropHelper;

import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Objects;

public class ReplicaClient {

    private static Socket replica2Master;

    public static void handleReplicationHandshake() {
        new Thread(ReplicaClient::listenHandshakeFromMaster).start();
        ReplicaClient.sendHandshake2Master();
    }
    
    public static void connect2Master() {
        try {
            MasterNode master = SystemPropHelper.getServerMaster();
            if (Objects.isNull(master) || Objects.isNull(master.getHost()) || master.getPort() <= 0) {
                throw new RuntimeException("replica node is missing master's host or port");
            }
            replica2Master = new Socket(master.getHost(), master.getPort());
        } catch (Exception e) {
            throw new RuntimeException("failed to connect to master from replica, ignore handshake due to " + e.getMessage());
        }
    }

    protected static String getRESPReplConfListeningPort() {
        String replicaPort = String.valueOf(SystemPropHelper.getServerPortOrDefault());
        List<String> list = List.of(Command.REPLCONF.name(), Command.LISTENING_PORT.getAlias(), replicaPort);
        return RESPUtils.toArray(list);
    }

    protected static String getRESPReplConfCapa() {
        List<String> list = List.of(Command.REPLCONF.name(), Command.CAPA.getAlias(), OutputConstants.REPLICA_PSYNC2);
        return RESPUtils.toArray(list);
    }

    protected static String getRESPPsync() {
        List<String> list = List.of(Command.PSYNC.name(), OutputConstants.QUESTION_MARK, OutputConstants.NULL_BULK);
        return RESPUtils.toArray(list);
    }

    protected static void sendRespPING() {
        try {
            OutputStream outputStream = replica2Master.getOutputStream();
            outputStream.write(RESPUtils.getRESPPing().getBytes(StandardCharsets.UTF_8));
            outputStream.flush();
            Thread.sleep(OutputConstants.THREAD_SLEEP_100_MILLIS); // temporary solution
        } catch(IOException | InterruptedException e) {
            throw new RuntimeException("failed to PING master node, ignore handshake", e);
        }
    }

    protected static void sendRespListeningPort() {
        try {
            OutputStream outputStream = replica2Master.getOutputStream();
            outputStream.write(ReplicaClient.getRESPReplConfListeningPort().getBytes(StandardCharsets.UTF_8));
            outputStream.flush();
            Thread.sleep(OutputConstants.THREAD_SLEEP_100_MILLIS); // temporary solution
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException("failed to send replica's listening port to master node, ignore handshake", e);
        }
    }

    protected static void sendRespCapa() {
        try {
            OutputStream outputStream = replica2Master.getOutputStream();
            outputStream.write(ReplicaClient.getRESPReplConfCapa().getBytes(StandardCharsets.UTF_8));
            outputStream.flush();
            Thread.sleep(OutputConstants.THREAD_SLEEP_100_MILLIS); // temporary solution
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException("failed to send replica's capa psync2 to master node, ignore handshake", e);
        }
    }

    protected static void sendRespPsync() {
        try {
            OutputStream outputStream = replica2Master.getOutputStream();
            outputStream.write(ReplicaClient.getRESPPsync().getBytes(StandardCharsets.UTF_8));
            outputStream.flush();
            Thread.sleep(OutputConstants.THREAD_SLEEP_100_MILLIS); // temporary solution
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException("failed to send replica's psync to master node, ignore handshake", e);
        }
    }

    public static void sendHandshake2Master() {
        sendRespPING();
        sendRespListeningPort();
        sendRespCapa();
        sendRespPsync();
    }

    public static void listenHandshakeFromMaster() {
        try {
            while (!replica2Master.isClosed()) {
                RESPResult result = new RESPParser.Builder()
                        .addClientSocket(replica2Master)
                        .addBufferSize(ParserConstants.RESP_PARSER_BUFFER_SIZE)
                        .build()
                        .process();
                if (!RESPResultType.shouldProcess(result.getType())) {
                    return;
                }
                RESPResultType type = result.getType();
                List<String> list = result.getList();
                if (Objects.equals(type, RESPResultType.STRING)) {
                    handleClientSimpleString(list.getFirst());
                }
                Thread.sleep(Duration.of(100, ChronoUnit.MICROS));
            }
        } catch (IOException | InterruptedException | RuntimeException e) {
            System.out.println("failed to receive answer from master due to " + e.getMessage());
        }
    }

    private static void handleClientSimpleString(String string) {
        if (RESPUtils.isValidHandshakeReplicationSimpleString(string)) {
            System.out.println("received " + string + " from master");
            // proxy to specific handler
            return;
        }
        if (RESPUtils.isValidRESPResponse(string)) {
            System.out.println("recorded " + string.substring(0,3) + " after command from master");
            return;
        }
    }
}
