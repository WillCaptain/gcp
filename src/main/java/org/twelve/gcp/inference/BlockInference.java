package org.twelve.gcp.inference;

import org.twelve.gcp.node.expression.body.Block;
import org.twelve.gcp.outline.Outline;

public class BlockInference extends BodyInference<Block> {
    @Override
    public Outline infer(Block node, Inferences inferences) {
        return super.infer(node, inferences);
    }
}
