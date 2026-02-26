package org.twelve.gcp.inference;

import org.twelve.gcp.node.expression.typeable.OptionTypeNode;
import org.twelve.gcp.outline.Outline;
import org.twelve.gcp.outline.adt.Option;

public class OptionTypeInference implements Inference<OptionTypeNode>{
    @Override
    public Outline infer(OptionTypeNode node, Inferencer inferencer) {
        Outline[] outlines = node.nodes().stream().map(n -> n.infer(inferencer)).toArray(Outline[]::new);
        return Option.from(node,outlines);
    }
}
