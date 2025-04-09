package org.twelve.gcp.node.statement;

import org.twelve.gcp.ast.AST;
import org.twelve.gcp.inference.Inferences;
import org.twelve.gcp.node.expression.Expression;
import org.twelve.gcp.outline.Outline;

public class ReturnStatement extends Statement{
    private final Expression expression;

    public ReturnStatement(Expression expression) {
        super(expression.ast());
        this.expression = this.addNode(expression);
    }

    public ReturnStatement(AST ast) {
        super(ast);
        this.expression = null;
    }

    @Override
    public Outline outline() {
        return this.expression.outline();
    }

    public Expression expression() {
        return this.expression;
    }

    @Override
    protected Outline accept(Inferences inferences) {
        return inferences.visit(this);
    }
}
