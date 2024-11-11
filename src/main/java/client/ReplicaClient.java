package client;

import constants.OutputConstants;
import dto.Master;
import enums.Command;
import service.RESPUtils;
import service.SystemPropHelper;

import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.List;

public class ReplicaClient {
    
    private Socket replica2Master;
    private Master master;
    
    public ReplicaClient(Master master) {
        this.master = master;
        this.replica2Master = null;
    }
    
    public void connect2Master() throws IOException {
        this.replica2Master = new Socket(this.master.getHost(), this.master.getPort());
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

    protected  static String getRESPPsync() {
        List<String> list = List.of(Command.PSYNC.name(), OutputConstants.QUESTION_MARK, OutputConstants.NULL_BULK);
        return RESPUtils.toArray(list);
    }
    
    public void sendRespPING() {
        try {
            OutputStream outputStream = this.replica2Master.getOutputStream();
            outputStream.write(RESPUtils.getRESPPing().getBytes(StandardCharsets.UTF_8));
            outputStream.flush();
            Thread.sleep(200); // temporary solution
            System.out.println("sent PING command to master");
        } catch(IOException | InterruptedException e) {
            throw new RuntimeException("failed to PING master node, ignore handshake");
        }
    }

    public void sendRespListeningPort() {
        try {
            OutputStream outputStream = this.replica2Master.getOutputStream();
            outputStream.write(ReplicaClient.getRESPReplConfListeningPort().getBytes(StandardCharsets.UTF_8));
            outputStream.flush();
            Thread.sleep(200); // temporary solution
            System.out.println("sent REPLCONF listening-port command to master");
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException("failed to send replica's listening port to master node, ignore handshake");
        }
    }

    public void sendRespCapa() {
        try {
            OutputStream outputStream = this.replica2Master.getOutputStream();
            outputStream.write(ReplicaClient.getRESPReplConfCapa().getBytes(StandardCharsets.UTF_8));
            outputStream.flush();
            Thread.sleep(200); // temporary solution
            System.out.println("sent REPLCONF capa psync2 command to master");
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException("failed to send replica's capa psync2 to master node, ignore handshake");
        }
    }

    public void sendRespPsync() {
        try {
            OutputStream outputStream = this.replica2Master.getOutputStream();
            outputStream.write(ReplicaClient.getRESPPsync().getBytes(StandardCharsets.UTF_8));
            outputStream.flush();
            Thread.sleep(200);
            System.out.println("send PSYNC command to master");
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException("failed to send replica's psync to master node, ignore handshake");
        }
    }
}
