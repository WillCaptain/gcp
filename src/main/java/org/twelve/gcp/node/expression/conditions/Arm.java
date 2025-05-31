package org.twelve.gcp.node.expression.conditions;

import org.twelve.gcp.ast.AST;
import org.twelve.gcp.ast.Token;
import org.twelve.gcp.inference.Inferences;
import org.twelve.gcp.node.expression.Expression;
import org.twelve.gcp.node.expression.LiteralNode;
import org.twelve.gcp.outline.Outline;

/**
 * one particular branch in a selection
 * contains a predicate and a consequence
 * if predicate is null means this is else/alternative
 * @author huizi 2025
 */
public class Arm extends Expression {

    private final Expression test;
    private final Consequence consequence;
    private final LiteralNode<Boolean> others;

    public Arm(Expression test, Consequence consequence) {
        super(consequence.ast(),null);
        this.addNode(test);
        this.addNode(consequence);
        this.test = test;
        this.consequence = consequence;
        this.others = LiteralNode.parse(consequence.ast(), new Token<>(true));
    }
    public Arm(Consequence consequence) {
        this(null,consequence);

    }


    public Expression test(){
        return this.test==null?others:test;
    }

    public Boolean isElse(){
        return this.test==null;
    }

    public Consequence consequence(){
        return consequence;
    }

    @Override
    protected Outline accept(Inferences inferences) {
        return inferences.visit(this);
    }

    @Override
    public Outline outline() {
        return this.consequence.outline();
    }

}
