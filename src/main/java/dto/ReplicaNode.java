package dto;

import java.net.Socket;

public class ReplicaNode extends ServerNode {

    private MasterNode master;
    private Socket replica2Master;

    public ReplicaNode(int port) {
        super(port);
    }

    public ReplicaNode(String host, int port) {
        super(host, port);
    }

    public ReplicaNode(MasterNode master) {
        this.master = master;
    }

    public MasterNode getMaster() {
        return master;
    }

    public void setMaster(MasterNode master) {
        this.master = master;
    }

    public Socket getReplica2Master() {
        return replica2Master;
    }

    public void setReplica2Master(Socket replica2Master) {
        this.replica2Master = replica2Master;
    }
}
