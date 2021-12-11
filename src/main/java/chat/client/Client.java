package chat.client;

import chat.Settings;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.Scanner;

public class Client {
    private final int PORT;
    private final String HOST;

    public Client(int port, String host) {
        this.PORT = port;
        this.HOST = host;
    }

    public static void main(String[] args) {
        new Client(Settings.DEFAULT_PORT, Settings.DEFAULT_HOST).run();
    }

    private void run() {
        var scn = new Scanner(System.in);

        try (Socket socket = new Socket(HOST, PORT)) {
            System.out.println("Client started!");
            try (DataInputStream inStream = new DataInputStream(socket.getInputStream());
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
