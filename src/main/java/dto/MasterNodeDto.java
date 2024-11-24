package dto;

import java.net.Socket;
import java.util.*;

public class MasterNodeDto extends ServerNodeDto {

    private List<Socket> replicaNodeSocketList;
    private Integer numReplicas;

    public MasterNodeDto(String host, int port) {
        super(host, port);
        replicaNodeSocketList = new ArrayList<>();
        numReplicas = 0;
    }

    public List<Socket> getReplicaNodeSocketList() {
        return replicaNodeSocketList;
    }

    public void setReplicaNodeSocketList(List<Socket> replicaNodeSocketList) {
        this.replicaNodeSocketList = replicaNodeSocketList;
    }

    public Integer getNumReplicas() {
        return numReplicas;
    }

    public void setNumReplicas(Integer numReplicas) {
        this.numReplicas = numReplicas;
    }
}
