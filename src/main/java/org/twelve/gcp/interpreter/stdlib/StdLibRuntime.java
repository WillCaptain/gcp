package org.twelve.gcp.interpreter.stdlib;

import org.twelve.gcp.common.StdLib;
import org.twelve.gcp.interpreter.Environment;
import org.twelve.gcp.interpreter.value.*;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
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
        env.define("string",  buildString());
    }

    // ── IO in-memory sandbox ──────────────────────────────────────────────────

    private static final java.util.concurrent.ConcurrentHashMap<String, String> IO_FS =
            new java.util.concurrent.ConcurrentHashMap<>();

    /**
     * Reset the per-run in-memory sandbox filesystem.
     * Called by the playground before each code execution.
     */
    public static void resetFs() {
        IO_FS.clear();
    }

    // ── Date ──────────────────────────────────────────────────────────────────

    private static EntityValue buildDateRecord(LocalDateTime dt) {
        LinkedHashMap<String, Value> fields = new LinkedHashMap<>();
        fields.put("year",   IntValue.of(dt.getYear()));
        fields.put("month",  IntValue.of(dt.getMonthValue()));
        fields.put("day",    IntValue.of(dt.getDayOfMonth()));
        fields.put("hour",   IntValue.of(dt.getHour()));
        fields.put("minute", IntValue.of(dt.getMinute()));
        fields.put("second", IntValue.of(dt.getSecond()));
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
            if (v instanceof IntValue iv) return IntValue.of(Math.abs(iv.value()));
            return new FloatValue(Math.abs(toDouble(v)));
        }));
        fields.put("floor",  new FunctionValue(v -> IntValue.of((long) Math.floor(toDouble(v)))));
        fields.put("ceil",   new FunctionValue(v -> IntValue.of((long) Math.ceil(toDouble(v)))));
        fields.put("round",  new FunctionValue(v -> IntValue.of(Math.round(toDouble(v)))));
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
            String content = IO_FS.get(((StringValue) v).value());
            return new StringValue(content != null ? content : "");
        }));

        fields.put("write", new FunctionValue(pathV -> new FunctionValue(contentV -> {
            IO_FS.put(((StringValue) pathV).value(), ((StringValue) contentV).value());
            return UnitValue.INSTANCE;
        })));

        fields.put("append", new FunctionValue(pathV -> new FunctionValue(contentV -> {
            String path    = ((StringValue) pathV).value();
            String extra   = ((StringValue) contentV).value();
            IO_FS.merge(path, extra, String::concat);
            return UnitValue.INSTANCE;
        })));

        fields.put("exists", new FunctionValue(v ->
                BoolValue.of(IO_FS.containsKey(((StringValue) v).value()))));

        fields.put("delete", new FunctionValue(v -> {
            IO_FS.remove(((StringValue) v).value());
            return UnitValue.INSTANCE;
        }));

        fields.put("list", new FunctionValue(prefixV -> {
            String prefix = ((StringValue) prefixV).value();
            List<Value> paths = IO_FS.keySet().stream()
                    .filter(k -> k.startsWith(prefix))
                    .sorted()
                    .map(StringValue::new)
                    .collect(java.util.stream.Collectors.toList());
            return new ArrayValue(paths);
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

    // ── JSON parser ───────────────────────────────────────────────────────────

    static Value jsonToValue(String json) {
        if (json == null || json.isEmpty()) return UnitValue.INSTANCE;
        return new JsonParser(json).parse();
    }

    /**
     * Minimal recursive-descent JSON parser.
     * Produces: StringValue, IntValue, FloatValue, BoolValue, UnitValue,
     *           ArrayValue, EntityValue.
     */
    private static final class JsonParser {
        private final String src;
        private int pos;

        JsonParser(String src) { this.src = src; this.pos = 0; }

        Value parse() {
            skipWs();
            if (pos >= src.length()) return UnitValue.INSTANCE;
            char ch = src.charAt(pos);
            if (ch == '"')  return parseString();
            if (ch == '[')  return parseArray();
            if (ch == '{')  return parseObject();
            if (ch == 't')  { pos += 4; return BoolValue.TRUE;  }
            if (ch == 'f')  { pos += 5; return BoolValue.FALSE; }
            if (ch == 'n')  { pos += 4; return UnitValue.INSTANCE; }
            return parseNumber();
        }

        private void skipWs() {
            while (pos < src.length() && Character.isWhitespace(src.charAt(pos))) pos++;
        }

        private Value parseString() {
            pos++; // skip opening "
            var sb = new StringBuilder();
            while (pos < src.length()) {
                char c = src.charAt(pos++);
                if (c == '"') break;
                if (c == '\\' && pos < src.length()) {
                    char esc = src.charAt(pos++);
                    switch (esc) {
                        case '"'  -> sb.append('"');
                        case '\\' -> sb.append('\\');
                        case '/'  -> sb.append('/');
                        case 'n'  -> sb.append('\n');
                        case 'r'  -> sb.append('\r');
                        case 't'  -> sb.append('\t');
                        case 'b'  -> sb.append('\b');
                        case 'f'  -> sb.append('\f');
                        case 'u'  -> {
                            if (pos + 4 <= src.length()) {
                                sb.append((char) Integer.parseInt(src.substring(pos, pos + 4), 16));
                                pos += 4;
                            }
                        }
                        default   -> { sb.append('\\'); sb.append(esc); }
                    }
                } else {
                    sb.append(c);
                }
            }
            return new StringValue(sb.toString());
        }

        private Value parseArray() {
            pos++; // skip '['
            var elems = new java.util.ArrayList<Value>();
            skipWs();
            if (pos < src.length() && src.charAt(pos) == ']') { pos++; return new ArrayValue(elems); }
            while (pos < src.length()) {
                skipWs();
                elems.add(parse());
                skipWs();
                if (pos < src.length() && src.charAt(pos) == ',') { pos++; continue; }
                if (pos < src.length() && src.charAt(pos) == ']') { pos++; break; }
                break;
            }
            return new ArrayValue(elems);
        }

        private Value parseObject() {
            pos++; // skip '{'
            var fields = new LinkedHashMap<String, Value>();
            skipWs();
            if (pos < src.length() && src.charAt(pos) == '}') { pos++; return new EntityValue(fields); }
            while (pos < src.length()) {
                skipWs();
                if (pos >= src.length() || src.charAt(pos) != '"') break;
                String key = ((StringValue) parseString()).value();
                skipWs();
                if (pos < src.length() && src.charAt(pos) == ':') pos++;
                skipWs();
                Value val = parse();
                fields.put(key, val);
                skipWs();
                if (pos < src.length() && src.charAt(pos) == ',') { pos++; continue; }
                if (pos < src.length() && src.charAt(pos) == '}') { pos++; break; }
                break;
            }
            return new EntityValue(fields);
        }

        private Value parseNumber() {
            int start = pos;
            boolean isFloat = false;
            if (pos < src.length() && src.charAt(pos) == '-') pos++;
            while (pos < src.length() && (Character.isDigit(src.charAt(pos)))) pos++;
            if (pos < src.length() && src.charAt(pos) == '.') { isFloat = true; pos++; }
            while (pos < src.length() && Character.isDigit(src.charAt(pos))) pos++;
            if (pos < src.length() && (src.charAt(pos) == 'e' || src.charAt(pos) == 'E')) {
                isFloat = true; pos++;
                if (pos < src.length() && (src.charAt(pos) == '+' || src.charAt(pos) == '-')) pos++;
                while (pos < src.length() && Character.isDigit(src.charAt(pos))) pos++;
            }
            String num = src.substring(start, pos);
            if (isFloat) {
                try { return new FloatValue(Double.parseDouble(num)); } catch (NumberFormatException ignored) {}
            } else {
                try { return new IntValue(Long.parseLong(num)); } catch (NumberFormatException ignored) {}
                try { return new FloatValue(Double.parseDouble(num)); } catch (NumberFormatException ignored) {}
            }
            return new StringValue(num);
        }
    }

    // ── String ────────────────────────────────────────────────────────────────

    private static EntityValue buildString() {
        LinkedHashMap<String, Value> fields = new LinkedHashMap<>();

        fields.put("trim",        new FunctionValue(v -> new StringValue(str(v).trim())));
        fields.put("upper",       new FunctionValue(v -> new StringValue(str(v).toUpperCase())));
        fields.put("lower",       new FunctionValue(v -> new StringValue(str(v).toLowerCase())));

        fields.put("contains",    new FunctionValue(sv -> new FunctionValue(sub ->
                BoolValue.of(str(sv).contains(str(sub))))));

        fields.put("starts_with", new FunctionValue(sv -> new FunctionValue(prefix ->
                BoolValue.of(str(sv).startsWith(str(prefix))))));

        fields.put("ends_with",   new FunctionValue(sv -> new FunctionValue(suffix ->
                BoolValue.of(str(sv).endsWith(str(suffix))))));

        fields.put("index_of",    new FunctionValue(sv -> new FunctionValue(sub ->
                IntValue.of(str(sv).indexOf(str(sub))))));

        fields.put("split", new FunctionValue(sv -> new FunctionValue(delimV -> {
            String[] parts = str(sv).split(java.util.regex.Pattern.quote(str(delimV)), -1);
            List<Value> elems = new java.util.ArrayList<>();
            for (String p : parts) elems.add(new StringValue(p));
            return new ArrayValue(elems);
        })));

        fields.put("join", new FunctionValue(arrV -> new FunctionValue(delimV -> {
            String delim = str(delimV);
            if (arrV instanceof ArrayValue av) {
                StringBuilder sb = new StringBuilder();
                List<Value> elems = av.elements();
                for (int i = 0; i < elems.size(); i++) {
                    if (i > 0) sb.append(delim);
                    sb.append(str(elems.get(i)));
                }
                return new StringValue(sb.toString());
            }
            return new StringValue(str(arrV));
        })));

        fields.put("replace", new FunctionValue(sv -> new FunctionValue(oldV -> new FunctionValue(newV ->
                new StringValue(str(sv).replace(str(oldV), str(newV)))))));

        fields.put("slice", new FunctionValue(sv -> new FunctionValue(fromV -> new FunctionValue(toV -> {
            String s   = str(sv);
            int    from = (int) ((IntValue) fromV).value();
            int    to   = (int) ((IntValue) toV).value();
            from = Math.max(0, Math.min(from, s.length()));
            to   = Math.max(from, Math.min(to, s.length()));
            return new StringValue(s.substring(from, to));
        }))));

        fields.put("pad_left", new FunctionValue(sv -> new FunctionValue(widthV -> new FunctionValue(padV -> {
            String s     = str(sv);
            int    width = (int) ((IntValue) widthV).value();
            String pad   = str(padV);
            if (pad.isEmpty()) pad = " ";
            StringBuilder sb = new StringBuilder(s);
            while (sb.length() < width) sb.insert(0, pad);
            return new StringValue(sb.length() > width ? sb.substring(sb.length() - width) : sb.toString());
        }))));

        fields.put("pad_right", new FunctionValue(sv -> new FunctionValue(widthV -> new FunctionValue(padV -> {
            String s     = str(sv);
            int    width = (int) ((IntValue) widthV).value();
            String pad   = str(padV);
            if (pad.isEmpty()) pad = " ";
            StringBuilder sb = new StringBuilder(s);
            while (sb.length() < width) sb.append(pad);
            return new StringValue(sb.length() > width ? sb.substring(0, width) : sb.toString());
        }))));

        fields.put("repeat", new FunctionValue(sv -> new FunctionValue(nV -> {
            int n = (int) ((IntValue) nV).value();
            return new StringValue(str(sv).repeat(Math.max(0, n)));
        })));

        return new EntityValue(fields);
    }

    private static String str(Value v) {
        if (v instanceof StringValue sv) return sv.value();
        return v.display();
    }

    // ── HTTP ──────────────────────────────────────────────────────────────────

    private static final HttpClient HTTP_CLIENT = HttpClient.newHttpClient();

    /** Wrap a raw Java HttpResponse into an Outline EntityValue with status/ok/body/json()/text(). */
    private static EntityValue wrapHttpResponse(HttpResponse<String> raw) {
        int statusCode = raw.statusCode();
        String body     = raw.body() != null ? raw.body() : "";
        LinkedHashMap<String, Value> fields = new LinkedHashMap<>();
        fields.put("status", IntValue.of(statusCode));
        fields.put("ok",     BoolValue.of(statusCode >= 200 && statusCode < 300));
        fields.put("body",   new StringValue(body));
        fields.put("json",   new FunctionValue(u -> jsonToValue(body.trim())));
        fields.put("text",   new FunctionValue(u -> new StringValue(body)));
        return new EntityValue(fields);
    }

    /** Return an error-response entity (status=0, ok=false, empty body). */
    private static EntityValue errorHttpResponse(Exception e) {
        LinkedHashMap<String, Value> fields = new LinkedHashMap<>();
        fields.put("status", IntValue.of(0));
        fields.put("ok",     BoolValue.FALSE);
        fields.put("body",   new StringValue(""));
        fields.put("json",   new FunctionValue(u -> UnitValue.INSTANCE));
        fields.put("text",   new FunctionValue(u -> new StringValue("")));
        return new EntityValue(fields);
    }

    private static EntityValue buildHttp() {
        LinkedHashMap<String, Value> fields = new LinkedHashMap<>();

        fields.put("get", new FunctionValue(urlV -> {
            try {
                HttpRequest req = HttpRequest.newBuilder(URI.create(((StringValue) urlV).value())).GET().build();
                return wrapHttpResponse(HTTP_CLIENT.send(req, HttpResponse.BodyHandlers.ofString()));
            } catch (Exception e) {
                return errorHttpResponse(e);
            }
        }));

        fields.put("post", new FunctionValue(urlV -> new FunctionValue(bodyV -> {
            try {
                HttpRequest req = HttpRequest.newBuilder(URI.create(((StringValue) urlV).value()))
                        .POST(HttpRequest.BodyPublishers.ofString(((StringValue) bodyV).value()))
                        .header("Content-Type", "application/json")
                        .build();
                return wrapHttpResponse(HTTP_CLIENT.send(req, HttpResponse.BodyHandlers.ofString()));
            } catch (Exception e) {
                return errorHttpResponse(e);
            }
        })));

        fields.put("post_json", new FunctionValue(urlV -> new FunctionValue(bodyV -> {
            try {
                HttpRequest req = HttpRequest.newBuilder(URI.create(((StringValue) urlV).value()))
                        .POST(HttpRequest.BodyPublishers.ofString(((StringValue) bodyV).value()))
                        .header("Content-Type", "application/json")
                        .header("Accept", "application/json")
                        .build();
                return wrapHttpResponse(HTTP_CLIENT.send(req, HttpResponse.BodyHandlers.ofString()));
            } catch (Exception e) {
                return errorHttpResponse(e);
            }
        })));

        fields.put("put", new FunctionValue(urlV -> new FunctionValue(bodyV -> {
            try {
                HttpRequest req = HttpRequest.newBuilder(URI.create(((StringValue) urlV).value()))
                        .PUT(HttpRequest.BodyPublishers.ofString(((StringValue) bodyV).value()))
                        .header("Content-Type", "application/json")
                        .build();
                return wrapHttpResponse(HTTP_CLIENT.send(req, HttpResponse.BodyHandlers.ofString()));
            } catch (Exception e) {
                return errorHttpResponse(e);
            }
        })));

        fields.put("delete", new FunctionValue(urlV -> {
            try {
                HttpRequest req = HttpRequest.newBuilder(URI.create(((StringValue) urlV).value()))
                        .DELETE()
                        .build();
                return wrapHttpResponse(HTTP_CLIENT.send(req, HttpResponse.BodyHandlers.ofString()));
            } catch (Exception e) {
                return errorHttpResponse(e);
            }
        }));

        fields.put("postForm", new FunctionValue(urlV -> new FunctionValue(bodyV -> {
            try {
                HttpRequest req = HttpRequest.newBuilder(URI.create(((StringValue) urlV).value()))
                        .POST(HttpRequest.BodyPublishers.ofString(((StringValue) bodyV).value()))
                        .header("Content-Type", "application/x-www-form-urlencoded")
                        .build();
                return wrapHttpResponse(HTTP_CLIENT.send(req, HttpResponse.BodyHandlers.ofString()));
            } catch (Exception e) {
                return errorHttpResponse(e);
            }
        })));

        return new EntityValue(fields);
    }
}
