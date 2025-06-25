package org.twelve.gcp.inference;

import org.twelve.gcp.node.expression.PolyNode;
import org.twelve.gcp.outline.Outline;
import org.twelve.gcp.outline.adt.Poly;

public class PolyInference implements Inference<PolyNode> {
    @Override
    public Outline infer(PolyNode node, Inferences inferences) {
        Outline[] outlines = node.nodes().stream().map(n -> n.infer(inferences)).toArray(Outline[]::new);
        return Poly.from(node,outlines);
    }
}
