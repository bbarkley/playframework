package play.api.libs.concurrent;

/**
 * @author bbarkley
 */
public class ThreadLogBuffer {

    private static ThreadLocal<StringBuilder> INSTANCE = new ThreadLocal<StringBuilder>();

    public static void log(String val) {
        if (INSTANCE.get() == null) {
            INSTANCE.set(new StringBuilder());
        }
        INSTANCE.get().append(val);
    }

    public static String get() {
        return INSTANCE.get() == null ? null : INSTANCE.get().toString();
    }

    public static void clear() {
        INSTANCE.set(null);
    }

}
