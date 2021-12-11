package chat.server;

import chat.Settings;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Scanner;

public class Server {
    private final int PORT;
    private final String HOST;

    public static void main(String[] args) {
        new Server(Settings.DEFAULT_PORT, Settings.DEFAULT_HOST).run();
    }

    public Server(int port, String host) {
        this.PORT = port;
        this.HOST = host;
    }

    public void run() {
        var scn = new Scanner(System.in);

        try (ServerSocket serverSocket = new ServerSocket(PORT, 30, InetAddress.getByName(HOST))) {
            System.out.println("Server started!");

            try (Socket socket = serverSocket.accept();
                 DataInputStream inStream = new DataInputStream(socket.getInputStream());
                 DataOutputStream outStream = new DataOutputStream(socket.getOutputStream())) {

                outStream.writeUTF(scn.nextLine());
                outStream.writeUTF(scn.nextLine());

                System.out.println(inStream.readUTF());
                System.out.println(inStream.readUTF());
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
