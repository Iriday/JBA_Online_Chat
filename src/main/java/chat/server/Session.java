package chat.server;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;

public class Session implements Runnable {
    private final Socket socket;
    private final long ID;
    private final Server server;
    private DataOutputStream outStream;
    private String clientName;

    public Session(Socket socket, long id, Server server) {
        this.socket = socket;
        this.ID = id;
        this.server = server;
    }

    @Override
    public void run() {

        try {
            DataInputStream inStream = new DataInputStream(socket.getInputStream());
            outStream = new DataOutputStream(socket.getOutputStream());

            // register user
            outStream.writeUTF("Server: write your name");
            while (true) {
                clientName = inStream.readUTF();
                boolean isAdded = server.addSession(clientName, this);
                if (isAdded) {
                    break;
                } else {
                    outStream.writeUTF("Server: this name is already taken! Choose another one.");
                }
            }

            // send 10 lest msgs to user
            for (var msg : server.getTenLastMsgs()) {
                outStream.writeUTF(msg);
            }

            // allow user to input msgs
            while (true) {
                String data = inStream.readUTF();
                if ("/exit".equalsIgnoreCase(data)) {
                    server.removeSession(clientName);
                    break;
                }

                server.sendMessageToAllClients(clientName + ": " + data);
            }

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                socket.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public void sendMsgToClient(String msg) {
        try {
            outStream.writeUTF(msg);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
