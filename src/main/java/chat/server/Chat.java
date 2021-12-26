package chat.server;

import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.stream.Collectors;

public class Chat {
    private static final List<Chat> chats = new CopyOnWriteArrayList<>();

    private final Server server;
    private final Set<String> users; // logins
    private final Set<String> usersCurrInChat; // logins
    private final List<Map.Entry<String, Set<String>>> messages;

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

    public synchronized List<String> getTenLastMsgs(String currUser) {
        var rawTenMessages = messages.subList(messages.size() < 10 ? 0 : messages.size() - 10, messages.size());
        var formattedTenMsgs = appendNewAndFormat(rawTenMessages, currUser);
        makeMessagesNotNew(messages, currUser);
        return formattedTenMsgs;
    }

    public synchronized void sendMessage(String sender, String msg) {
        String fullMsg = sender + ": " + msg;
        Set<String> notInChatUsers = getNotInChatUsers(users, usersCurrInChat);

        messages.add(Map.entry(fullMsg, notInChatUsers));

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

    private static Set<String> getNotInChatUsers(Set<String> users, Set<String> usersCurrInChat) {
        return users
                .stream()
                .filter(u -> !usersCurrInChat.contains(u))
                .collect(Collectors.toCollection(CopyOnWriteArraySet::new));
    }

    private static List<String> appendNewAndFormat(List<Map.Entry<String, Set<String>>> messages, String currUser) {
        return messages
                .stream()
                .map(l -> l.getValue().contains(currUser) ? "(new) " + l.getKey() : l.getKey())
                .collect(Collectors.toList());
    }

    private static void makeMessagesNotNew(List<Map.Entry<String, Set<String>>> messages, String currUser) {
        for (int i = messages.size() - 1; i >= 0; i--) {
            var notOnlineUsers = messages.get(i).getValue();
            if (!notOnlineUsers.remove(currUser)) {
                break;
            }
        }
    }
}
