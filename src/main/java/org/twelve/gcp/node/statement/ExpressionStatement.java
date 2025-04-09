package org.twelve.gcp.node.statement;

import org.twelve.gcp.inference.Inferences;
import org.twelve.gcp.node.expression.Expression;
import org.twelve.gcp.outline.adt.ProductADT;
import org.twelve.gcp.outline.Outline;

/**
 * wrap an expression to make it a return ignore statement
 */
public class ExpressionStatement extends Statement{
    private final Expression expression;

    public ExpressionStatement(Expression expression) {
        super(expression.ast());
        this.expression = expression;
        this.addNode(expression);
    }

    public Expression expression() {
        return this.expression;
    }

    @Override
    protected Outline accept(Inferences inferences) {
        this.expression.infer(inferences);
        return ProductADT.Ignore;
    }

    @Override
    public String lexeme() {
        return super.lexeme()+";";
    }
}
