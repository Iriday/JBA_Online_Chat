package chat.server;

import chat.additional.ServerMessage;
import chat.user.Chat;
import chat.user.UserRepo;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.net.Socket;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

import static chat.additional.Command.*;
import static chat.additional.Role.*;
import static chat.additional.ServerMessage.*;

public class Session implements Runnable {
    private final Socket socket;
    private final Server server;
    private DataOutputStream outStream;
    private String login;
    private volatile Chat currChat = null;
    private volatile boolean isUserIdentified = false;

    public Session(Socket socket, Server server) {
        this.socket = socket;
        this.server = server;
    }

    @Override
    public void run() {

        try {
            DataInputStream inStream = new DataInputStream(socket.getInputStream());
            outStream = new DataOutputStream(socket.getOutputStream());

            outStream.writeUTF(AUTHORIZE_OR_REGISTER.msg);

            // allow user to input msgs
            while (true) {
                if (!isUserIdentified) {
                    if (!authOrRegister(inStream, outStream)) {
                        break;
                    } else {
                        // make user online
                        server.addSession(login, this);
                        isUserIdentified = true;
                    }
                }

                String clientInput = inStream.readUTF();
                if (UserRepo.isUserBlocked(login)) {
                    outStream.writeUTF(NOT_IN_CHAT.msg);
                    continue;
                }

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

                        // send 25 lest msgs to user
                        outStream.writeUTF(String.join("\n", currChat.getLast25Msgs(login)));
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
                    if (!UserRepo.isUserHasRole(login, ADMIN.name())) {
                        outStream.writeUTF(NOT_ADMIN.msg);
                    } else {
                        String grantTo = clientInput.substring(GRANT.msg.length() + 1);
                        ServerMessage serverMsg = UserRepo.grantRole(grantTo, MODERATOR.name());
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
                } else if (clientInput.startsWith(REVOKE.msg + " ")) {
                    if (!UserRepo.isUserHasRole(login, ADMIN.name())) {
                        outStream.writeUTF(NOT_ADMIN.msg);
                    } else {
                        String revokeFrom = clientInput.substring(REVOKE.msg.length() + 1);
                        ServerMessage serverMsg = UserRepo.removeRole(revokeFrom, MODERATOR.name());
                        if (serverMsg == ROLE_REMOVED) {
                            outStream.writeUTF("Server: " + revokeFrom + " is no longer a moderator!");
                            Session session = server.getSession(revokeFrom);
                            if (session != null) {
                                session.sendMsgToClient(NO_LONGER_MODERATOR.msg);
                            }
                        } else if (serverMsg == NO_ROLE) {
                            outStream.writeUTF(NOT_MODERATOR.msg);
                        } else {
                            outStream.writeUTF(serverMsg.msg);
                        }
                    }
                } else if (clientInput.startsWith(KICK.msg + " ")) {
                    String kick = clientInput.substring(KICK.msg.length() + 1);

                    if (!(UserRepo.isUserHasRole(login, ADMIN.name()) || UserRepo.isUserHasRole(login, MODERATOR.name()))) {
                        outStream.writeUTF(NOT_MODERATOR_OR_ADMIN.msg);
                    } else if (kick.equals(login)) {
                        outStream.writeUTF(CANT_KICK_YOURSELF.msg);
                    } else if (UserRepo.isUserHasRole(login, MODERATOR.name()) && UserRepo.isUserHasRole(kick, MODERATOR.name())) {
                        outStream.writeUTF(CANT_KICK_MODERATOR.msg);
                    } else if (UserRepo.isUserHasRole(kick, ADMIN.name())) {
                        outStream.writeUTF(CANT_KICK_ADMIN.msg);
                    } else {
                        if (!UserRepo.isUserLoginExists(kick)) {
                            outStream.writeUTF(INCORRECT_LOGIN.msg);
                        } else {
                            Session session = server.getSession(kick);
                            if (session == null) {
                                outStream.writeUTF(USER_NOT_ONLINE.msg);
                            } else {
                                UserRepo.setBlocked(kick, LocalDateTime.now().plusSeconds(25));
                                session.isUserIdentified = false;
                                outStream.writeUTF("Server: " + kick + " was kicked!");
                                session.sendMsgToClient(KICKED.msg);
                                server.removeSession(kick);
                                if (currChat != null) {
                                    session.currChat.leaveChat(kick);
                                    session.currChat = null;
                                }
                            }
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
                server.removeSession(login);
                if (currChat != null) {
                    currChat.leaveChat(login);
                }
                socket.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private boolean authOrRegister(DataInputStream inStream, DataOutputStream outStream) throws IOException {

        while (true) {
            String clientInput = inStream.readUTF();
            String[] cmdLoginPass = clientInput.split(" ");

            if (EXIT.msg.equalsIgnoreCase(clientInput)) {
                return false;
            } else if (cmdLoginPass.length == 3 && (cmdLoginPass[0].equals(REGISTRATION.msg) || cmdLoginPass[0].equals(AUTH.msg))) {
                if (UserRepo.isUserBlocked(login)) {
                    outStream.writeUTF(BANNED.msg);
                } else {
                    login = cmdLoginPass[1];
                    String password = cmdLoginPass[2];

                    if (cmdLoginPass[0].equals(REGISTRATION.msg)) {
                        ServerMessage serverMsg = server.registerUser(login, password, List.of(USER.name()));
                        outStream.writeUTF(serverMsg.msg);
                        if (serverMsg == REGISTERED_SUCCESSFULLY) {
                            return true;
                        }
                    } else {
                        ServerMessage serverMsg = server.authenticateUser(login, password);
                        outStream.writeUTF(serverMsg.msg);
                        if (serverMsg == AUTHORIZED_SUCCESSFULLY) {
                            return true;
                        }
                    }
                }
            } else {
                outStream.writeUTF(NOT_IN_CHAT.msg);
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
