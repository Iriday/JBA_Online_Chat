package chat.server;

import chat.ServerMessage;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.net.Socket;
import java.util.List;
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
                    ServerMessage serverMsg = server.registerUser(login, password, List.of("USER"));
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
                } else if (STATISTIC.msg.equals(clientInput)) {
                    outStream.writeUTF(currChat == null ? LIST_COMMAND.msg : currChat.getStatistic(login));
                } else if (clientInput.startsWith(HISTORY.msg + " ")) {
                    if (currChat == null) {
                        outStream.writeUTF(LIST_COMMAND.msg);
                    } else {
                        String fromStr = clientInput.substring(HISTORY.msg.length() + 1);
                        int from;
                        try {
                            var fromBigInt = new BigInteger(fromStr);

                            try {
                                from = fromBigInt.intValueExact();
                            } catch (ArithmeticException e) {
                                outStream.writeUTF("Server: value should not be bigger than: " + Integer.MAX_VALUE);
                                continue;
                            }

                            if (from < 0) {
                                outStream.writeUTF(VAL_SHOULD_BE_POSITIVE.msg);
                                continue;
                            }
                        } catch (NumberFormatException e) {
                            outStream.writeUTF("Server: " + fromStr + " is not a number!");
                            continue;
                        }
                        outStream.writeUTF("Server:\n"
                                + String.join("\n", currChat.getNLastMsgsStartingFrom(from, 25)));
                    }
                } else if (UNREAD.msg.equals(clientInput)) {
                    var users = Chat.getUsersThatSentUnreadMsgs(login);
                    String usersStr = String.join(" ", users);
                    outStream.writeUTF(users.size() == 0 ? NO_ONE_UNREAD.msg : "Server: unread from: " + usersStr);
                } else if (clientInput.startsWith(GRANT.msg + " ")) {
                    if (!UserRepo.isUserHasRole(login, "ADMIN")) {
                        outStream.writeUTF(NOT_ADMIN.msg);
                    } else {
                        String grantTo = clientInput.substring(GRANT.msg.length() + 1);
                        ServerMessage serverMsg = UserRepo.grantRole(grantTo, "MODERATOR");
                        if (serverMsg == ROLE_GRANTED) {
                            outStream.writeUTF("Server: " + grantTo + " is the new moderator!");
                            Session session = server.getSession(grantTo);
                            if (session != null) {
                                session.sendMsgToClient(NEW_MODERATOR.msg);
                            }
                        } else if (serverMsg == ROLE_WAS_GRANTED_PREVIOUSLY) {
                            outStream.writeUTF(ALREADY_MODERATOR.msg);
                        } else {
                            outStream.writeUTF(serverMsg.msg);
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
