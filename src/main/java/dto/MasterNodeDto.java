package dto;

import java.net.Socket;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;

public class MasterNodeDto extends ServerNodeDto {

    private List<Socket> replicaNodeSocketList;
    public ConcurrentLinkedQueue<TaskDto> taskQueue;

    public MasterNodeDto(String host, int port) {
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

    public ConcurrentLinkedQueue<TaskDto> getTaskQueue() {
        return taskQueue;
    }

    public void setTaskQueue(ConcurrentLinkedQueue<TaskDto> taskQueue) {
        this.taskQueue = taskQueue;
    }
}
