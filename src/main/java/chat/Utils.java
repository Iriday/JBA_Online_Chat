package chat;

public class Utils {
    public static int countWords(String str){
        return str.trim().split("\\s+").length;
    }
}
