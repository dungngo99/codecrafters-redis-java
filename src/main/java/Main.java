import dto.Cache;
import enums.Command;
import handler.CommandHandler;
import handler.impl.EchoHandler;
import handler.impl.GetHandler;
import handler.impl.PingHandler;
import handler.impl.SetHandler;
import service.LocalMap;
import service.Parser;
import stream.RedisInputStream;

import java.io.IOException;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class Main {

    private static final int PORT = 6379;
    private ServerSocket serverSocket;
    private int port;

    public Main(int port) {
        this.port = port;
    }

    private void registerCommandHandler() {
        new EchoHandler().register();
        new PingHandler().register();
        new SetHandler().register();
        new GetHandler().register();
    }

    private void initCleanLocalMap() {
        new Thread(this::removeExpiredKeyFromLocalMap).start();
    }

    private void removeExpiredKeyFromLocalMap() {
        try {
            while (this.serverSocket != null && !this.serverSocket.isClosed()) {
                Long currentTime = System.currentTimeMillis();
                List<String> cacheKey2Remove = getCacheKey2Remove(currentTime);
                for (String key: cacheKey2Remove) {
                    LocalMap.LOCAL_MAP.remove(key);
                }
                Thread.sleep(10);
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private static List<String> getCacheKey2Remove(Long currentTime) {
        List<String> cacheKey2Remove = new ArrayList<>();
        for(Map.Entry<String, Cache> cacheEntry: LocalMap.LOCAL_MAP.entrySet()) {
            String key = cacheEntry.getKey();
            Cache cache = cacheEntry.getValue();
            Long px = cache.getPx();
            Long startTime = cache.getStartTime();
            if (px == null || startTime == null) {
                continue;
            }
            if (px < currentTime -startTime) {
                cacheKey2Remove.add(key);
            }
        }
        return cacheKey2Remove;
    }

    private void startServerSocket() {
        // Uncomment this block to pass the first stage
        try {
            this.serverSocket = new ServerSocket(this.port);
            // Since the tester restarts your program quite often, setting SO_REUSEADDR
            // ensures that we don't run into 'Address already in use' errors
            this.serverSocket.setReuseAddress(true);
            // init job to clean local map
            initCleanLocalMap();
            // Wait for connection from client.
            while (!this.serverSocket.isClosed()) {
                Socket clientSocket = serverSocket.accept(); // blocking
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
            while (!clientSocket.isClosed()) {
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

  public static void main(String[] args) {
        // You can use print statements as follows for debugging, they'll be visible when running tests.
        System.out.println("Logs from your program will appear here! with " + Arrays.toString(args));
        Main main = new Main(PORT);
        main.registerCommandHandler();
        main.startServerSocket();
  }
}
