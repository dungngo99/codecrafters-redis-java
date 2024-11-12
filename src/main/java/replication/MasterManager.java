package replication;

import constants.OutputConstants;
import dto.MasterNode;
import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.binary.Hex;
import service.RESPUtils;

import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

public class MasterManager {

    private MasterNode masterNode;

    public void transferEmptyRDBFile(String ans, Socket clientSocket) throws IOException, InterruptedException {
        if (ans == null || ans.isBlank() || !ans.contains(OutputConstants.REPLICA_FULL_RESYNC)) {
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
        outputStream.write(emptyRDBPrefix.getBytes(StandardCharsets.UTF_8));
        outputStream.write(bytes);
        outputStream.flush();
    }

    public MasterNode getMasterNode() {
        return masterNode;
    }

    public void setMasterNode(MasterNode masterNode) {
        this.masterNode = masterNode;
    }
}
