package chat.user;

import chat.server.Server;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.stream.Collectors;

public class Chat {
    private static final List<Chat> chats;
    private static final String dbPath;
    private static final Server server;
    private static Gson gson;

    static {
        server = Server.getServer();
        dbPath = "chatsDb.txt";
        gson = new Gson();
        if (Files.exists(Path.of(dbPath))) {
            chats = deserialize(dbPath, gson);
        } else {
            chats = new CopyOnWriteArrayList<>();
        }
    }

    private final Set<String> users; // logins
    private final Set<String> usersCurrInChat; // logins
    private final List<Map.Entry<String, Set<String>>> messages;

    private Chat(Set<String> users) {
        this.users = Collections.unmodifiableSet(users);
        this.usersCurrInChat = new CopyOnWriteArraySet<>();
        this.messages = new CopyOnWriteArrayList<>();
    }

    private Chat(Set<String> users, List<Map.Entry<String, Set<String>>> messages) {
        this.users = Collections.unmodifiableSet(users);
        this.usersCurrInChat = new CopyOnWriteArraySet<>();
        this.messages = messages;
    }

    public static Chat getChat(Set<String> users) {
        Optional<Chat> chat = chats
                .stream()
                .filter(c -> c.users.equals(users))
                .findFirst();

        if (chat.isPresent()) {
            return chat.get();
        } else {
            Chat newChat = new Chat(users);
            chats.add(newChat);
            return newChat;
        }
    }

    public synchronized List<String> getLast25Msgs(String currUser) {
        var raw25Msgs = messages
                .subList(messages.size() < 25 ? 0 : messages.size() - 25, messages.size());

        long numOfViewedMsgs = raw25Msgs
                .stream()
                .filter(m -> !m.getValue().contains(currUser))
                .count();
        while (numOfViewedMsgs-- > 10) {
            raw25Msgs.remove(0);
        }

        var formattedMsgs = appendNewAndFormat(raw25Msgs, currUser);

        makeMessagesNotNew(messages, currUser);
        serialize(chats, dbPath, gson);

        return formattedMsgs;
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

        serialize(chats, dbPath, gson);
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

    private static synchronized void serialize(List<Chat> chats, String dbPath, Gson gson) {
        try (FileWriter fileWriter = new FileWriter(dbPath)) {
            chats.forEach(c -> {
                try {
                    fileWriter
                            .append(gson.toJson(c.users))
                            .append("\n")
                            .append(gson.toJson(c.messages))
                            .append("\n");
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            });
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static List<Chat> deserialize(String dbPath, Gson gson) {
        List<String> raw;
        try {
            raw = Files.readAllLines(Path.of(dbPath));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        List<Chat> newChats = new CopyOnWriteArrayList<>();
        for (int i = 0; i < raw.size(); i += 2) {
            Set<String> newUsers = gson
                    .fromJson(raw.get(i), new TypeToken<CopyOnWriteArraySet<String>>() {
                    }.getType());

            List<Map.Entry<String, Set<String>>> newMessages = gson
                    .fromJson(raw.get(i + 1), new TypeToken<CopyOnWriteArrayList<AbstractMap.SimpleEntry<String, CopyOnWriteArraySet<String>>>>() {
                    }.getType());

            newChats.add(new Chat(newUsers, newMessages));
        }

        return newChats;
    }

    public String getStatistic(String curUserName) {
        long curUserMsgsLen = messages
                .stream()
                .filter(e -> e.getKey().startsWith(curUserName + ": "))
                .count();

        String secUserName = users
                .stream()
                .filter(u -> !u.equals(curUserName))
                .findFirst()
                .orElseThrow(IllegalStateException::new);

        return new StringBuilder()
                .append("Server:\n")
                .append("Statistics with ").append(secUserName)
                .append(":\n")
                .append("Total messages: ").append(messages.size())
                .append("\n")
                .append("Messages from ").append(curUserName).append(": ").append(curUserMsgsLen)
                .append("\n")
                .append("Messages from ").append(secUserName).append(": ").append(messages.size() - curUserMsgsLen)
                .toString();
    }

    public synchronized List<String> getNLastMsgsStartingFrom(int from, int len) {
        if (from < 0 || len < 0) {
            throw new IllegalArgumentException("Values should not be negative");
        }

        int indexFrom = messages.size() < from ? 0 : messages.size() - from;
        int indexTo = Math.min(messages.size() - from + len, messages.size());

        if (indexTo <= 0) {
            return List.of();
        }

        return messages
                .subList(indexFrom, indexTo)
                .stream()
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());
    }

    public static List<String> getUsersThatSentUnreadMsgs(String login) {
        return chats
                .stream()
                .filter(c -> c.users.contains(login))
                .filter(c -> c.messages.stream().anyMatch(m -> m.getValue().contains(login)))
                .map(c -> c.users.stream().filter(u -> !u.equals(login)).findFirst().orElseThrow(IllegalStateException::new))
                .sorted()
                .collect(Collectors.toList());
    }
}
