package org.twelve.gcp.inference;

import org.twelve.gcp.node.LiteralUnionNode;
import org.twelve.gcp.outline.Outline;
import org.twelve.gcp.outline.adt.LiteralUnion;

public class LiteralUnionInference  implements Inference<LiteralUnionNode>{
    @Override
    public Outline infer(LiteralUnionNode node, Inferences inferences) {
        return LiteralUnion.from(node);
    }
}
