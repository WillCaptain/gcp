package org.twelve.gcp.common;

import org.twelve.gcp.ast.AST;
import org.twelve.gcp.outline.adt.Array;
import org.twelve.gcp.outline.adt.Entity;
import org.twelve.gcp.outline.projectable.FirstOrderFunction;
import org.twelve.gcp.outline.projectable.Return;
import org.twelve.gcp.outline.projectable.Returnable;
import org.twelve.gcp.outlineenv.LocalSymbolEnvironment;

/**
 * Standard-library built-in entity modules registered into every AST's symbol environment.
 *
 * <p>Three singleton module entities are pre-defined and accessible by name without any import:
 * <ul>
 *   <li><b>date</b>    – date/time factory and inspection</li>
 *   <li><b>console</b> – I/O utilities</li>
 *   <li><b>math</b>    – numeric/mathematical constants and functions</li>
 * </ul>
 *
 * <p>The corresponding runtime values are registered by
 * {@link org.twelve.gcp.interpreter.stdlib.StdLibRuntime}.
 */
public final class StdLib {

    private StdLib() {}

    /** Register all stdlib modules into the root scope of the given environment. */
    public static void registerAll(AST ast, LocalSymbolEnvironment env) {
        env.defineSymbol("date",    buildDate(ast),    false, null);
        env.defineSymbol("console", buildConsole(ast), false, null);
        env.defineSymbol("math",    buildMath(ast),    false, null);
        env.defineSymbol("io",      buildIo(ast),      false, null);
        env.defineSymbol("json",    buildJson(ast),    false, null);
        env.defineSymbol("http",    buildHttp(ast),    false, null);
        env.defineSymbol("string",  buildString(ast),  false, null);
        registerGlobalFunctions(ast, env);
    }

    // ── IO ────────────────────────────────────────────────────────────────────

    /**
     * IO module entity (per-run in-memory sandbox):
     *   read(path: String)                   : String → String
     *   write(path: String)(content: String) : String → String → Unit  (curried)
     *   append(path: String)(content: String): String → String → Unit  (curried)
     *   exists(path: String)                 : String → Bool
     *   delete(path: String)                 : String → Unit
     *   list(prefix: String)                 : String → [String]
     */
    private static Entity buildIo(AST ast) {
        Entity io = Entity.from(ast.program());
        io.addMember("read",   FirstOrderFunction.from(ast, ast.String,                       ast.String),              Modifier.PUBLIC, false, null, false);
        io.addMember("write",  FirstOrderFunction.from(ast, ast.Unit,                         ast.String, ast.String),  Modifier.PUBLIC, false, null, false);
        io.addMember("append", FirstOrderFunction.from(ast, ast.Unit,                         ast.String, ast.String),  Modifier.PUBLIC, false, null, false);
        io.addMember("exists", FirstOrderFunction.from(ast, ast.Boolean,                      ast.String),              Modifier.PUBLIC, false, null, false);
        io.addMember("delete", FirstOrderFunction.from(ast, ast.Unit,                         ast.String),              Modifier.PUBLIC, false, null, false);
        io.addMember("list",   FirstOrderFunction.from(ast, Array.from(ast, ast.String),      ast.String),              Modifier.PUBLIC, false, null, false);
        return io;
    }

    // ── String ────────────────────────────────────────────────────────────────

