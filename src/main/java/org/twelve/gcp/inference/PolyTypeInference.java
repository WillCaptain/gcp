package org.twelve.gcp.inference;

import org.twelve.gcp.node.expression.typeable.PolyTypeNode;
import org.twelve.gcp.outline.Outline;
import org.twelve.gcp.outline.adt.Poly;

public class PolyTypeInference  implements Inference<PolyTypeNode>{
    @Override
    public Outline infer(PolyTypeNode node, Inferences inferences) {
        Outline[] outlines = node.nodes().stream().map(n -> n.infer(inferences)).toArray(Outline[]::new);
        return Poly.from(node,outlines);
    }
}
