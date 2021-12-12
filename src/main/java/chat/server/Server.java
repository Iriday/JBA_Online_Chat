package chat.server;

import chat.Settings;

import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Server {
    private final int PORT;
    private final String HOST;
    private final ExecutorService executorService;
    private int numberOfCreatedSessions = 0;
    private ServerSocket serverSocket;


    public static void main(String[] args) {
        new Server(Settings.DEFAULT_PORT, Settings.DEFAULT_HOST).run();
    }

    public Server(int port, String host) {
        this.PORT = port;
        this.HOST = host;
        this.executorService = Executors.newFixedThreadPool(3);
    }

    public void run() {
        try {
            serverSocket = new ServerSocket(PORT, 30, InetAddress.getByName(HOST));
            System.out.println("Server started!");

            while (true) {
                executorService.submit(new Session(serverSocket.accept(), ++numberOfCreatedSessions));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
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
