package dto;

import java.util.HashMap;
import java.util.Map;

public class MasterNode extends ServerNode {

    private Map<String, ReplicaNode> replicaNodeMap;

    public MasterNode() {
        this.replicaNodeMap = new HashMap<>();
    }

    public MasterNode(String host, int port) {
        super(host, port);
        this.replicaNodeMap = new HashMap<>();
    }

    public Map<String, ReplicaNode> getReplicaNodeMap() {
        return replicaNodeMap;
    }

    public void setReplicaNodeMap(Map<String, ReplicaNode> replicaNodeMap) {
        this.replicaNodeMap = replicaNodeMap;
    }
}
