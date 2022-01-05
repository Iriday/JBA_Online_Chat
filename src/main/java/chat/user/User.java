package chat.user;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.concurrent.CopyOnWriteArrayList;

@AllArgsConstructor
@Data
public class User {
    public final String login;
    public final String password;
    public final CopyOnWriteArrayList<String> roles;
    public long blockedUntil;
}
