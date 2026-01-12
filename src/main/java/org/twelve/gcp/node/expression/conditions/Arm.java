package org.twelve.gcp.node.expression.conditions;

import org.twelve.gcp.ast.AST;
import org.twelve.gcp.ast.Location;
import org.twelve.gcp.ast.Token;
import org.twelve.gcp.inference.Inferences;
import org.twelve.gcp.node.expression.Expression;
import org.twelve.gcp.node.expression.LiteralNode;
import org.twelve.gcp.node.expression.body.Block;
import org.twelve.gcp.outline.Outline;

/**
 * one particular branch in a selection
 * contains a predicate and a consequence
 * if predicate is null means this is else/alternative
 * @author huizi 2025
 */
public abstract class Arm<T extends Expression> extends Block {
    protected final T test;
    protected final Consequence consequence;

    public Arm(AST ast, T test, Consequence consequence) {
        super(ast);
        this.addNode(test);
        this.addNode(consequence);
        this.test = test;
        this.consequence = consequence;
    }

    public Consequence consequence(){
        return this.consequence;
    }
    @Override
    public Outline accept(Inferences inferences) {
        return inferences.visit(this);
    }
    @Override
    public Outline outline() {
        return this.consequence.outline();
    }

    public abstract T test();

    public abstract Boolean isElse();

}
