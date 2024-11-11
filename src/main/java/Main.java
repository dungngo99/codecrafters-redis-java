import client.ReplicaClient;
import constants.OutputConstants;
import dto.Cache;
import dto.Master;
import enums.RoleType;
import handler.impl.*;
import service.RedisLocalMap;
import service.RESPParser;
import service.RDBLoaderUtils;
import service.SystemPropHelper;
import stream.RedisInputStream;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.*;

public class Main {
    private ServerSocket serverSocket;
    private int port;
    private String role;
    private Master master;
    private ReplicaClient client;

    public Main() {}

    public Main(int port) {
        this.port = port;
    }

    private void registerNewEnvVars(String[] args) {
        int length = args.length;
        for(int i=0; i<length; i+=2) {
            if (i+1>=length) {
                break;
            }
            String key = args[i].substring(2).toLowerCase();
            String value = args[i+1];
            SystemPropHelper.setNewEnvProperty(key, value);
        }
    }

    private void registerCommandHandler() {
        new EchoHandler().register();
        new PingHandler().register();
        new SetHandler().register();
        new GetHandler().register();
        new ConfigHandler().register();
        new SaveHandler().register();
        new KeysHandler().register();
        new InfoHandler().register();
        new ReplConfigHandler().register();
        new PsyncHandler().register();
    }

    private void registerRDB() {
        RDBLoaderUtils.load();
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
                    RedisLocalMap.LOCAL_MAP.remove(key);
                }
                Thread.sleep(10);
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private static List<String> getCacheKey2Remove(Long currentTime) {
        List<String> cacheKey2Remove = new ArrayList<>();
        for(Map.Entry<String, Cache> cacheEntry: RedisLocalMap.LOCAL_MAP.entrySet()) {
            String key = cacheEntry.getKey();
            Cache cache = cacheEntry.getValue();
            Long expireTime = cache.getExpireTime();
            if (expireTime == null) {
                continue;
            }
            if (expireTime.equals(currentTime) || expireTime < currentTime) {
                cacheKey2Remove.add(key);
            }
        }
        return cacheKey2Remove;
    }

    private void startServerSocket() {
        // Uncomment this block to pass the first stage
        try {
            // start redis-server
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

    private void fillRedisServerInfo() {
        this.port = this.port != 0 ? this.port : SystemPropHelper.getServerPortOrDefault();
        this.role = SystemPropHelper.getSetServerRoleOrDefault();
        this.master = SystemPropHelper.getServerMaster();
        this.client = new ReplicaClient(this.master);
    }

    private void preCheck() {
        System.out.println("Pre-check if redis-server can be started");
        if (!RoleType.MASTER.name().equalsIgnoreCase(this.role) && Objects.equals(this.port, OutputConstants.DEFAULT_REDIS_MASTER_SERVER_PORT)) {
            throw new RuntimeException("not allow non-master node to use default master port=6379");
        }
    }

    private void handshake2Master() {
        if (RoleType.MASTER.name().equalsIgnoreCase(this.role)) {
            return;
        }
        if (Objects.isNull(this.master) || Objects.isNull(this.master.getHost()) || this.master.getPort() <= 0) {
            throw new RuntimeException("replica node is missing master's host or port");
        }
        try {
            this.client.connect2Master();
            this.client.sendRespPING();
            this.client.sendRespListeningPort();
            this.client.sendRespCapa();
            this.client.sendRespPsync();
        } catch(IOException e) {
            throw new RuntimeException("failed to PING master node, ignore handshake");
        }
    }

    private static void handleClientConnection(Socket clientSocket) {
        try {
            // handle multiple commands from redis client
            while (!clientSocket.isClosed()) {
                RedisInputStream redisInputStream = new RedisInputStream(clientSocket.getInputStream(), 1000);
                String ans = RESPParser.process(redisInputStream);
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
        Main main = new Main();
        main.registerNewEnvVars(args);
        main.registerCommandHandler();
        main.registerRDB();
        main.fillRedisServerInfo();
        main.preCheck();
        main.handshake2Master();
        main.startServerSocket();
  }
}
