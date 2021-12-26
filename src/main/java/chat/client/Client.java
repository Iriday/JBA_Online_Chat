package chat.client;

import chat.Command;
import chat.Settings;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.Scanner;

import static chat.ServerMessage.*;

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

                // register/auth
                while (true) {
                    String msg = inStream.readUTF();

                    if (AUTHORIZE_OR_REGISTER.msg.equalsIgnoreCase(msg)
                            || INCORRECT_LOGIN.msg.equalsIgnoreCase(msg)
                            || LOGIN_ALREADY_TAKEN.msg.equals(msg)
                            || INCORRECT_PASSWORD.msg.equals(msg)
                            || SHORT_PASSWORD.msg.equals(msg)
                            || NOT_IN_CHAT.msg.equals(msg)) {
                        System.out.println(msg);
                        outStream.writeUTF(scn.nextLine());
                    } else if (REGISTERED_SUCCESSFULLY.msg.equals(msg)
                            || AUTHORIZED_SUCCESSFULLY.msg.equals(msg)) {
                        System.out.println(msg);
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
                    if (Command.EXIT.msg.equalsIgnoreCase(msg)) {
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
