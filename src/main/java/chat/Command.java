package chat;

public enum Command {
    AUTH("/auth"),
    REGISTRATION("/registration"),
    LIST("/list"),
    CHAT("/chat"),
    EXIT("/exit"),
    STATISTIC("/stats");

    public final String msg;

    Command(String msg) {
        this.msg = msg;
    }
}
