package org.twelve.gcp.inference;

import org.twelve.gcp.node.expression.typeable.ArrayTypeNode;
import org.twelve.gcp.node.function.Argument;
import org.twelve.gcp.outline.Outline;
import org.twelve.gcp.outline.adt.Array;
import org.twelve.gcp.outline.projectable.Generic;

public class ArrayTypeNodeInference implements Inference<ArrayTypeNode> {
    @Override
    public Outline infer(ArrayTypeNode node, Inferencer inferencer) {

        Outline itemOutline = node.itemNode() == null ? node.ast().Any : node.itemNode().infer(inferencer);
        if (itemOutline == node.ast().Any && node.parent() instanceof Argument) {
            itemOutline = Generic.from(node, null);
        }
        return Array.from(node, itemOutline);
    }
}