    /**
     * String module entity:
     *   trim(s)                  : String → String
     *   upper(s)                 : String → String
     *   lower(s)                 : String → String
     *   contains(s)(sub)         : String → String → Bool
     *   starts_with(s)(prefix)   : String → String → Bool
     *   ends_with(s)(suffix)     : String → String → Bool
     *   index_of(s)(sub)         : String → String → Int
     *   split(s)(delim)          : String → String → [String]
     *   join(arr)(delim)         : Any    → String → String
     *   replace(s)(old)(new)     : String → String → String → String
     *   slice(s)(from)(to)       : String → Int → Int → String
     *   pad_left(s)(width)(pad)  : String → Int → String → String
     *   pad_right(s)(width)(pad) : String → Int → String → String
     *   repeat(s)(n)             : String → Int → String
     */
    private static Entity buildString(AST ast) {
        Array stringArray = Array.from(ast, ast.String);
        Entity s = Entity.from(ast.program());
        s.addMember("trim",        FirstOrderFunction.from(ast, ast.String,  ast.String),                            Modifier.PUBLIC, false, null, false);
        s.addMember("upper",       FirstOrderFunction.from(ast, ast.String,  ast.String),                            Modifier.PUBLIC, false, null, false);
        s.addMember("lower",       FirstOrderFunction.from(ast, ast.String,  ast.String),                            Modifier.PUBLIC, false, null, false);
        s.addMember("contains",    FirstOrderFunction.from(ast, ast.Boolean, ast.String,  ast.String),               Modifier.PUBLIC, false, null, false);
        s.addMember("starts_with", FirstOrderFunction.from(ast, ast.Boolean, ast.String,  ast.String),               Modifier.PUBLIC, false, null, false);
        s.addMember("ends_with",   FirstOrderFunction.from(ast, ast.Boolean, ast.String,  ast.String),               Modifier.PUBLIC, false, null, false);
        s.addMember("index_of",    FirstOrderFunction.from(ast, ast.Integer, ast.String,  ast.String),               Modifier.PUBLIC, false, null, false);
        s.addMember("split",       FirstOrderFunction.from(ast, stringArray, ast.String,  ast.String),               Modifier.PUBLIC, false, null, false);
        s.addMember("join",        FirstOrderFunction.from(ast, ast.String,  ast.Any,     ast.String),               Modifier.PUBLIC, false, null, false);
        s.addMember("replace",     FirstOrderFunction.from(ast, ast.String,  ast.String,  ast.String, ast.String),   Modifier.PUBLIC, false, null, false);
        s.addMember("slice",       FirstOrderFunction.from(ast, ast.String,  ast.String,  ast.Integer, ast.Integer), Modifier.PUBLIC, false, null, false);
        s.addMember("pad_left",    FirstOrderFunction.from(ast, ast.String,  ast.String,  ast.Integer, ast.String),  Modifier.PUBLIC, false, null, false);
        s.addMember("pad_right",   FirstOrderFunction.from(ast, ast.String,  ast.String,  ast.Integer, ast.String),  Modifier.PUBLIC, false, null, false);
        s.addMember("repeat",      FirstOrderFunction.from(ast, ast.String,  ast.String,  ast.Integer),              Modifier.PUBLIC, false, null, false);
        return s;
    }

    // ── JSON ──────────────────────────────────────────────────────────────────

    /**
     * JSON module entity:
     *   stringify(value: Any)  : Any    → String
     *   parse(text: String)    : String → Any
     */
    private static Entity buildJson(AST ast) {
        Entity json = Entity.from(ast.program());
        json.addMember("stringify", FirstOrderFunction.from(ast, ast.String, ast.Any),    Modifier.PUBLIC, false, null, false);
        json.addMember("parse",     FirstOrderFunction.from(ast, ast.Any,    ast.String), Modifier.PUBLIC, false, null, false);
        return json;
    }

    // ── HTTP ──────────────────────────────────────────────────────────────────

    /**
     * HttpResponse entity type returned by all http.* methods:
     *   status  : Int            — HTTP status code (e.g. 200, 404)
     *   ok      : Bool           — true when 200 ≤ status < 300
     *   body    : String         — raw response body text
     *   json()  : Unit → Any     — parse body as JSON
     *   text()  : Unit → String  — body as a no-arg function
     */
    static Entity buildHttpResponseType(AST ast) {
        Entity r = Entity.from(ast.program());
        r.addMember("status", ast.Integer,                                                   Modifier.PUBLIC, false, null, false);
        r.addMember("ok",     ast.Boolean,                                                   Modifier.PUBLIC, false, null, false);
        r.addMember("body",   ast.String,                                                    Modifier.PUBLIC, false, null, false);
        // json() returns Any: pre-seed supposed=ANY so that zero-arg calls via supposedToBe()
        // return ANY (assignable) rather than NOTHING (not assignable).
        r.addMember("json",   FirstOrderFunction.from(ast, dynamicAny(ast), ast.Unit),       Modifier.PUBLIC, false, null, false);
        r.addMember("text",   FirstOrderFunction.from(ast, ast.String,      ast.Unit),       Modifier.PUBLIC, false, null, false);
        return r;
    }

    /**
     * Creates a {@link Returnable} whose {@code supposed} field is pre-seeded to {@code ANY},
     * so that zero-argument calls on built-in functions (e.g. {@code resp.json()}) return
     * {@code ANY} instead of {@code NOTHING} via {@code supposedToBe()}.
     *
     * <p>Background: {@link Return#supposedToBe()} returns {@code supposed} directly when
     * {@code declaredToBe == ANY}.  Because {@link FirstOrderFunction#from} always calls
     * {@code addReturn(Nothing)} on the newly created Returnable, {@code supposed} would
     * normally be set to {@code NOTHING}, which is not assignable.  By calling
     * {@code addReturn(Any)} first we lock {@code supposed = ANY}; the subsequent
     * {@code addReturn(Nothing)} becomes a no-op (only UNKNOWN/NOTHING triggers an update).
     */
    private static Returnable dynamicAny(AST ast) {
        Returnable ret = Return.from(ast, ast.Any);
        ret.addReturn(ast.Any);   // supposed ← ANY; addReturn(Nothing) below won't overwrite
        return ret;
    }

