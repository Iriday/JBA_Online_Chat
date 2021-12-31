package chat.server;

import chat.ServerMessage;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.Set;

import static chat.Command.*;
import static chat.ServerMessage.*;

public class Session implements Runnable {
    private final Socket socket;
    private final Server server;
    private DataOutputStream outStream;
    private String login;
    private Chat currChat = null;

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

            // make user online
            server.addSession(login, this);

            // allow user to input msgs
            while (true) {
                String clientInput = inStream.readUTF();

                if (EXIT.msg.equalsIgnoreCase(clientInput)) {
                    server.removeSession(login);
                    if (currChat != null) {
                        currChat.leaveChat(login);
                    }
                    break;
                } else if (LIST.msg.equalsIgnoreCase(clientInput)) {
                    var friends = server.getOnlineFriendsOfUser(login);
                    outStream.writeUTF(friends.isEmpty() ? NO_ONE_ONLINE.msg
                            : ONLINE.msg + String.join(" ", friends));
                } else if (clientInput.startsWith(CHAT.msg + " ")) {
                    String loginOfSecondUser = clientInput.substring(CHAT.msg.length() + 1);

                    if (server.getSession(loginOfSecondUser) == null) {
                        outStream.writeUTF(USER_NOT_ONLINE.msg);
                    } else {
                        if (currChat != null) {
                            currChat.leaveChat(login);
                        }
                        currChat = Chat.getChat(Set.of(login, loginOfSecondUser));
                        currChat.joinChat(login);

                        // send 10 lest msgs to user
                        for (var msg : currChat.getTenLastMsgs(login)) {
                            outStream.writeUTF(msg);
                        }
                    }
                } else if (clientInput.startsWith("/")) {
                    outStream.writeUTF(INCORRECT_COMMAND.msg);
                } else {
                    if (currChat == null) {
                        outStream.writeUTF(LIST_COMMAND.msg);
                    } else {
                        currChat.sendMessage(login, clientInput);
                    }
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                if (currChat != null) {
                    currChat.leaveChat(login);
                }
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
