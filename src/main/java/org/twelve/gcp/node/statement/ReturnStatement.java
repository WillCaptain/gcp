package org.twelve.gcp.node.statement;

import org.twelve.gcp.ast.AST;
import org.twelve.gcp.inference.Inferencer;
import org.twelve.gcp.node.expression.Expression;
import org.twelve.gcp.outline.Outline;
import org.twelve.gcp.interpreter.Interpreter;
import org.twelve.gcp.interpreter.value.Value;

public class ReturnStatement extends Statement {
    private final Expression expression;

    public ReturnStatement(Expression expression) {
        this(expression.ast(), expression);
    }

    public ReturnStatement(AST ast) {
        this(ast, null);
    }

    private ReturnStatement(AST ast, Expression expression) {
        super(ast);
        this.outline = ast.unknown(this);
        if (expression == null) {
            this.expression = null;
        } else {
            this.expression = this.addNode(expression);
        }
    }
    public Expression expression() {
        return this.expression;
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
