package domain;

import constants.OutputConstants;

import java.net.Socket;

public class ParserDto<T> {

    private Socket socket;
    private T value;
    private Boolean isNoProcessCommandHandler;
    private String userName = OutputConstants.WHOAMI_USER_NAME_DEFAULT;

    public ParserDto(Socket socket, T value) {
        this(socket, value, null);
    }

    public ParserDto(Socket socket, T value, Boolean isNoProcessCommandHandler) {
        this.socket = socket;
        this.value = value;
        this.isNoProcessCommandHandler = isNoProcessCommandHandler;
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

    public Boolean getNoProcessCommandHandler() {
        return isNoProcessCommandHandler;
    }

    public void setNoProcessCommandHandler(Boolean noProcessCommandHandler) {
        isNoProcessCommandHandler = noProcessCommandHandler;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }
}
