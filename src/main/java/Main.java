import constants.ParserConstants;
import dto.ServerNode;
import replication.MasterManager;
import replication.ReplicaClient;
import constants.OutputConstants;
import dto.Cache;
import enums.RoleType;
import handler.impl.*;
import service.*;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.*;

public class Main {
    private final ServerNode serverNode;

    public Main() {
        this.serverNode = new ServerNode();
    }

    public Main(int port) {
        this.serverNode = new ServerNode(port);
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
            ServerSocket serverSocket = this.serverNode.getServerSocket();
            while (serverSocket != null && !serverSocket.isClosed()) {
                Long currentTime = System.currentTimeMillis();
                List<String> cacheKey2Remove = getCacheKey2Remove(currentTime);
                for (String key: cacheKey2Remove) {
                    RedisLocalMap.LOCAL_MAP.remove(key);
                }
                Thread.sleep(Duration.of(100, ChronoUnit.MICROS));
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private List<String> getCacheKey2Remove(Long currentTime) {
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
            ServerSocket serverSocket = new ServerSocket(this.serverNode.getPort());
            this.serverNode.setServerSocket(serverSocket);
            // Since the tester restarts your program quite often, setting SO_REUSEADDR
            // ensures that we don't run into 'Address already in use' errors
            serverSocket.setReuseAddress(true);
            // init job to clean local map
            initCleanLocalMap();
            // Wait for connection from client.
            while (!serverSocket.isClosed()) {
                Socket clientSocket = serverSocket.accept(); // blocking
                new Thread(() -> handleClientConnection(clientSocket)).start();
            }
        } catch (IOException e) {
            System.out.println("IOException: " + e.getMessage());
        } finally {
            try {
                ServerSocket serverSocket = this.serverNode.getServerSocket();
                if (serverSocket != null) {
                    serverSocket.close();
                }
            } catch (IOException e) {
                System.out.println("IOException: " + e.getMessage());
            }
        }
    }

    private void fillRedisServerInfo() {
        String serverNodeHost = SystemPropHelper.getServerHostOrDefault();
        this.serverNode.setHost(serverNodeHost);

        int serverNodePort = this.serverNode.getPort();
        int newServerNodePort = serverNodePort != 0 ? serverNodePort : SystemPropHelper.getServerPortOrDefault();
        this.serverNode.setPort(newServerNodePort);

        String role = SystemPropHelper.getSetServerRoleOrDefault();
        this.serverNode.setRole(role);

        String id = SystemPropHelper.getSetMasterNodeId();
        this.serverNode.setId(id);
    }

    private void preCheck() {
        System.out.println("Pre-check if redis-server can be started");
        String role = this.serverNode.getRole();
        int port = this.serverNode.getPort();
        if (!RoleType.MASTER.name().equalsIgnoreCase(role) && Objects.equals(port, OutputConstants.DEFAULT_REDIS_MASTER_SERVER_PORT)) {
            throw new RuntimeException("not allow non-master node to use default master port=6379");
        }
    }

    private void handleReplicationHandshake() {
        String role = this.serverNode.getRole();
        if (RoleType.MASTER.name().equalsIgnoreCase(role)) {
            MasterManager.registerMasterManager(this.serverNode.getId(), this.serverNode.getHost(), this.serverNode.getPort());
            MasterManager.registerPropagateTaskHandler(this.serverNode.getId());
        } else if (RoleType.SLAVE.name().equalsIgnoreCase(role)) {
            ReplicaClient.connect2Master();
            ReplicaClient.handleReplicationHandshake();
        }
    }

    private void handleClientConnection(Socket clientSocket) {
        try {
            // handle multiple commands from redis client
            while (!clientSocket.isClosed()) {
                String ans = new RESPParser.Builder()
                        .addClientSocket(clientSocket)
                        .addBufferSize(ParserConstants.RESP_PARSER_BUFFER_SIZE)
                        .build()
                        .process();
                try {
                    if (!RESPUtils.isValidRESPResponse(ans)) {
                        continue;
                    }
                    if (!ans.isBlank()) {
                        OutputStream outputStream = clientSocket.getOutputStream();
                        // attempt to write. If EOF or Broken pipeline, break the loop
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
        // Below are only server node's operations
        System.out.println("Logs from your program will appear here! with " + Arrays.toString(args));
        Main main = new Main();
        main.registerNewEnvVars(args);
        main.registerCommandHandler();
        main.registerRDB();
        main.fillRedisServerInfo();
        main.preCheck();
        main.handleReplicationHandshake();
        main.startServerSocket();
  }
}
