package org.twelve.gcp.inference;

import org.twelve.gcp.node.expression.typeable.ArrayTypeNode;
import org.twelve.gcp.node.function.Argument;
import org.twelve.gcp.outline.Outline;
import org.twelve.gcp.outline.adt.Array;
import org.twelve.gcp.outline.projectable.Generic;

public class ArrayTypeNodeInference implements Inference<ArrayTypeNode> {
    @Override
    public Outline infer(ArrayTypeNode node, Inferences inferences) {
        Outline itemOutline = node.itemNode() == null ? Outline.Unknown : node.itemNode().infer(inferences);
        if(itemOutline==Outline.Unknown && node.parent() instanceof Argument){
            itemOutline = Generic.from(node,null);
        }
        return new Array(node, itemOutline);
    }
}
