package org.twelve.gcp.inference;

import org.twelve.gcp.ast.Node;
import org.twelve.gcp.node.expression.This;
import org.twelve.gcp.outline.Outline;
import org.twelve.gcp.outline.adt.ProductADT;

import static org.twelve.gcp.common.Tool.cast;

public class ThisInference implements Inference<This> {
    @Override
    public Outline infer(This node, Inferences inferences) {
        return findEntity(node);
    }

    private Outline findEntity(Node node) {
        while (node != null && !(node.outline() instanceof ProductADT)) {
            node = node.parent();
        }
        if (node == null) {
            return Outline.Unknown;
        } else {
            return cast(node.outline());
        }
    }
}
