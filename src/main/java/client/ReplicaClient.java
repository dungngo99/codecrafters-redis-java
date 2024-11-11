package client;

import constants.OutputConstants;
import dto.Master;
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

public class ReplicaClient {
    
    private Socket replica2Master;
    private Master master;
    
    public ReplicaClient(Master master) {
        this.master = master;
        this.replica2Master = null;
    }
    
    public void connect2Master() {
        try {
            this.replica2Master = new Socket(this.master.getHost(), this.master.getPort());
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
        try {
            OutputStream outputStream = this.replica2Master.getOutputStream();
            outputStream.write(RESPUtils.getRESPPing().getBytes(StandardCharsets.UTF_8));
            outputStream.flush();
            Thread.sleep(OutputConstants.THREAD_SLEEP_100_MILLIS); // temporary solution
            System.out.println("sent PING command to master");
        } catch(IOException | InterruptedException e) {
            throw new RuntimeException("failed to PING master node, ignore handshake", e);
        }
    }

    protected void sendRespListeningPort() {
        try {
            OutputStream outputStream = this.replica2Master.getOutputStream();
            outputStream.write(ReplicaClient.getRESPReplConfListeningPort().getBytes(StandardCharsets.UTF_8));
            outputStream.flush();
            Thread.sleep(OutputConstants.THREAD_SLEEP_100_MILLIS); // temporary solution
            System.out.println("sent REPLCONF listening-port command to master");
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException("failed to send replica's listening port to master node, ignore handshake", e);
        }
    }

    protected void sendRespCapa() {
        try {
            OutputStream outputStream = this.replica2Master.getOutputStream();
            outputStream.write(ReplicaClient.getRESPReplConfCapa().getBytes(StandardCharsets.UTF_8));
            outputStream.flush();
            Thread.sleep(OutputConstants.THREAD_SLEEP_100_MILLIS); // temporary solution
            System.out.println("sent REPLCONF capa psync2 command to master");
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException("failed to send replica's capa psync2 to master node, ignore handshake", e);
        }
    }

    protected void sendRespPsync() {
        try {
            OutputStream outputStream = this.replica2Master.getOutputStream();
            outputStream.write(ReplicaClient.getRESPPsync().getBytes(StandardCharsets.UTF_8));
            outputStream.flush();
            Thread.sleep(OutputConstants.THREAD_SLEEP_100_MILLIS); // temporary solution
            System.out.println("send PSYNC command to master");
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
        try {
            while (!this.replica2Master.isClosed()) {
                RedisInputStream redisInputStream = new RedisInputStream(this.replica2Master.getInputStream(), 1000);
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
