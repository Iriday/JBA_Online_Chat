package chat;

public class Log {
    public static void clientConnected(long id) {
        System.out.println("Client " + id + " connected!");
    }

    public static void clientDisconnected(long id) {
        System.out.println("Client " + id + " disconnected!");
    }

    public static void clientSentDataToServer(long id, String data) {
        System.out.println("Client " + id + " sent: " + data);
    }

    public static void serverSentDataToClient(long id, String data) {
        System.out.println("Sent to client " + id + ": " + data);
    }
}
