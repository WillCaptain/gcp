package org.twelve.gcp.inference;

import org.twelve.gcp.ast.AbstractNode;
import org.twelve.gcp.outline.Outline;

public interface Inference<T extends AbstractNode> {
    Outline infer(T node, Inferencer inferencer);
}
