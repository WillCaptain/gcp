package org.twelve.gcp.inference;

import org.twelve.gcp.ast.Node;
import org.twelve.gcp.node.expression.Base;
import org.twelve.gcp.outline.Outline;
import org.twelve.gcp.outline.adt.Entity;
import org.twelve.gcp.outline.adt.ProductADT;

public class BaseInference implements Inference<Base> {
    @Override
    public Outline infer(Base node, Inferences inferences) {
        return findBase(node);
    }

    private Outline findBase(Node node) {
        while (node != null && !(node.outline() instanceof ProductADT)) {
            node = node.parent();
        }
        if (node == null) {
            return Outline.Unknown;
        } else {
            return ((Entity)node.outline()).base();
        }
    }
}
