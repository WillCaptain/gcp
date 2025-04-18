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

    public Arm(AST ast, Expression test, Consequence consequence) {
        super(ast,null);
        this.addNode(test);
        this.addNode(consequence);
        this.test = test;
        this.consequence = consequence;
    }
    public Arm(AST ast, Consequence consequence) {
        this(ast, LiteralNode.parse(ast,new Token<>(true)),consequence);
    }


    public Expression test(){
        return this.test;
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
