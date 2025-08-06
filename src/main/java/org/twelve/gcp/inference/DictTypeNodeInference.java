package org.twelve.gcp.inference;

import org.twelve.gcp.node.expression.typeable.DictTypeNode;
import org.twelve.gcp.node.function.Argument;
import org.twelve.gcp.outline.Outline;
import org.twelve.gcp.outline.adt.Dict;
import org.twelve.gcp.outline.projectable.Generic;

public class DictTypeNodeInference implements Inference<DictTypeNode> {
    @Override
    public Outline infer(DictTypeNode node, Inferences inferences) {
        Outline key = node.keyNode() == null ? Outline.Any : node.keyNode().infer(inferences);
        Outline value = node.valueNode() == null ? Outline.Any : node.valueNode().infer(inferences);
        if (node.parent() instanceof Argument) {
            if (key == Outline.Any) {
                key = Generic.from(node, null);
                value = Generic.from(node, null);
            }
        } else {//if parent is not Argument, remove the Generic
            if (key instanceof Generic) key = Outline.Any;
            if (value instanceof Generic) value = Outline.Any;
        }
        return new Dict(node, key, value);
    }
}
