package chat.server;

import chat.additional.ServerMessage;
import chat.additional.Settings;
import chat.user.UserRepo;

import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

import static chat.additional.Role.ADMIN;
import static chat.additional.Role.USER;
import static chat.additional.ServerMessage.*;

public class Server {
    private static Server server;
    private final int PORT;
    private final String HOST;
    private final ExecutorService executorService;
    private final Map<String, Session> sessions;
    private ServerSocket serverSocket;

    public static void main(String[] args) {
        getServer();
        server.run();
    }

    private Server(int port, String host) {
        this.PORT = port;
        this.HOST = host;
        this.executorService = Executors.newCachedThreadPool();
        this.sessions = new ConcurrentHashMap<>();
        registerUser("admin", "12345678", List.of(ADMIN.name(), USER.name())); // add hardcoded admin
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

    public ServerMessage registerUser(String login, String pass, List<String> roles) {
        return pass.length() < 8 ? SHORT_PASSWORD
                : UserRepo.saveIfAbsent(login, encodePass(pass), roles) ? REGISTERED_SUCCESSFULLY
                : LOGIN_ALREADY_TAKEN;
    }

    public ServerMessage authenticateUser(String login, String pass) {
        String repoPass = UserRepo.getUserPassByLogin(login);

        return repoPass == null ? INCORRECT_LOGIN
                : repoPass.equals(encodePass(pass)) ? AUTHORIZED_SUCCESSFULLY
                : INCORRECT_PASSWORD;
    }

    public List<String> getOnlineFriendsOfUser(String login) {
        return sessions
                .keySet()
                .stream()
                .filter(u -> !u.equals(login))
                .sorted()
                .collect(Collectors.toList());
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

    private static String encodePass(String pass) {
        return String.valueOf(pass.hashCode());
    }

    public static Server getServer() {
        if (server == null) {
            server = new Server(Settings.DEFAULT_PORT, Settings.DEFAULT_HOST);
        }
        return server;
    }
}
