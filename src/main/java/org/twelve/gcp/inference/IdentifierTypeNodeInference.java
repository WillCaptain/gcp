package org.twelve.gcp.inference;

import org.twelve.gcp.node.expression.typeable.IdentifierTypeNode;
import org.twelve.gcp.outline.Outline;

public class IdentifierTypeNodeInference implements Inference<IdentifierTypeNode> {
    @Override
    public Outline infer(IdentifierTypeNode node, Inferencer inferencer) {
        return node.ast().symbolEnv().lookupOutline(node.name()).outline();
    }
}
