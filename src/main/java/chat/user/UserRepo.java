package chat.user;

import chat.additional.ServerMessage;
import com.google.gson.Gson;

import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CopyOnWriteArraySet;

import static chat.additional.ServerMessage.*;

public class UserRepo {
    private static final String dbPath;
    private static final Set<User> users;
    private static final Gson gson;

    static {
        dbPath = "usersDb.txt";
        gson = new Gson();
        try {
            if (Files.exists(Path.of(dbPath))) {
                users = deserialize();
            } else {
                users = new CopyOnWriteArraySet<>();
            }
        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    public static synchronized boolean saveIfAbsent(String login, String pass, List<String> roles) {
        if (isUserLoginExists(login)) {
            return false;
        }
        users.add(new User(login, pass, new CopyOnWriteArrayList<>(roles), 0));
        serialize(users);
        return true;
    }

    public static synchronized void serialize(Set<User> users) {
        try (FileWriter fileWriter = new FileWriter(dbPath)) {
            users.forEach(u -> {
                try {
                    fileWriter
                            .append(gson.toJson(u))
                            .append("\n");
                } catch (IOException e) {
                    e.printStackTrace();
                }
            });
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static synchronized Set<User> deserialize() throws IOException {
        List<String> rawUsers = Files.readAllLines(Path.of(dbPath));
        Set<User> newUsers = new CopyOnWriteArraySet<>();
        rawUsers
                .stream()
                .map(u -> gson.fromJson(u, User.class))
                .forEach(newUsers::add);

        return newUsers;
    }

    public static String getUserPassByLogin(String login) {
        return users
                .stream()
                .filter(u -> u.getLogin().equals(login))
                .findFirst()
                .map(User::getPassword)
                .orElse(null);
    }

    public static boolean isUserLoginExists(String login) {
        return users
                .stream()
                .anyMatch(u -> u.getLogin().equals(login));
    }

    public static synchronized ServerMessage grantRole(String login, String role) {
        User user = users
                .stream()
                .filter(u -> u.getLogin().equals(login))
                .findFirst()
                .orElse(null);

        if (user == null) {
            return INCORRECT_LOGIN;
        } else if (user.getRoles().addIfAbsent(role)) {
            serialize(users);
            return ROLE_GRANTED;
        } else return ROLE_WAS_GRANTED_PREVIOUSLY;
    }

    public static synchronized ServerMessage removeRole(String login, String role) {
        User user = users
                .stream()
                .filter(u -> u.getLogin().equals(login))
                .findFirst()
                .orElse(null);

        if (user == null) {
            return INCORRECT_LOGIN;
        } else if (user.getRoles().remove(role)) {
            serialize(users);
            return ROLE_REMOVED;
        } else {
            return NO_ROLE;
        }
    }

    public static boolean isUserHasRole(String login, String role) {
        return users
                .stream()
                .filter(u -> u.getLogin().equals(login))
                .findFirst()
                .orElseThrow(IllegalArgumentException::new)
                .getRoles()
                .contains(role);
    }

    public static boolean isUserBlocked(String login) {
        return users
                .stream()
                .filter(u -> u.getLogin().equals(login))
                .map(u -> u.getBlockedUntil() > LocalDateTime.now().toEpochSecond(ZoneOffset.UTC))
                .findFirst()
                .orElse(false);
    }

    public static void setBlocked(String login, long blockedUntil) {
        users
                .stream()
                .filter(u -> u.getLogin().equals(login))
                .findFirst()
                .orElseThrow(IllegalArgumentException::new)
                .setBlockedUntil(blockedUntil);

        serialize(users);
    }
}
