package chat;

public enum ServerMessage {
    INCORRECT_LOGIN("incorrect login!"),
    INCORRECT_PASSWORD("incorrect password!"),
    SHORT_PASSWORD("the password is too short!"),
    LOGIN_ALREADY_TAKEN("this login is already taken! Choose another one."),
    USER_NOT_ONLINE("the user is not online!"),
    INCORRECT_COMMAND("incorrect command!"),

    AUTHORIZE_OR_REGISTER("authorize or register"),
    REGISTERED_SUCCESSFULLY("you are registered successfully!"),
    AUTHORIZED_SUCCESSFULLY("you are authorized successfully!"),
    NOT_IN_CHAT("you are not in the chat!"),
    LIST_COMMAND("use /list command to choose a user to text!"),
    NO_ONE_ONLINE("no one online"),
    ONLINE("online: "),
    VAL_SHOULD_BE_POSITIVE("value should be positive"),
    NO_ONE_UNREAD("no one unread"),

    ROLE_WAS_GRANTED_PREVIOUSLY("role was granted previously"),
    ROLE_GRANTED("role was granted successfully"),
    ALREADY_MODERATOR("this user is already a moderator!"),
    NOT_ADMIN("you are not an admin!"),
    NEW_MODERATOR("you are the new moderator now!");


    public final String msg;

    ServerMessage(String msg) {
        this.msg = "Server: " + msg;
    }
}
