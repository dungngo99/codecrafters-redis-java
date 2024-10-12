import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

public class Main {
  public static void main(String[] args){
    // You can use print statements as follows for debugging, they'll be visible when running tests.
    System.out.println("Logs from your program will appear here!");

      // Uncomment this block to pass the first stage
        ServerSocket serverSocket;
        Socket clientSocket = null;
        int port = 6379;
        try {
          serverSocket = new ServerSocket(port);
          // Since the tester restarts your program quite often, setting SO_REUSEADDR
          // ensures that we don't run into 'Address already in use' errors
          serverSocket.setReuseAddress(true);
          // Wait for connection from client.
          while (!serverSocket.isClosed()) {
              clientSocket = serverSocket.accept();
              // handle multiple commands from redis client
              while (true) {
                  BufferedReader reader = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                  String ans = getCommand(reader);
                  if (ans.isBlank()) {
                      break;
                  }
                  clientSocket.getOutputStream().write("+PONG\r\n".getBytes(StandardCharsets.UTF_8));
              }
          }
        } catch (IOException e) {
          System.out.println("IOException: " + e.getMessage());
        } finally {
          try {
            if (clientSocket != null) {
              clientSocket.close();
            }
          } catch (IOException e) {
            System.out.println("IOException: " + e.getMessage());
          }
        }
  }

  private static String getCommand(BufferedReader reader) throws IOException {
      StringBuilder builder = new StringBuilder();
      while (reader.ready()) {
          builder
                  .append(reader.readLine())
                  .append("\r\n");
      }
      return builder.toString();
  }
}
