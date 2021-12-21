package chat;

public enum Command {
    AUTH("/auth"),
    REGISTRATION("/registration"),
    LIST("/list"),
    CHAT("/chat"),
    EXIT("/exit");

    public final String command;

    Command(String command) {
        this.command = command;
    }
}
