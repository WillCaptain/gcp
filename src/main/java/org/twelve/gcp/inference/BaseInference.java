package org.twelve.gcp.inference;

import org.twelve.gcp.ast.Node;
import org.twelve.gcp.node.expression.BaseNode;
import org.twelve.gcp.outline.Outline;
import org.twelve.gcp.outline.adt.Entity;
import org.twelve.gcp.outline.adt.ProductADT;

public class BaseInference implements Inference<BaseNode> {
    @Override
    public Outline infer(BaseNode node, Inferencer inferencer) {
        return findBase(node);
    }

    private Outline findBase(Node node) {
        Node next = node;
        while (next != null && !(next.outline() instanceof ProductADT)) {
            next = next.parent();
        }
        if (next == null) {
            return node.ast().unknown(node);
        } else {
            return ((Entity)next.outline()).base();
        }
    }
}
