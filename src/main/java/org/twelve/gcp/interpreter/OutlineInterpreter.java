package org.twelve.gcp.interpreter;

import org.twelve.gcp.ast.ASF;
import org.twelve.gcp.ast.AST;
import org.twelve.gcp.ast.Node;
import org.twelve.gcp.common.CONSTANTS;
import org.twelve.gcp.inference.OutlineInferencer;
import org.twelve.gcp.interpreter.interpretation.*;
import org.twelve.gcp.node.expression.OutlineDefinition;
import org.twelve.gcp.interpreter.value.*;
import org.twelve.gcp.node.expression.*;
import org.twelve.gcp.node.expression.accessor.ArrayAccessor;
import org.twelve.gcp.node.expression.accessor.MemberAccessor;
import org.twelve.gcp.node.expression.body.Block;
import org.twelve.gcp.node.expression.body.FunctionBody;
import org.twelve.gcp.node.expression.body.WithExpression;
import org.twelve.gcp.node.expression.conditions.*;
import org.twelve.gcp.node.expression.identifier.Identifier;
import org.twelve.gcp.node.expression.identifier.SymbolIdentifier;
import org.twelve.gcp.node.expression.referable.ReferenceCallNode;
import org.twelve.gcp.node.function.FunctionCallNode;
import org.twelve.gcp.node.function.FunctionNode;
import org.twelve.gcp.node.imexport.Export;
import org.twelve.gcp.node.imexport.Import;
import org.twelve.gcp.node.statement.*;
import org.twelve.gcp.node.expression.typeable.*;
import org.twelve.gcp.outline.Outline;
import org.twelve.gcp.outline.adt.Entity;
import org.twelve.gcp.outline.builtin.UNKNOWN;
import org.twelve.gcp.outline.primitive.*;
import org.twelve.gcp.outline.projectable.Function;
import org.twelve.gcp.outline.projectable.Genericable;

import org.twelve.gcp.config.GCPConfig;
import org.twelve.gcp.plugin.PluginLoader;

