package org.twelve.gcp.inference;

import org.twelve.gcp.ast.Node;
import org.twelve.gcp.outline.Outline;

public interface Inference<T extends Node> {
    Outline infer(T node, Inferences inferences);
}
