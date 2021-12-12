package chat.server;

import chat.Log;
import chat.Utils;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.net.Socket;

public class Session implements Runnable {
    private final Socket socket;
    private final long ID;

    public Session(Socket socket, long id) {
        this.socket = socket;
        this.ID = id;

        Log.clientConnected(ID);
    }

    @Override
    public void run() {
        try (DataInputStream inStream = new DataInputStream(socket.getInputStream());
             DataOutputStream outStream = new DataOutputStream(socket.getOutputStream())) {

            while (true) {
                String data = inStream.readUTF();
                if ("/exit".equalsIgnoreCase(data)) {
                    Log.clientDisconnected(ID);
                    break;
                }

                Log.clientSentDataToServer(ID, data);
                String response = "Count is " + Utils.countWords(data);
                outStream.writeUTF(response);
                Log.serverSentDataToClient(ID, response);
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
}
