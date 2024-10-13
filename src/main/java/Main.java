import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.StringJoiner;

public class Main {
  public static void main(String[] args){
    // You can use print statements as follows for debugging, they'll be visible when running tests.
    System.out.println("Logs from your program will appear here! with " + Arrays.toString(args));

      // Uncomment this block to pass the first stage
        ServerSocket serverSocket = null;
        int port = 6379;
        try {
          serverSocket = new ServerSocket(port);
          // Since the tester restarts your program quite often, setting SO_REUSEADDR
          // ensures that we don't run into 'Address already in use' errors
          serverSocket.setReuseAddress(true);
          // Wait for connection from client.
          while (!serverSocket.isClosed()) {
              Socket clientSocket = serverSocket.accept();
              new Thread(() -> handleClientConnection(clientSocket)).start();
          }
        } catch (IOException e) {
          System.out.println("IOException: " + e.getMessage());
        } finally {
          try {
            if (serverSocket != null) {
                serverSocket.close();
            }
          } catch (IOException e) {
            System.out.println("IOException: " + e.getMessage());
          }
        }
  }

    private static void handleClientConnection(Socket clientSocket) {
      try {
          // handle multiple commands from redis client
          while (true) {
              RedisInputStream redisInputStream = new RedisInputStream(clientSocket.getInputStream(), 1000);
              String ans = Parser.process(redisInputStream);
              OutputStream outputStream = clientSocket.getOutputStream();
              try {
                  if (!ans.isBlank()) {
                      outputStream.write(ans.getBytes(StandardCharsets.UTF_8));
                      outputStream.flush();
                  }
              } catch (IOException e) {
                  break;
              }
              Thread.sleep(Duration.of(100, ChronoUnit.MICROS));
          }
      } catch (IOException | InterruptedException e) {
          e.printStackTrace();
          try {
              if (!clientSocket.isClosed()) {
                  clientSocket.close();
              }
          } catch (IOException e2) {
              e2.printStackTrace();
          }
      }
    }

  private static String getCommand(BufferedReader reader) throws IOException {
      StringJoiner builder = new StringJoiner(",");
      while (reader.ready()) {
          builder.add(reader.readLine());
      }
      return builder.toString();
  }
}
