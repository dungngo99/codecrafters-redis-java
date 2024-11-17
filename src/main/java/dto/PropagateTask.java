package dto;

import java.net.Socket;

public class PropagateTask {
    private Socket socket;
    private String command;

    public Socket getSocket() {
        return socket;
    }

    public void setSocket(Socket socket) {
        this.socket = socket;
    }

    public String getCommand() {
        return command;
    }

    public void setCommand(String command) {
        this.command = command;
    }

    @Override
    public String toString() {
        return "PropagateTask{" +
                "socket=" + socket +
                '}';
    }
}
