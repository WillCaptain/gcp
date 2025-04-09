package org.twelve.gcp.node.expression.conditions;

import org.twelve.gcp.ast.OAST;
import org.twelve.gcp.node.expression.Expression;
import org.twelve.gcp.node.expression.body.Block;
import org.twelve.gcp.outline.Outline;

public class Arm extends Expression {
    private final Predicate predicate;
    private final Block block;

    public Arm(OAST ast, Predicate predicate, Block block) {
        super(ast,null);
        this.addNode(predicate);
        this.addNode(block);
        this.predicate = predicate;
        this.block = block;
    }

    @Override
    public Outline outline() {
        return this.block.outline();
    }
}
