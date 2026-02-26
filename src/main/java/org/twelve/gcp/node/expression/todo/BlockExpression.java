package org.twelve.gcp.node.expression.todo;

import org.twelve.gcp.inference.Inferencer;
import org.twelve.gcp.node.expression.body.Block;
import org.twelve.gcp.node.expression.Expression;
import org.twelve.gcp.outline.Outline;

public class BlockExpression extends Expression {
    private final Block block;

    public BlockExpression(Block block) {
        super(block.ast(), block.loc());
        this.block = block;
    }

    @Override
    public Outline acceptInfer(Inferencer inferencer) {
        return inferencer.visit(this);
    }
}
