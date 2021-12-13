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
    private Thread messageReader;

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

                // register name
                while (true) {
                    String msg = inStream.readUTF();

                    if ("Server: write your name".equalsIgnoreCase(msg)
                            || "Server: this name is already taken! Choose another one.".equalsIgnoreCase(msg)) {

                        System.out.println(msg);
                        outStream.writeUTF(scn.nextLine());
                        break;
                    } else {
                        System.out.println("Error. Server sent unknown command");
                    }
                }

                // receive messages
                messageReader = createMessageReader(inStream, socket);
                messageReader.start();

                // send messages
                while (true) {
                    String msg = scn.nextLine();
                    outStream.writeUTF(msg);
                    if ("/exit".equalsIgnoreCase(msg)) {
                        break;
                    }
                }

            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static Thread createMessageReader(DataInputStream inputStream, Socket socket) {
        var t = new Thread(() -> {
            while (!socket.isClosed()) {
                try {
                    System.out.println(inputStream.readUTF());
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });

        t.setDaemon(true);

        return t;
    }
}
