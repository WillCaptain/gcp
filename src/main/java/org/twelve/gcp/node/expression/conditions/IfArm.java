package org.twelve.gcp.node.expression.conditions;

import org.twelve.gcp.ast.Token;
import org.twelve.gcp.inference.Inferences;
import org.twelve.gcp.node.expression.Expression;
import org.twelve.gcp.node.expression.LiteralNode;
import org.twelve.gcp.outline.Outline;

public class IfArm extends Arm<Expression>{
    private final LiteralNode<Boolean> others;

    public IfArm(Expression test, Consequence consequence) {
        super(consequence.ast(),test,consequence);
        this.others = LiteralNode.parse(consequence.ast(), new Token<>(true));
    }
    public IfArm(Consequence consequence) {
        this(null,consequence);

    }

    @Override
    public Expression test(){
        return this.test==null?others:test;
    }

    @Override
    public Boolean isElse(){
        return this.test==null;
    }

}
