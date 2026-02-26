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
 * </ul>
 */
public class OutlineInterpreter implements Interpreter {

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

    public OutlineInterpreter() {
        registerBuiltins();
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
        env.define("to_str", new FunctionValue(v -> new StringValue(v.display())));
        env.define("print",  new FunctionValue(v -> {
            System.out.println(v.display());
            return UnitValue.INSTANCE;
        }));
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
