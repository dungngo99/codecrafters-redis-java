package domain;

import java.net.Socket;

public class ParserDto<T> {

    private Socket socket;
    private T value;

    public ParserDto(Socket socket, T value) {
        this.socket = socket;
        this.value = value;
    }
    public Socket getSocket() {
        return socket;
    }

    public void setSocket(Socket socket) {
        this.socket = socket;
    }

    public T getValue() {
        return value;
    }

    public void setValue(T value) {
        this.value = value;
    }
}
