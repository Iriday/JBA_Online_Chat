package chat.server;

import chat.Settings;

import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Server {
    private final int PORT;
    private final String HOST;
    private final ExecutorService executorService;
    private int numberOfCreatedSessions = 0;
    private ServerSocket serverSocket;
    private Map<String, Session> sessions;


    public static void main(String[] args) {
        new Server(Settings.DEFAULT_PORT, Settings.DEFAULT_HOST).run();
    }

    public Server(int port, String host) {
        this.PORT = port;
        this.HOST = host;
        this.executorService = Executors.newFixedThreadPool(3);
        sessions = new ConcurrentHashMap<>();
    }

    public void run() {
        try {
            serverSocket = new ServerSocket(PORT, 30, InetAddress.getByName(HOST));
            System.out.println("Server started!");

            while (true) {
                executorService.submit(new Session(serverSocket.accept(), ++numberOfCreatedSessions, this));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public boolean addSession(String id, Session session) {
        return sessions.putIfAbsent(id, session) == null;
    }

    public void removeSession(String id) {
        sessions.remove(id);
    }

    public void sendMessageToAllClients(String msg) {
        sessions.values().forEach(v -> v.sendMsgToClient(msg));
    }

    public void stop() {
        executorService.shutdownNow();

        try {
            serverSocket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