    /**
     * HTTP module entity:
     *   get(url: String)                          : String → HttpResponse
     *   post(url: String)(body: String)           : String → String → HttpResponse  (curried)
     *   post_json(url: String)(body: String)      : String → String → HttpResponse  (curried, sets Accept: application/json)
     *   put(url: String)(body: String)            : String → String → HttpResponse  (curried)
     *   delete(url: String)                       : String → HttpResponse
     *   postForm(url: String)(body: String)       : String → String → HttpResponse  (curried)
     */
    private static Entity buildHttp(AST ast) {
        Entity resp = buildHttpResponseType(ast);
        Entity http = Entity.from(ast.program());
        http.addMember("get",       FirstOrderFunction.from(ast, resp, ast.String),              Modifier.PUBLIC, false, null, false);
        http.addMember("post",      FirstOrderFunction.from(ast, resp, ast.String, ast.String),  Modifier.PUBLIC, false, null, false);
        http.addMember("post_json", FirstOrderFunction.from(ast, resp, ast.String, ast.String),  Modifier.PUBLIC, false, null, false);
        http.addMember("put",       FirstOrderFunction.from(ast, resp, ast.String, ast.String),  Modifier.PUBLIC, false, null, false);
        http.addMember("delete",    FirstOrderFunction.from(ast, resp, ast.String),              Modifier.PUBLIC, false, null, false);
        http.addMember("postForm",  FirstOrderFunction.from(ast, resp, ast.String, ast.String),  Modifier.PUBLIC, false, null, false);
        return http;
    }

    // ── Global built-in functions ─────────────────────────────────────────────

    /**
     * Global first-class functions available in every module without qualification:
     * <pre>
     *   print(x: Any)         → Unit
     *   to_str(x: Any)        → String
     *   to_int(x: Any)        → Int
     *   to_float(x: Any)      → Float
     *   to_number(x: Any)     → Number
     *   len(x: Any)           → Int
     *   assert(cond: Bool)    → Unit
     * </pre>
     */
    private static void registerGlobalFunctions(AST ast, LocalSymbolEnvironment env) {
        env.defineSymbol("print",     FirstOrderFunction.from(ast, ast.Unit,    ast.Any),     false, null);
        env.defineSymbol("to_str",    FirstOrderFunction.from(ast, ast.String,  ast.Any),     false, null);
        env.defineSymbol("to_int",    FirstOrderFunction.from(ast, ast.Integer, ast.Any),     false, null);
        env.defineSymbol("to_float",  FirstOrderFunction.from(ast, ast.Float,   ast.Any),     false, null);
        env.defineSymbol("to_number", FirstOrderFunction.from(ast, ast.Number,  ast.Any),     false, null);
        env.defineSymbol("len",       FirstOrderFunction.from(ast, ast.Integer, ast.Any),     false, null);
        env.defineSymbol("assert",    FirstOrderFunction.from(ast, ast.Unit,    ast.Boolean), false, null);
        env.defineSymbol("type_of",   FirstOrderFunction.from(ast, ast.String,  ast.Any),     false, null);
    }

    // ── Date ──────────────────────────────────────────────────────────────────

    /**
     * DateRecord: the shape returned by Date.now() and Date.parse().
     * Fields: year, month, day, hour, minute, second (all Int)
     * Methods: format(String)→String, timestamp()→Long, day_of_week()→Int, to_str()→String
     */
    static Entity buildDateRecord(AST ast) {
        Entity r = Entity.from(ast.program());
        r.addMember("year",        ast.Integer,                                                     Modifier.PUBLIC, false, null, false);
        r.addMember("month",       ast.Integer,                                                     Modifier.PUBLIC, false, null, false);
        r.addMember("day",         ast.Integer,                                                     Modifier.PUBLIC, false, null, false);
        r.addMember("hour",        ast.Integer,                                                     Modifier.PUBLIC, false, null, false);
        r.addMember("minute",      ast.Integer,                                                     Modifier.PUBLIC, false, null, false);
        r.addMember("second",      ast.Integer,                                                     Modifier.PUBLIC, false, null, false);
        r.addMember("format",      FirstOrderFunction.from(ast, ast.String,  ast.String),           Modifier.PUBLIC, false, null, false);
        r.addMember("timestamp",   FirstOrderFunction.from(ast, ast.Long,    ast.Unit),             Modifier.PUBLIC, false, null, false);
        r.addMember("day_of_week", FirstOrderFunction.from(ast, ast.Integer, ast.Unit),             Modifier.PUBLIC, false, null, false);
        r.addMember("to_str",      FirstOrderFunction.from(ast, ast.String,  ast.Unit),             Modifier.PUBLIC, false, null, false);
        return r;
    }

