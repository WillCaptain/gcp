package org.twelve.gcp.inference;

import org.twelve.gcp.node.expression.typeable.IdentifierTypeNode;
import org.twelve.gcp.outline.Outline;

public class IdentifierTypeInference implements Inference<IdentifierTypeNode> {
    @Override
    public Outline infer(IdentifierTypeNode node, Inferences inferences) {
        return node.ast().symbolEnv().lookupOutline(node.name()).outline();
    }
}
