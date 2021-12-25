package chat.server;

import chat.ServerMessage;
import chat.Settings;

import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static chat.ServerMessage.*;

public class Server {
    private final int PORT;
    private final String HOST;
    private final ExecutorService executorService;
    private final Map<String, Session> sessions;
    private final Map<String, String> usersDb;
    private ServerSocket serverSocket;


    public static void main(String[] args) {
        new Server(Settings.DEFAULT_PORT, Settings.DEFAULT_HOST).run();
    }

    public Server(int port, String host) {
        this.PORT = port;
        this.HOST = host;
        this.executorService = Executors.newFixedThreadPool(3);
        this.sessions = new ConcurrentHashMap<>();
        this.usersDb = new ConcurrentHashMap<>();
    }

    public void run() {
        try {
            serverSocket = new ServerSocket(PORT, 30, InetAddress.getByName(HOST));
            System.out.println("Server started!");

            while (true) {
                executorService.submit(new Session(serverSocket.accept(), this));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void addSession(String login, Session session) {
        sessions.putIfAbsent(login, session);
    }

    public void removeSession(String login) {
        sessions.remove(login);
    }

    public ServerMessage registerUser(String login, String pass) {
        return pass.length() < 8 ? SHORT_PASSWORD
                : usersDb.putIfAbsent(login, pass) == null ? REGISTERED_SUCCESSFULLY
                : LOGIN_ALREADY_TAKEN;
    }

    public ServerMessage authenticateUser(String login, String pass) {
        return !usersDb.containsKey(login) ? INCORRECT_LOGIN
                : usersDb.get(login).equals(pass) ? AUTHORIZED_SUCCESSFULLY
                : INCORRECT_PASSWORD;
    }

    public Set<String> getOnlineFriendsOfUser(String login) {
        var friends = new HashSet<>(sessions.keySet());
        friends.remove(login);
        return friends;
    }

    public Session getSession(String login) {
        return sessions.get(login);
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
