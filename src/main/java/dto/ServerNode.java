package dto;

import java.net.ServerSocket;

public class ServerNode {

    private String id;
    private ServerSocket serverSocket;
    private String host;
    private int port;
    private String role;

    public ServerNode() {

    }

    public ServerNode(int port) {
        this.port = port;
    }

    public ServerNode(String host, int port) {
        this.host = host;
        this.port = port;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public ServerSocket getServerSocket() {
        return serverSocket;
    }

    public void setServerSocket(ServerSocket serverSocket) {
        this.serverSocket = serverSocket;
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }
}
