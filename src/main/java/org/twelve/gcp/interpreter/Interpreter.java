package org.twelve.gcp.interpreter;

import org.twelve.gcp.ast.Node;
import org.twelve.gcp.inference.Inferencer;
import org.twelve.gcp.interpreter.value.Value;
import org.twelve.gcp.interpreter.value.UnitValue;
import org.twelve.gcp.node.expression.*;
import org.twelve.gcp.node.expression.OutlineDefinition;
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

import java.util.Collections;
import java.util.Map;

/**
 * Visitor interface for the interpreter pass.
 * Mirrors {@link Inferencer} in the type-inference layer.
 *
 * <p>Each {@code visit} method corresponds to one AST node type.
 * The concrete implementation ({@link OutlineInterpreter}) carries all runtime state
 * (environment, module exports, constructor registry) and delegates each visit to
 * a dedicated {@link Interpretation} class.
 *
 * <p>Additional state-access methods ({@link #eval}, {@link #env}, {@link #setEnv},
 * {@link #apply}) are provided so that stateless {@link Interpretation} classes can
 * recurse and manipulate the interpreter environment without needing a back-reference
 * to the concrete implementation.
 */
public interface Interpreter {

    // =========================================================================
    // Default fallback – unhandled nodes produce no value
    // =========================================================================

    /** Fallback: unrecognised node returns unit. */
    default Value visit(Node node) { return UnitValue.INSTANCE; }

    // =========================================================================
    // Literals & identifiers
    // =========================================================================

    Value visit(LiteralNode<?> node);
    Value visit(Identifier node);
    Value visit(SymbolIdentifier node);
    Value visit(Variable node);

    // =========================================================================
    // Expressions
    // =========================================================================

    Value visit(BinaryExpression node);
    Value visit(UnaryExpression node);
    Value visit(Assignment node);
    Value visit(IsAs node);
    Value visit(As node);
    Value visit(PolyNode node);
    Value visit(ThisNode node);
    Value visit(BaseNode node);

    // =========================================================================
    // Collections & constructors
    // =========================================================================

    Value visit(EntityNode node);
    Value visit(TupleNode node);
    Value visit(ArrayNode node);
    Value visit(DictNode node);

    // =========================================================================
    // Functions
    // =========================================================================

    Value visit(FunctionNode node);
    Value visit(FunctionCallNode node);

    // =========================================================================
    // Accessors
    // =========================================================================

    Value visit(MemberAccessor node);
    Value visit(ArrayAccessor node);
    Value visit(ReferenceCallNode node);

    // =========================================================================
    // Control flow
    // =========================================================================

    Value visit(Selections<?> node);
    Value visit(MatchExpression node);

    // =========================================================================
    // Bodies / blocks
    // =========================================================================

    Value visit(Block node);
    Value visit(FunctionBody node);
    Value visit(WithExpression node);

    // =========================================================================
    // Statements
    // =========================================================================

    Value visit(VariableDeclarator node);
    Value visit(ExpressionStatement node);
    Value visit(ReturnStatement node);
    Value visit(MemberNode node);
    Value visit(OutlineDeclarator node);

    // =========================================================================
    // Module system
    // =========================================================================

    Value visit(Import node);
    Value visit(Export node);

    // =========================================================================
    // State-access methods for use by Interpretation classes
    // =========================================================================

    /**
     * Evaluate any AST node in the current environment.
     * Equivalent to calling {@code node.interpret(this)}.
     */
    Value eval(Node node);

    /** Returns the current lexical environment. */
    Environment env();

    /** Replaces the current lexical environment. */
    void setEnv(Environment env);

    /**
     * Applies {@code fn} to {@code arg}: single-argument curried application.
     * Built-ins, closures and unit-calls are all handled here.
     */
    Value apply(Value fn, Value arg);

    /** Returns the registered external constructor plugins. */
    Map<String, SymbolConstructor> constructors();

    /** Returns the module-export table for cross-module import resolution. */
    Map<String, Map<String, Value>> moduleExports();

    /**
     * Returns the runtime type-definition registry populated by {@code outline} declarations.
     * Used by the interpreter to resolve declared-type annotations (e.g. {@code let x:T = ...})
     * without depending on the inference pass.
     */
    default Map<String, OutlineDefinition> typeDefinitions() {
        return Collections.emptyMap();
    }
}
