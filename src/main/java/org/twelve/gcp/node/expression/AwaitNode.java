package org.twelve.gcp.node.expression;

import org.twelve.gcp.ast.AST;
import org.twelve.gcp.inference.Inferencer;
import org.twelve.gcp.interpreter.Interpreter;
import org.twelve.gcp.interpreter.value.Value;
import org.twelve.gcp.outline.Outline;

/**
 * AST node for the {@code await} expression: {@code await <expr>}.
 *
 * <p>Unwraps a {@code Promise<T>} to its inner type {@code T}, blocking
 * the current execution until the async computation completes.
 *
 * <p>Example (GCP pseudo-syntax):
 * <pre>
 *   let p = async { 42 }
 *   let x = await p      // x : Integer, value = 42
 * </pre>
 */
public class AwaitNode extends Expression {

    private final Expression promise;

    public AwaitNode(AST ast, Expression promise) {
        super(ast, null);
        this.promise = this.addNode(promise);
    }

    /** Returns the expression that should evaluate to a {@code Promise<T>}. */
    public Expression promise() {
        return promise;
    }

    @Override
    public String lexeme() {
        return "await " + promise.lexeme();
    }

    @Override
    public Outline acceptInfer(Inferencer inferencer) {
        return inferencer.visit(this);
    }

    @Override
    public Value acceptInterpret(Interpreter interpreter) {
        return interpreter.visit(this);
    }
}
