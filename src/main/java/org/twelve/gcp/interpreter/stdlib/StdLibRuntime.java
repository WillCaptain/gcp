package org.twelve.gcp.interpreter.stdlib;

import org.twelve.gcp.common.StdLib;
import org.twelve.gcp.interpreter.Environment;
import org.twelve.gcp.interpreter.value.*;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;

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
        env.define("date",    buildDate());
        env.define("console", buildConsole());
        env.define("math",    buildMath());
        env.define("io",      buildIo());
        env.define("json",    buildJson());
        env.define("http",    buildHttp());
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

    // ── IO ────────────────────────────────────────────────────────────────────

    private static EntityValue buildIo() {
        LinkedHashMap<String, Value> fields = new LinkedHashMap<>();
        fields.put("read", new FunctionValue(v -> {
            try {
                return new StringValue(Files.readString(Path.of(((StringValue) v).value())));
            } catch (Exception e) {
                return new StringValue("");
            }
        }));
        fields.put("write", new FunctionValue(pathV -> new FunctionValue(contentV -> {
            try {
                Files.writeString(Path.of(((StringValue) pathV).value()), ((StringValue) contentV).value());
            } catch (Exception ignored) {}
            return UnitValue.INSTANCE;
        })));
        fields.put("exists", new FunctionValue(v ->
                BoolValue.of(Files.exists(Path.of(((StringValue) v).value())))));
        fields.put("delete", new FunctionValue(v -> {
            try {
                return BoolValue.of(Files.deleteIfExists(Path.of(((StringValue) v).value())));
            } catch (Exception e) {
                return BoolValue.FALSE;
            }
        }));
        return new EntityValue(fields);
    }

    // ── JSON ──────────────────────────────────────────────────────────────────

    private static EntityValue buildJson() {
        LinkedHashMap<String, Value> fields = new LinkedHashMap<>();
        fields.put("stringify", new FunctionValue(v -> new StringValue(valueToJson(v))));
        fields.put("parse",     new FunctionValue(v -> jsonToValue(((StringValue) v).value().trim())));
        return new EntityValue(fields);
    }

    private static String valueToJson(Value v) {
        if (v instanceof StringValue sv)  return "\"" + sv.value().replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "\\r") + "\"";
        if (v instanceof IntValue iv)     return String.valueOf(iv.value());
        if (v instanceof FloatValue fv)   return String.valueOf(fv.value());
        if (v instanceof BoolValue bv)    return bv.value() ? "true" : "false";
        if (v instanceof UnitValue)       return "null";
        if (v instanceof ArrayValue av) {
            var sb = new StringBuilder("[");
            List<Value> elems = av.elements();
            for (int i = 0; i < elems.size(); i++) {
                if (i > 0) sb.append(",");
                sb.append(valueToJson(elems.get(i)));
            }
            return sb.append("]").toString();
        }
        if (v instanceof EntityValue ev) {
            var sb = new StringBuilder("{");
            boolean first = true;
            for (var entry : ev.ownFields().entrySet()) {
                if (!first) sb.append(",");
                sb.append("\"").append(entry.getKey()).append("\":").append(valueToJson(entry.getValue()));
                first = false;
            }
            return sb.append("}").toString();
        }
        return "\"" + v.display().replace("\"", "\\\"") + "\"";
    }

    private static Value jsonToValue(String json) {
        if (json.equals("null"))  return UnitValue.INSTANCE;
        if (json.equals("true"))  return BoolValue.TRUE;
        if (json.equals("false")) return BoolValue.FALSE;
        if (json.startsWith("\"") && json.endsWith("\""))
            return new StringValue(json.substring(1, json.length() - 1)
                    .replace("\\\"", "\"").replace("\\n", "\n").replace("\\\\", "\\"));
        try { return new IntValue(Long.parseLong(json)); } catch (NumberFormatException ignored) {}
        try { return new FloatValue(Double.parseDouble(json)); } catch (NumberFormatException ignored) {}
        return new StringValue(json);
    }

    // ── HTTP ──────────────────────────────────────────────────────────────────

    private static final HttpClient HTTP_CLIENT = HttpClient.newHttpClient();

    private static EntityValue buildHttp() {
        LinkedHashMap<String, Value> fields = new LinkedHashMap<>();
        fields.put("get", new FunctionValue(urlV -> {
            try {
                HttpRequest req = HttpRequest.newBuilder(URI.create(((StringValue) urlV).value())).GET().build();
                return new StringValue(HTTP_CLIENT.send(req, HttpResponse.BodyHandlers.ofString()).body());
            } catch (Exception e) {
                return new StringValue("");
            }
        }));
        fields.put("post", new FunctionValue(urlV -> new FunctionValue(bodyV -> {
            try {
                HttpRequest req = HttpRequest.newBuilder(URI.create(((StringValue) urlV).value()))
                        .POST(HttpRequest.BodyPublishers.ofString(((StringValue) bodyV).value()))
                        .header("Content-Type", "application/json")
                        .build();
                return new StringValue(HTTP_CLIENT.send(req, HttpResponse.BodyHandlers.ofString()).body());
            } catch (Exception e) {
                return new StringValue("");
            }
        })));
        fields.put("put", new FunctionValue(urlV -> new FunctionValue(bodyV -> {
            try {
                HttpRequest req = HttpRequest.newBuilder(URI.create(((StringValue) urlV).value()))
                        .PUT(HttpRequest.BodyPublishers.ofString(((StringValue) bodyV).value()))
                        .header("Content-Type", "application/json")
                        .build();
                return new StringValue(HTTP_CLIENT.send(req, HttpResponse.BodyHandlers.ofString()).body());
            } catch (Exception e) {
                return new StringValue("");
            }
        })));
        fields.put("delete", new FunctionValue(urlV -> {
            try {
                HttpRequest req = HttpRequest.newBuilder(URI.create(((StringValue) urlV).value()))
                        .DELETE()
                        .build();
                return new StringValue(HTTP_CLIENT.send(req, HttpResponse.BodyHandlers.ofString()).body());
            } catch (Exception e) {
                return new StringValue("");
            }
        }));
        return new EntityValue(fields);
    }
}