import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Concrete implementation of {@link Interpreter}: the GCP tree-walking interpreter.
 *
 * <h2>Design – Visitor Pattern</h2>
 * <p>Mirrors {@link OutlineInferencer}:
 * <ul>
 *   <li>Each {@code visit(NodeType)} method creates the corresponding
 *       {@link Interpretation} instance and delegates to it.</li>
 *   <li>Individual {@link Interpretation} classes are <em>stateless</em> – they
 *       receive {@code Interpreter} as a parameter and use {@link #eval},
 *       {@link #env}, {@link #setEnv} and {@link #apply} to recurse and
 *       manipulate the runtime environment.</li>
 *   <li>Nodes dispatch via {@code node.interpret(this)} →
 *       {@code node.acceptInterpret(this)} → {@code this.visit(nodeTyped)}.</li>
 * </ul>
 *
 * <h2>Shared State</h2>
 * <ul>
 *   <li>{@link Environment} – lexical scope chain.</li>
 *   <li>{@code moduleExports} – cross-module exported bindings.</li>
 *   <li>{@code constructors} – registered {@link SymbolConstructor} plugins.</li>
 *   <li>{@link GCPConfig} – runtime configuration (plugin dir, …).</li>
 * </ul>
 */
public class OutlineInterpreter implements Interpreter {

    // ── runtime configuration ─────────────────────────────────────────────────
    private final GCPConfig config;

    // ── external plugin registry ─────────────────────────────────────────────
    private final Map<String, SymbolConstructor> constructors = new LinkedHashMap<>();

    // ── cross-module exports (namespace -> (name -> value)) ──────────────────
    private final Map<String, Map<String, Value>> moduleExports = new LinkedHashMap<>();

    // ── type-definition registry (populated by outline declarations) ─────────
    private final Map<String, OutlineDefinition> typeDefinitions = new LinkedHashMap<>();

    // ── runtime environment ───────────────────────────────────────────────────
    private Environment env = new Environment(null);

    // ── optional bound ASF (for the single-arg constructor convenience API) ──
    private ASF boundAsf = null;

    // ── pre-built stateless interpretation delegates ──────────────────────────
    private final LiteralInterpretation          literalI          = new LiteralInterpretation();
    private final IdentifierInterpretation        identifierI       = new IdentifierInterpretation();
    private final SymbolIdentifierInterpretation  symbolIdI         = new SymbolIdentifierInterpretation();
    private final VariableInterpretation          variableI         = new VariableInterpretation();
    private final BinaryInterpretation            binaryI           = new BinaryInterpretation();
    private final UnaryInterpretation             unaryI            = new UnaryInterpretation();
    private final AssignmentInterpretation        assignmentI       = new AssignmentInterpretation();
    private final VariableDeclaratorInterpretation varDeclI         = new VariableDeclaratorInterpretation();
    private final EntityInterpretation            entityI           = new EntityInterpretation();
    private final TupleInterpretation             tupleI            = new TupleInterpretation();
    private final ArrayInterpretation             arrayI            = new ArrayInterpretation();
    private final DictInterpretation              dictI             = new DictInterpretation();
    private final FunctionInterpretation          functionI         = new FunctionInterpretation();
    private final FunctionCallInterpretation      functionCallI     = new FunctionCallInterpretation();
    private final MemberAccessorInterpretation    memberAccessorI   = new MemberAccessorInterpretation();
    private final ArrayAccessorInterpretation     arrayAccessorI    = new ArrayAccessorInterpretation();
    private final ReferenceCallInterpretation     refCallI          = new ReferenceCallInterpretation();
    private final SelectionsInterpretation        selectionsI       = new SelectionsInterpretation();
    private final MatchInterpretation             matchI            = new MatchInterpretation();
    private final ThisInterpretation              thisI             = new ThisInterpretation();
    private final BaseInterpretation              baseI             = new BaseInterpretation();
    private final IsAsInterpretation              isAsI             = new IsAsInterpretation();
    private final AsInterpretation                asI               = new AsInterpretation();
    private final PolyInterpretation              polyI             = new PolyInterpretation();
    private final BlockInterpretation             blockI            = new BlockInterpretation();
    private final FunctionBodyInterpretation      functionBodyI     = new FunctionBodyInterpretation();
    private final WithExpressionInterpretation    withExprI         = new WithExpressionInterpretation();
    private final ExpressionStatementInterpretation exprStmtI       = new ExpressionStatementInterpretation();
    private final ReturnInterpretation            returnI           = new ReturnInterpretation();
    private final MemberNodeInterpretation        memberNodeI       = new MemberNodeInterpretation();
    private final OutlineDeclaratorInterpretation outlineDeclI      = new OutlineDeclaratorInterpretation();
    private final ImportInterpretation            importI           = new ImportInterpretation();
    private final ExportInterpretation            exportI           = new ExportInterpretation();

    // =========================================================================
    // Public entry points
    // =========================================================================

    /**
     * Creates an interpreter using {@link GCPConfig#load()} — reads {@code gcp.properties}
     * bundled in the JAR, with system-property and environment-variable overrides.
     * Plugin JARs are auto-loaded from the configured {@code plugin_dir}.
     */
    public OutlineInterpreter() {
        this(GCPConfig.load());
    }

    /**
     * Creates an interpreter with the given {@link GCPConfig}.
     * Plugin JARs are automatically loaded from {@code config.getPath("plugin_dir")}
     * if that directory exists on the filesystem.
     */
    public OutlineInterpreter(GCPConfig config) {
        this.config = config;
        registerBuiltins();
        PluginLoader.load(config).forEach(p -> constructors.put(p.id(), p::construct));
    }

    /**
     * Convenience constructor: binds an {@link ASF} so that {@link #run()} (no-arg)
     * can be called directly.  Mirrors the previous API used by existing tests.
     */
    public OutlineInterpreter(ASF asf) {
        this();
        this.boundAsf = asf;
    }

    /**
     * Runs the bound {@link ASF} (set via {@link #OutlineInterpreter(ASF)}).
     * Equivalent to {@link #run(ASF) run(boundAsf)}.
     */
    public Value run() {
        if (boundAsf == null)
            throw new IllegalStateException("No ASF bound; use run(ASF) instead.");
        return run(boundAsf);
    }

    /**
     * Registers an external {@link SymbolConstructor} plugin for {@code __name__}
     * reference calls.
     */
    public OutlineInterpreter registerConstructor(String name, SymbolConstructor constructor) {
        constructors.put(name, constructor);
        return this;
    }

    /**
     * Scans {@code dir} for {@code ext_builder_*.jar} files and registers every
     * {@link org.twelve.gcp.plugin.GCPBuilderPlugin} discovered via {@link java.util.ServiceLoader}.
     *
     * <p>The constructor already calls this automatically via {@link PluginLoader#load(GCPConfig)}.
     * Use this method to dynamically append plugins from an additional directory at runtime.
     *
     * @param dir directory to scan (silently ignored if {@code null} or absent)
     * @return {@code this} for chaining
     */
    public OutlineInterpreter loadPlugins(Path dir) {
        PluginLoader.loadFromDirectory(dir)
            .forEach(plugin -> constructors.put(plugin.id(), plugin::construct));
        return this;
    }

    /**
     * Runs a complete multi-module program given as an {@link ASF}.
     * Modules are iterated in declaration order; imports reference previously
     * executed modules.
     */
    public Value run(ASF asf) {
        Value lastResult = UnitValue.INSTANCE;
        for (AST ast : asf.asts()) {
            lastResult = runAst(ast);
        }
        return lastResult;
    }

    /** Runs a single {@link AST} module, handles imports and collects exports. */
    public Value runAst(AST ast) {
        for (Import imp : ast.program().body().imports()) {
            imp.interpret(this);
        }
        Value result = UnitValue.INSTANCE;
        try {
            for (Node stmt : ast.program().body().nodes()) {
                result = eval(stmt);
            }
        } catch (ReturnException re) {
            result = re.value();
        }
        Map<String, Value> exports = resolveExports(ast);
        if (!exports.isEmpty()) {
            moduleExports.put(ast.name(), exports);
        }
        return result;
    }

    // =========================================================================
    // Interpreter state-access methods
    // =========================================================================

    @Override
    public Value eval(Node node) {
        if (node == null) return UnitValue.INSTANCE;
        return node.interpret(this);
    }

    @Override
    public Environment env() { return env; }

    @Override
    public void setEnv(Environment env) { this.env = env; }

    @Override
    public Map<String, SymbolConstructor> constructors() { return constructors; }

    @Override
    public Map<String, Map<String, Value>> moduleExports() { return moduleExports; }

    /**
     * Applies a single argument to a function value (curried single-argument call).
     * Handles built-in lambdas and user-defined closures.
     */
    @Override
    public Value apply(Value fn, Value arg) {
        if (fn instanceof FunctionValue fv) {
            if (fv.isBuiltin()) return fv.builtinFn().apply(arg);

            Environment callEnv = new Environment(fv.closure());
            String paramName = fv.node().argument().name();
            if (!CONSTANTS.UNIT.equals(paramName)) {
                // For Poly arguments passed to a typed parameter, extract the matching component
                if (arg instanceof PolyValue polyArg && fv.node() != null) {
                    Value projected = null;
                    // Post-inference: use inferred outline type
                    Outline paramOutline = resolveParamOutline(fv);
                    if (paramOutline != null) {
                        projected = extractPolyComponent(polyArg, paramOutline.eventual());
                    }
                    // Pre-inference fallback: use declared TypeNode class
                    if (projected == null) {
                        TypeNode declared = fv.node().argument().declared();
                        if (declared != null) projected = extractPolyByTypeNode(polyArg, declared);
                    }
                    if (projected != null) arg = projected;
                }
                callEnv.define(paramName, arg);
            }
            Environment saved = env;
            env = callEnv;
            try {
                return functionBodyI.interpret(fv.node().body(), this);
            } catch (ReturnException re) {
                return re.value();
            } finally {
                env = saved;
            }
        }
        throw new RuntimeException("Cannot apply " + fn + " to " + arg);
    }

    /**
     * Resolves the outline type for a function's first parameter.
     * Uses rawOutline() to avoid ClassCastException when inference has not yet run.
     * Prefers the already-inferred argument outline; falls back to the declared TypeNode's outline.
     */
    private Outline resolveParamOutline(FunctionValue fv) {
        var arg = fv.node().argument();
        // Post-inference path: argument outline is a concrete Genericable
        Outline raw = arg.rawOutline();
        if (raw instanceof Genericable<?,?> && !(raw instanceof UNKNOWN)) {
            return raw;
        }
        // Pre-inference path: use the declared TypeNode's own outline (set during parsing/conversion)
        TypeNode declared = arg.declared();
        if (declared == null) return null;
        Outline declaredOutline = declared.rawOutline();
        if (declaredOutline != null && !(declaredOutline instanceof UNKNOWN)) {
            return declaredOutline;
        }
        return null;
    }

    /**
     * Given a Poly value and the expected parameter outline type, find the component
     * whose runtime type best matches the declared parameter type (role-based dispatch).
     * Returns null if no match is found (caller will pass the full PolyValue as-is).
     */
    private Value extractPolyComponent(PolyValue poly, Outline expected) {
        for (Value opt : poly.options()) {
            if (valueMatchesOutline(opt, expected)) return opt;
        }
        return null;
    }

    /**
     * Pre-inference fallback: extract the Poly component matching the declared TypeNode class.
     * Used when inference has not yet run and the argument outline is still UNKNOWN.
     */
    private Value extractPolyByTypeNode(PolyValue poly, TypeNode declared) {
        if (declared instanceof EntityTypeNode) {
            return poly.options().stream().filter(v -> v instanceof EntityValue).findFirst().orElse(null);
        }
        if (declared instanceof IdentifierTypeNode itn) {
            return switch (itn.name()) {
                case "Integer", "Long"             -> poly.options().stream().filter(v -> v instanceof IntValue).findFirst().orElse(null);
                case "Float", "Double", "Number"   -> poly.options().stream().filter(v -> v instanceof FloatValue).findFirst().orElse(null);
                case "String"                      -> poly.options().stream().filter(v -> v instanceof StringValue).findFirst().orElse(null);
                case "Bool"                        -> poly.options().stream().filter(v -> v instanceof BoolValue).findFirst().orElse(null);
                default -> null;
            };
        }
        return null;
    }

    private boolean valueMatchesOutline(Value v, Outline o) {
        if (o instanceof Entity)   return v instanceof EntityValue;
        if (o instanceof LONG)     return v instanceof IntValue;   // INTEGER extends LONG
        if (o instanceof FLOAT || o instanceof DOUBLE || o instanceof NUMBER)
                                   return v instanceof FloatValue;
        if (o instanceof STRING)   return v instanceof StringValue;
        if (o instanceof BOOL)     return v instanceof BoolValue;
        if (o instanceof Function) return v instanceof FunctionValue;
        return false;
    }

    // =========================================================================
    // Visitor dispatch methods  (mirror OutlineInferencer.visit*)
    // =========================================================================

    @Override public Value visit(LiteralNode<?> node)         { return literalI.interpret(node, this); }
    @Override public Value visit(Identifier node)             { return identifierI.interpret(node, this); }
    @Override public Value visit(SymbolIdentifier node)       { return symbolIdI.interpret(node, this); }
    @Override public Value visit(Variable node)               { return variableI.interpret(node, this); }
    @Override public Value visit(BinaryExpression node)       { return binaryI.interpret(node, this); }
    @Override public Value visit(UnaryExpression node)        { return unaryI.interpret(node, this); }
    @Override public Value visit(Assignment node)             { return assignmentI.interpret(node, this); }
    @Override public Value visit(IsAs node)                   { return isAsI.interpret(node, this); }
    @Override public Value visit(As node)                     { return asI.interpret(node, this); }
    @Override public Value visit(PolyNode node)               { return polyI.interpret(node, this); }
    @Override public Value visit(ThisNode node)               { return thisI.interpret(node, this); }
    @Override public Value visit(BaseNode node)               { return baseI.interpret(node, this); }
    @Override public Value visit(EntityNode node)             { return entityI.interpret(node, this); }
    @Override public Value visit(TupleNode node)              { return tupleI.interpret(node, this); }
    @Override public Value visit(ArrayNode node)              { return arrayI.interpret(node, this); }
    @Override public Value visit(DictNode node)               { return dictI.interpret(node, this); }
    @Override public Value visit(FunctionNode node)           { return functionI.interpret(node, this); }
    @Override public Value visit(FunctionCallNode node)       { return functionCallI.interpret(node, this); }
    @Override public Value visit(MemberAccessor node)         { return memberAccessorI.interpret(node, this); }
    @Override public Value visit(ArrayAccessor node)          { return arrayAccessorI.interpret(node, this); }
    @Override public Value visit(ReferenceCallNode node)      { return refCallI.interpret(node, this); }
    @Override public Value visit(Selections<?> node)          { return selectionsI.interpret(node, this); }
    @Override public Value visit(MatchExpression node)        { return matchI.interpret(node, this); }
    @Override public Value visit(Block node)                  { return blockI.interpret(node, this); }
    @Override public Value visit(FunctionBody node)           { return functionBodyI.interpret(node, this); }
    @Override public Value visit(WithExpression node)         { return withExprI.interpret(node, this); }
    @Override public Value visit(VariableDeclarator node)     { return varDeclI.interpret(node, this); }
    @Override public Value visit(ExpressionStatement node)    { return exprStmtI.interpret(node, this); }
    @Override public Value visit(ReturnStatement node)        { return returnI.interpret(node, this); }
    @Override public Value visit(MemberNode node)             { return memberNodeI.interpret(node, this); }
    @Override public Value visit(OutlineDeclarator node)      { return outlineDeclI.interpret(node, this); }
    @Override public Value visit(Import node)                 { return importI.interpret(node, this); }
    @Override public Value visit(Export node)                 { return exportI.interpret(node, this); }

    // =========================================================================
    // Helpers
    // =========================================================================

    private void registerBuiltins() {
        env.define("true",  BoolValue.TRUE);
        env.define("false", BoolValue.FALSE);
        // ── Global built-in functions ─────────────────────────────────────────
        env.define("print", new FunctionValue(v -> {
            String msg = v.display();
            org.twelve.gcp.interpreter.stdlib.ConsoleCapture.add(
                    org.twelve.gcp.interpreter.stdlib.ConsoleCapture.Level.LOG, msg);
            System.out.println(msg);
            return UnitValue.INSTANCE;
        }));
        env.define("to_str", new FunctionValue(v -> new StringValue(v.display())));
        env.define("to_int", new FunctionValue(v -> {
            if (v instanceof IntValue iv) return iv;
            if (v instanceof FloatValue fv) return new IntValue((long) fv.value());
            if (v instanceof StringValue sv) {
                try { return new IntValue(Long.parseLong(sv.value().trim())); }
                catch (NumberFormatException e) { return UnitValue.INSTANCE; }
            }
            return UnitValue.INSTANCE;
        }));
        env.define("to_float", new FunctionValue(v -> {
            if (v instanceof FloatValue fv) return fv;
            if (v instanceof IntValue iv) return new FloatValue((double) iv.value());
            if (v instanceof StringValue sv) {
                try { return new FloatValue(Double.parseDouble(sv.value().trim())); }
                catch (NumberFormatException e) { return UnitValue.INSTANCE; }
            }
            return UnitValue.INSTANCE;
        }));
        env.define("to_number", new FunctionValue(v -> {
            if (v instanceof IntValue || v instanceof FloatValue) return v;
            if (v instanceof StringValue sv) {
                try { return new IntValue(Long.parseLong(sv.value().trim())); }
                catch (NumberFormatException ignored) {}
                try { return new FloatValue(Double.parseDouble(sv.value().trim())); }
                catch (NumberFormatException e) { return UnitValue.INSTANCE; }
            }
            return UnitValue.INSTANCE;
        }));
        env.define("len", new FunctionValue(v -> {
            if (v instanceof StringValue sv) return new IntValue(sv.value().length());
            if (v instanceof ArrayValue av)  return new IntValue(av.size());
            if (v instanceof DictValue dv)   return new IntValue(dv.size());
            if (v instanceof TupleValue tv)  return new IntValue(tv.size());
            throw new RuntimeException("len: unsupported type " + v.getClass().getSimpleName());
        }));
        env.define("type_of", new FunctionValue(v -> {
            String name;
            if      (v instanceof IntValue)      name = "Int";
            else if (v instanceof FloatValue)    name = "Double";
            else if (v instanceof StringValue)   name = "String";
            else if (v instanceof BoolValue)     name = "Bool";
            else if (v instanceof UnitValue)     name = "Unit";
            else if (v instanceof ArrayValue)    name = "Array";
            else if (v instanceof TupleValue)    name = "Tuple";
            else if (v instanceof DictValue)     name = "Dict";
            else if (v instanceof EntityValue)   name = "Entity";
            else if (v instanceof FunctionValue) name = "Function";
            else                                 name = v.getClass().getSimpleName();
            return new StringValue(name);
        }));
        env.define("assert", new FunctionValue(v -> {
            if (!v.isTruthy()) throw new RuntimeException("Assertion failed");
            return UnitValue.INSTANCE;
        }));
        org.twelve.gcp.interpreter.stdlib.StdLibRuntime.registerAll(env);

        // Built-in external builder — same GCPBuilderPlugin contract as JAR plugins;
        // only the loading mechanism differs (instantiated directly, not discovered from a JAR).
        ExternalBuilderPlugin externalBuilder = new ExternalBuilderPlugin(this, typeDefinitions);
        constructors.put(externalBuilder.id(), externalBuilder::construct);
    }

    private Map<String, Value> resolveExports(AST ast) {
        Map<String, Value> result = new LinkedHashMap<>();
        for (Export export : ast.program().body().exports()) {
            export.specifiers().forEach(spec -> {
                String localName    = spec.local().name();
                String exportedName = spec.exported().name();
                Value v = env.lookup(localName);
                if (v != null) result.put(exportedName, v);
            });
        }
        return result;
    }

    @Override
    public Map<String, OutlineDefinition> typeDefinitions() { return typeDefinitions; }

    /** Returns the current environment (for testing / introspection). */
    public Environment currentEnv() { return env; }
}
