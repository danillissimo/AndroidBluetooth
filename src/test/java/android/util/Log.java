package android.util;

public class Log {
    public static int d(String tag, String msg) {
        System.out.println("DEBUG: " + tag + ": " + msg);
        return 0;
    }

    public static int i(String tag, String msg) {
        System.out.println("INFO: " + tag + ": " + msg);
        return 0;
    }

    public static int w(String tag, String msg) {
        System.out.println("WARN: " + tag + ": " + msg);
        return 0;
    }

    public static int w(String tag, String msg, Throwable t) {
        System.out.println("WARN: " + tag + ": " + msg + "\n" + t.getLocalizedMessage());
        return 0;
    }

    public static int e(String tag, String msg) {
        System.out.println("ERROR: " + tag + ": " + msg);
        return 0;
    }

    public static int v(String tag, String msg){
        System.out.println("VERBOSE: " + tag + ": " + msg);
        return 0;
    }

    public static int v(String tag, String msg, Throwable t){
        System.out.println("VERBOSE: " + tag + ": " + msg + "\n" + t.getLocalizedMessage());
        return 0;
    }

    public static int wtf(String tag, String msg){
        System.out.println("WTF: " + tag + ": " + msg);
        return 0;
    }

    // add other methods if required...
}