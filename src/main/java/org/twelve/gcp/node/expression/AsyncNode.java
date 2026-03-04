package org.twelve.gcp.node.expression;

import org.twelve.gcp.ast.AST;
import org.twelve.gcp.inference.Inferencer;
import org.twelve.gcp.interpreter.Interpreter;
import org.twelve.gcp.interpreter.value.Value;
import org.twelve.gcp.outline.Outline;

/**
 * AST node for the {@code async} expression: {@code async <expr>}.
 *
 * <p>The wrapped expression is evaluated asynchronously, and the node's
 * inferred type is {@code Promise<T>} where {@code T} is the type of the inner expression.
 *
 * <p>Example (GCP pseudo-syntax):
 * <pre>
 *   let p = async (x: Integer) { x + 1 }
 *   // p : Promise&lt;Integer -&gt; Integer&gt;
 *
 *   let q = async { 42 }
 *   // q : Promise&lt;Integer&gt;
 * </pre>
 */
public class AsyncNode extends Expression {

    private final Expression body;

    public AsyncNode(AST ast, Expression body) {
        super(ast, null);
        this.body = this.addNode(body);
    }

    /** Returns the expression that runs asynchronously. */
    public Expression body() {
        return body;
    }

    @Override
    public String lexeme() {
        return "async " + body.lexeme();
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
