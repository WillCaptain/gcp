package org.twelve.gcp.inference;

import org.twelve.gcp.node.expression.typeable.ThisTypeNode;
import org.twelve.gcp.outline.Outline;

public class ThisTypeNodeInference implements Inference<ThisTypeNode> {
    @Override
    public Outline infer(ThisTypeNode node, Inferencer inferencer) {
        return ThisInference.findEntity(node);
    }
}
