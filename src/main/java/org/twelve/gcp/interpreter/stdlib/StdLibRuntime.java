package org.twelve.gcp.interpreter.stdlib;

import org.twelve.gcp.common.StdLib;
import org.twelve.gcp.interpreter.Environment;
import org.twelve.gcp.interpreter.value.*;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;

/**
 * Runtime implementations of the stdlib built-in entity modules.
 * Mirrors the type-level definitions in {@link StdLib}.
 *
 * <p>Each module is represented as an {@link EntityValue} whose fields are
 * {@link FunctionValue} lambdas (marked as builtins via the {@code v -> ...} constructor).
 */
public final class StdLibRuntime {

    private StdLibRuntime() {}

    /** Register all stdlib runtime values into the given interpreter environment. */
    public static void registerAll(Environment env) {
        env.define("Date",    buildDate());
        env.define("Console", buildConsole());
        env.define("Math",    buildMath());
    }

    /**
     * Reset any per-request stateful stdlib resources (e.g. virtual filesystem).
     * Called by the playground before each code execution to avoid cross-request state leakage.
     * No-op in the base implementation; extended runtimes may override via subclassing.
     */
    public static void resetFs() {
        // no virtual-filesystem state in the base runtime
    }

    // ── Date ──────────────────────────────────────────────────────────────────

    private static EntityValue buildDateRecord(LocalDateTime dt) {
        LinkedHashMap<String, Value> fields = new LinkedHashMap<>();
        fields.put("year",   new IntValue(dt.getYear()));
        fields.put("month",  new IntValue(dt.getMonthValue()));
        fields.put("day",    new IntValue(dt.getDayOfMonth()));
        fields.put("hour",   new IntValue(dt.getHour()));
        fields.put("minute", new IntValue(dt.getMinute()));
        fields.put("second", new IntValue(dt.getSecond()));
        fields.put("format", new FunctionValue(fmtVal -> {
            String pattern = ((StringValue) fmtVal).value()
                    .replace("YYYY", "yyyy")
                    .replace("DD",   "dd");
            try {
                return new StringValue(dt.format(DateTimeFormatter.ofPattern(pattern)));
            } catch (Exception e) {
                return new StringValue(dt.toString());
            }
        }));
        fields.put("timestamp",   new FunctionValue(u -> new IntValue(dt.toEpochSecond(ZoneOffset.UTC))));
        fields.put("day_of_week", new FunctionValue(u -> new IntValue(dt.getDayOfWeek().getValue())));
        fields.put("to_str",      new FunctionValue(u -> new StringValue(dt.toString())));
        return new EntityValue(fields);
    }

    private static EntityValue buildDate() {
        LinkedHashMap<String, Value> fields = new LinkedHashMap<>();
        fields.put("now", new FunctionValue(u -> buildDateRecord(LocalDateTime.now())));
        fields.put("parse", new FunctionValue(v -> {
            String s = ((StringValue) v).value();
            try {
                LocalDateTime dt = LocalDateTime.parse(s + "T00:00:00");
                return buildDateRecord(dt);
            } catch (Exception e) {
                // Try date-only format: "YYYY-MM-DD"
                try {
                    String[] parts = s.split("-");
                    LocalDateTime dt = LocalDateTime.of(
                            Integer.parseInt(parts[0]),
                            Integer.parseInt(parts[1]),
                            Integer.parseInt(parts[2]),
                            0, 0, 0);
                    return buildDateRecord(dt);
                } catch (Exception ex) {
                    return buildDateRecord(LocalDateTime.now());
                }
            }
        }));
        return new EntityValue(fields);
    }

    // ── Console ───────────────────────────────────────────────────────────────

    private static EntityValue buildConsole() {
        LinkedHashMap<String, Value> fields = new LinkedHashMap<>();
        fields.put("log", new FunctionValue(v -> {
            String msg = v.display();
            ConsoleCapture.add(ConsoleCapture.Level.LOG, msg);
            System.out.println(msg);
            return UnitValue.INSTANCE;
        }));
        fields.put("warn", new FunctionValue(v -> {
            String msg = v.display();
            ConsoleCapture.add(ConsoleCapture.Level.WARN, msg);
            System.out.println("[WARN] " + msg);
            return UnitValue.INSTANCE;
        }));
        fields.put("error", new FunctionValue(v -> {
            String msg = v.display();
            ConsoleCapture.add(ConsoleCapture.Level.ERROR, msg);
            System.err.println("[ERROR] " + msg);
            return UnitValue.INSTANCE;
        }));
        fields.put("read", new FunctionValue(u -> {
            try {
                java.util.Scanner scanner = new java.util.Scanner(System.in);
                if (scanner.hasNextLine()) return new StringValue(scanner.nextLine());
            } catch (Exception ignored) {}
            return new StringValue("");
        }));
        return new EntityValue(fields);
    }

    // ── Math ──────────────────────────────────────────────────────────────────

    private static EntityValue buildMath() {
        LinkedHashMap<String, Value> fields = new LinkedHashMap<>();
        fields.put("pi",     new FloatValue(Math.PI));
        fields.put("e",      new FloatValue(Math.E));
        fields.put("sqrt",   new FunctionValue(v -> new FloatValue(Math.sqrt(toDouble(v)))));
        fields.put("abs",    new FunctionValue(v -> {
            if (v instanceof IntValue iv) return new IntValue(Math.abs(iv.value()));
            return new FloatValue(Math.abs(toDouble(v)));
        }));
        fields.put("floor",  new FunctionValue(v -> new IntValue((long) Math.floor(toDouble(v)))));
        fields.put("ceil",   new FunctionValue(v -> new IntValue((long) Math.ceil(toDouble(v)))));
        fields.put("round",  new FunctionValue(v -> new IntValue(Math.round(toDouble(v)))));
        fields.put("max",    new FunctionValue(a -> new FunctionValue(b ->
                toDouble(a) >= toDouble(b) ? a : b)));
        fields.put("min",    new FunctionValue(a -> new FunctionValue(b ->
                toDouble(a) <= toDouble(b) ? a : b)));
        fields.put("pow",    new FunctionValue(base -> new FunctionValue(exp ->
                new FloatValue(Math.pow(toDouble(base), toDouble(exp))))));
        fields.put("random", new FunctionValue(u -> new FloatValue(Math.random())));
        return new EntityValue(fields);
    }

    private static double toDouble(Value v) {
        if (v instanceof IntValue iv)   return (double) iv.value();
        if (v instanceof FloatValue fv) return fv.value();
        throw new RuntimeException("Expected numeric value, got: " + v);
    }
}
