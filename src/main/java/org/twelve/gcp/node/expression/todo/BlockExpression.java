package org.twelve.gcp.node.expression.todo;

import org.twelve.gcp.inference.Inferences;
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
    public Outline accept(Inferences inferences) {
        return inferences.visit(this);
    }
}
