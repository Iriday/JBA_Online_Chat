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
    LIST_COMMAND("user /list command to choose a user to text!"),
    NO_ONE_ONLINE("no one online"),
    ONLINE("online: ");


    public final String msg;

    ServerMessage(String msg) {
        this.msg = "Server: " + msg;
    }
}
