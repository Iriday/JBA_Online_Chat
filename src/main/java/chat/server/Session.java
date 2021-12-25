package chat.server;

import chat.ServerMessage;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;

import static chat.Command.*;
import static chat.ServerMessage.*;

public class Session implements Runnable {
    private final Socket socket;
    private final Server server;
    private DataOutputStream outStream;
    private String login;

    public Session(Socket socket, Server server) {
        this.socket = socket;
        this.server = server;
    }

    @Override
    public void run() {

        try {
            DataInputStream inStream = new DataInputStream(socket.getInputStream());
            outStream = new DataOutputStream(socket.getOutputStream());

            // auth/register user
            outStream.writeUTF(AUTHORIZE_OR_REGISTER.msg);
            while (true) {
                String[] cmdLoginAndPass = inStream.readUTF().split(" ");
                if (cmdLoginAndPass.length != 3) {
                    outStream.writeUTF(NOT_IN_CHAT.msg);
                    continue;
                }

                login = cmdLoginAndPass[1];
                String password = cmdLoginAndPass[2];

                if (cmdLoginAndPass[0].equals(REGISTRATION.msg)) {
                    ServerMessage serverMsg = server.registerUser(login, password);
                    outStream.writeUTF(serverMsg.msg);
                    if (serverMsg == REGISTERED_SUCCESSFULLY) {
                        break;
                    }

                } else if (cmdLoginAndPass[0].equals(AUTH.msg)) {
                    ServerMessage serverMsg = server.authenticateUser(login, password);
                    outStream.writeUTF(serverMsg.msg);
                    if (serverMsg == AUTHORIZED_SUCCESSFULLY) {
                        break;
                    }
                } else {
                    outStream.writeUTF(NOT_IN_CHAT.msg);
                }
            }

            server.addSession(login, this);

            // send 10 lest msgs to user
            for (var msg : server.getTenLastMsgs()) {
                outStream.writeUTF(msg);
            }

            // allow user to input msgs
            while (true) {
                String data = inStream.readUTF();
                if (EXIT.msg.equalsIgnoreCase(data)) {
                    server.removeSession(login);
                    break;
                }

                server.sendMessageToAllClients(login + ": " + data);
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
