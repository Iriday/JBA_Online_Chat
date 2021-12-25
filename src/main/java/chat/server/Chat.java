package chat.server;

import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CopyOnWriteArraySet;

public class Chat {
    private static final List<Chat> chats = new CopyOnWriteArrayList<>();

    private final Server server;
    private final Set<String> users; // logins
    private final Set<String> usersCurrInChat; // logins
    private final List<String> messages;

    private Chat(Set<String> users, Server server) {
        this.server = server;
        this.users = Collections.unmodifiableSet(users);
        this.usersCurrInChat = new CopyOnWriteArraySet<>();
        this.messages = new CopyOnWriteArrayList<>();
    }

    public static Chat getChat(Set<String> users, Server server) {
        Optional<Chat> chat = chats
                .stream()
                .filter(c -> c.users.equals(users))
                .findFirst();

        if (chat.isPresent()) {
            return chat.get();
        } else {
            Chat newChat = new Chat(users, server);
            chats.add(newChat);
            return newChat;
        }
    }

    public List<String> getTenLastMsgs() {
        return messages.subList(messages.size() < 10 ? 0 : messages.size() - 10, messages.size());
    }

    public void sendMessage(String sender, String msg) {
        String fullMsg = sender + " " + msg;
        messages.add(fullMsg);

        usersCurrInChat
                .stream()
                .map(server::getSession)
                .filter(Objects::nonNull)
                .forEach(session -> session.sendMsgToClient(fullMsg));
    }

    public void joinChat(String user) {
        if (users.contains(user)) {
            usersCurrInChat.add(user);
        } else {
            throw new IllegalArgumentException("Specified user is not a member of this chat");
        }
    }

    public void leaveChat(String user) {
        usersCurrInChat.remove(user);
    }
}
