package dto;

import java.net.Socket;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;

public class MasterNode extends ServerNode {

    private List<Socket> replicaNodeSocketList;
    public ConcurrentLinkedQueue<PropagateTask> taskQueue;

    public MasterNode(String host, int port) {
        super(host, port);
        replicaNodeSocketList = new ArrayList<>();
        this.taskQueue = new ConcurrentLinkedQueue<>();
    }

    public List<Socket> getReplicaNodeSocketList() {
        return replicaNodeSocketList;
    }

    public void setReplicaNodeSocketList(List<Socket> replicaNodeSocketList) {
        this.replicaNodeSocketList = replicaNodeSocketList;
    }

    public ConcurrentLinkedQueue<PropagateTask> getTaskQueue() {
        return taskQueue;
    }

    public void setTaskQueue(ConcurrentLinkedQueue<PropagateTask> taskQueue) {
        this.taskQueue = taskQueue;
    }
}
