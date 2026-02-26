package org.twelve.gcp.inference;

import org.twelve.gcp.ast.AST;
import org.twelve.gcp.node.expression.typeable.DictTypeNode;
import org.twelve.gcp.node.function.Argument;
import org.twelve.gcp.outline.Outline;
import org.twelve.gcp.outline.adt.Dict;
import org.twelve.gcp.outline.projectable.Generic;

public class DictTypeNodeInference implements Inference<DictTypeNode> {
    @Override
    public Outline infer(DictTypeNode node, Inferencer inferencer) {
        AST ast = node.ast();
        Outline key = node.keyNode() == null ? ast.Any : node.keyNode().infer(inferencer);
        Outline value = node.valueNode() == null ? ast.Any : node.valueNode().infer(inferencer);
        if (node.parent() instanceof Argument) {
            if (key == ast.Any) {
                key = Generic.from(node, null);
                value = Generic.from(node, null);
            }
        } else {//if parent is not Argument, remove the Generic
            if (key instanceof Generic) key = ast.Any;
            if (value instanceof Generic) value = ast.Any;
        }
        return new Dict(node, key, value);
    }
}
