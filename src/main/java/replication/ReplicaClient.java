package replication;

import constants.OutputConstants;
import dto.MasterNode;
import dto.ReplicaNode;
import enums.Command;
import service.RESPParser;
import service.RESPUtils;
import service.SystemPropHelper;
import stream.RedisInputStream;

import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Objects;

public class ReplicaClient {

    private ReplicaNode replicaNode;
    
    public ReplicaClient(MasterNode master) {
        this.replicaNode = new ReplicaNode(master);
    }

    public void handleReplicationHandshake() {
        MasterNode master = this.replicaNode.getMaster();
        if (Objects.isNull(master) || Objects.isNull(master.getHost()) || master.getPort() <= 0) {
            throw new RuntimeException("replica node is missing master's host or port");
        }
        this.connect2Master();
        new Thread(this::listenHandshakeFromMaster).start();
        this.sendHandshake2Master();
    }
    
    public void connect2Master() {
        try {
            MasterNode master = replicaNode.getMaster();
            Socket replica2Master = new Socket(master.getHost(), master.getPort());
            replicaNode.setReplica2Master(replica2Master);
        } catch (IOException e) {
            throw new RuntimeException("failed to connect to master from replica, ignore handshake");
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

    protected void sendRespPING() {
        Socket replica2Master = this.replicaNode.getReplica2Master();
        try {
            OutputStream outputStream = replica2Master.getOutputStream();
            outputStream.write(RESPUtils.getRESPPing().getBytes(StandardCharsets.UTF_8));
            outputStream.flush();
            Thread.sleep(OutputConstants.THREAD_SLEEP_100_MILLIS); // temporary solution
        } catch(IOException | InterruptedException e) {
            throw new RuntimeException("failed to PING master node, ignore handshake", e);
        }
    }

    protected void sendRespListeningPort() {
        Socket replica2Master = this.replicaNode.getReplica2Master();
        try {
            OutputStream outputStream = replica2Master.getOutputStream();
            outputStream.write(ReplicaClient.getRESPReplConfListeningPort().getBytes(StandardCharsets.UTF_8));
            outputStream.flush();
            Thread.sleep(OutputConstants.THREAD_SLEEP_100_MILLIS); // temporary solution
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException("failed to send replica's listening port to master node, ignore handshake", e);
        }
    }

    protected void sendRespCapa() {
        Socket replica2Master = this.replicaNode.getReplica2Master();
        try {
            OutputStream outputStream = replica2Master.getOutputStream();
            outputStream.write(ReplicaClient.getRESPReplConfCapa().getBytes(StandardCharsets.UTF_8));
            outputStream.flush();
            Thread.sleep(OutputConstants.THREAD_SLEEP_100_MILLIS); // temporary solution
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException("failed to send replica's capa psync2 to master node, ignore handshake", e);
        }
    }

    protected void sendRespPsync() {
        Socket replica2Master = this.replicaNode.getReplica2Master();
        try {
            OutputStream outputStream = replica2Master.getOutputStream();
            outputStream.write(ReplicaClient.getRESPPsync().getBytes(StandardCharsets.UTF_8));
            outputStream.flush();
            Thread.sleep(OutputConstants.THREAD_SLEEP_100_MILLIS); // temporary solution
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException("failed to send replica's psync to master node, ignore handshake", e);
        }
    }

    public void sendHandshake2Master() {
        sendRespPING();
        sendRespListeningPort();
        sendRespCapa();
        sendRespPsync();
    }

    public void listenHandshakeFromMaster() {
        Socket replica2Master = this.replicaNode.getReplica2Master();
        try {
            while (!replica2Master.isClosed()) {
                RedisInputStream redisInputStream = new RedisInputStream(replica2Master.getInputStream(), 1000);
                String ans = RESPParser.process(redisInputStream);
                if (ans != null && !ans.isBlank()) {
                    System.out.println("received " + ans + " from master");
                }
                Thread.sleep(Duration.of(100, ChronoUnit.MICROS));
            }
        } catch (IOException | InterruptedException | RuntimeException e) {
            System.out.println("failed to receive answer from master due to " + e.getMessage());
        }
    }
}
