package app;

public class Log {

    public static final int ALL = 1;
    public static final int DEBUG = 2;
    public static final int INFO = 3;
    // ....

    public static void debug(String message) {
        if (Settings.getInstance().getLogLevel() <= DEBUG) {
            System.out.println(message);
        }
    }

    public static void info(String message) {
        if (Settings.getInstance().getLogLevel() <= INFO) {
            System.out.println(message);
        }
    }

}