package org.twelve.gcp.common;

import org.twelve.gcp.ast.AST;
import org.twelve.gcp.outline.adt.Entity;
import org.twelve.gcp.outline.projectable.FirstOrderFunction;
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
        registerGlobalFunctions(ast, env);
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
