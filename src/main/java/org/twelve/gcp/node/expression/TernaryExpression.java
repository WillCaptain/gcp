package org.twelve.gcp.node.expression;

import org.twelve.gcp.inference.Inferences;
import org.twelve.gcp.outline.Outline;

public class TernaryExpression extends Expression {
    private final Expression condition;     // The condition expression
    private final Expression trueBranch;    // Executed if condition is true
    private final Expression falseBranch;   // Executed if condition is false

    public TernaryExpression(Expression condition, Expression trueBranch, Expression falseBranch) {
        super(condition.ast(), null);
        this.condition = condition;
        this.trueBranch = trueBranch;
        this.falseBranch = falseBranch;
    }

    @Override
    public Outline outline() {
        return trueBranch.outline();
    }

    public Expression condition(){
        return this.condition;
    }

    public Expression trueBranch(){
        return this.trueBranch;
    }

    public Expression falseBranch(){
        return this.falseBranch;
    }

    @Override
    protected Outline accept(Inferences inferences) {
        return inferences.visit(this);
    }
}
