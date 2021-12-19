package chat.server;

import chat.Settings;

import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Server {
    private final int PORT;
    private final String HOST;
    private final ExecutorService executorService;
    private final Map<String, Session> sessions;
    private final List<String> tenLastMsgs;
    private int numberOfCreatedSessions = 0;
    private ServerSocket serverSocket;


    public static void main(String[] args) {
        new Server(Settings.DEFAULT_PORT, Settings.DEFAULT_HOST).run();
    }

    public Server(int port, String host) {
        this.PORT = port;
        this.HOST = host;
        this.executorService = Executors.newFixedThreadPool(3);
        this.sessions = new ConcurrentHashMap<>();
        this.tenLastMsgs = new CopyOnWriteArrayList<>();
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
        tenLastMsgs.add(msg);
        if (tenLastMsgs.size() >= 11) {
            tenLastMsgs.remove(0);
        }

        sessions.values().forEach(v -> v.sendMsgToClient(msg));
    }

    public List<String> getTenLastMsgs(){
        return tenLastMsgs;
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