    /**
     * Date module entity:
     *   now()          : Unit   → DateRecord
     *   parse(String)  : String → DateRecord
     */
    private static Entity buildDate(AST ast) {
        Entity dateRecord = buildDateRecord(ast);
        Entity d = Entity.from(ast.program());
        d.addMember("now",   FirstOrderFunction.from(ast, dateRecord, ast.Unit),   Modifier.PUBLIC, false, null, false);
        d.addMember("parse", FirstOrderFunction.from(ast, dateRecord, ast.String), Modifier.PUBLIC, false, null, false);
        return d;
    }

    // ── Console ───────────────────────────────────────────────────────────────

    /**
     * Console module entity:
     *   log(Any)    : Any → Unit   — accepts any type; runtime calls display()/to_str implicitly
     *   warn(Any)   : Any → Unit
     *   error(Any)  : Any → Unit
     *   read()      : Unit → String
     */
    private static Entity buildConsole(AST ast) {
        Entity c = Entity.from(ast.program());
        c.addMember("log",   FirstOrderFunction.from(ast, ast.Unit,   ast.Any),    Modifier.PUBLIC, false, null, false);
        c.addMember("warn",  FirstOrderFunction.from(ast, ast.Unit,   ast.Any),    Modifier.PUBLIC, false, null, false);
        c.addMember("error", FirstOrderFunction.from(ast, ast.Unit,   ast.Any),    Modifier.PUBLIC, false, null, false);
        c.addMember("read",  FirstOrderFunction.from(ast, ast.String, ast.Unit),   Modifier.PUBLIC, false, null, false);
        return c;
    }

    // ── Math ──────────────────────────────────────────────────────────────────

    /**
     * Math module entity:
     *   pi            : Double
     *   e             : Double
     *   sqrt(Number)  : Number → Double
     *   abs(Number)   : Number → Number
     *   floor(Number) : Number → Int
     *   ceil(Number)  : Number → Int
     *   round(Number) : Number → Int
     *   max(a)(b)     : Number → Number → Number  (curried)
     *   min(a)(b)     : Number → Number → Number  (curried)
     *   pow(base)(exp): Number → Number → Double  (curried)
     *   random()      : Unit   → Double
     */
    private static Entity buildMath(AST ast) {
        Entity m = Entity.from(ast.program());
        // Constants: Double (Java's Math.PI / Math.E are doubles)
        m.addMember("pi",     ast.Double,                                                              Modifier.PUBLIC, false, null, false);
        m.addMember("e",      ast.Double,                                                              Modifier.PUBLIC, false, null, false);
        // Functions: accept Number (broadest numeric type), return Double
        m.addMember("sqrt",   FirstOrderFunction.from(ast, ast.Double,  ast.Number),                  Modifier.PUBLIC, false, null, false);
        m.addMember("abs",    FirstOrderFunction.from(ast, ast.Number,  ast.Number),                  Modifier.PUBLIC, false, null, false);
        m.addMember("floor",  FirstOrderFunction.from(ast, ast.Integer, ast.Number),                  Modifier.PUBLIC, false, null, false);
        m.addMember("ceil",   FirstOrderFunction.from(ast, ast.Integer, ast.Number),                  Modifier.PUBLIC, false, null, false);
        m.addMember("round",  FirstOrderFunction.from(ast, ast.Integer, ast.Number),                  Modifier.PUBLIC, false, null, false);
        m.addMember("max",    FirstOrderFunction.from(ast, ast.Number,  ast.Number, ast.Number),      Modifier.PUBLIC, false, null, false);
        m.addMember("min",    FirstOrderFunction.from(ast, ast.Number,  ast.Number, ast.Number),      Modifier.PUBLIC, false, null, false);
        m.addMember("pow",    FirstOrderFunction.from(ast, ast.Double,  ast.Number, ast.Number),      Modifier.PUBLIC, false, null, false);
        m.addMember("random", FirstOrderFunction.from(ast, ast.Double,  ast.Unit),                    Modifier.PUBLIC, false, null, false);
        return m;
    }
}
