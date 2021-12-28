package chat.server;

import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class UserRepo {
    private static final String dbPath = "usersDb.txt";
    private static final Map<String, String> users;

    static {
        users = new ConcurrentHashMap<>();
        try {
            if (Files.exists(Path.of(dbPath))) {
                Files
                        .newBufferedReader(Path.of(dbPath))
                        .lines()
                        .map(l -> l.split(" : ", 2))
                        .forEach(arr -> users.put(arr[1], arr[0]));
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static synchronized boolean saveIfAbsent(String login, String pass) {
        if (users.containsKey(login)) {
            return false;
        }

        try {
            users.put(login, pass);
            try (FileWriter fileWriter = new FileWriter(dbPath, true)) {
                fileWriter
                        .append(pass)
                        .append(" : ")
                        .append(login)
                        .append("\n");
            }
            return true;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static String getUserPassByLogin(String login) {
        return users.get(login);
    }
}
