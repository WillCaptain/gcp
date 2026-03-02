package org.twelve.gcp.interpreter.stdlib;

import java.util.ArrayList;
import java.util.List;

/**
 * Thread-local sink for console output produced by the interpreter.
 * <p>
 * Call {@link #start()} before executing a program, then {@link #collect()} afterwards
 * to retrieve all entries in order.  The playground service uses this to forward
 * log / warn / error lines to the browser Console panel.
 */
public final class ConsoleCapture {

    public enum Level { LOG, WARN, ERROR }

    public record Entry(Level level, String message) {}

    private static final ThreadLocal<List<Entry>> STORE =
            ThreadLocal.withInitial(ArrayList::new);

    private ConsoleCapture() {}

    /** Clear any previous entries and begin a new capture session. */
    public static void start() {
        STORE.get().clear();
    }

    /** Append one entry (called by runtime Console.log / .warn / .error and print). */
    public static void add(Level level, String message) {
        STORE.get().add(new Entry(level, message));
    }

    /**
     * Return all captured entries and clear the store.
     * Always safe to call even when {@link #start()} was not called.
     */
    public static List<Entry> collect() {
        List<Entry> result = new ArrayList<>(STORE.get());
        STORE.get().clear();
        return result;
    }
}
