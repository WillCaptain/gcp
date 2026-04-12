package org.twelve.gcp.inference;

import org.twelve.gcp.node.expression.typeable.NullableTypeNode;
import org.twelve.gcp.outline.Outline;
import org.twelve.gcp.outline.adt.Option;

/**
 * Infers a {@link NullableTypeNode} ({@code T?}) as {@code T|Nothing}.
 */
public class NullableTypeInference implements Inference<NullableTypeNode> {
    @Override
    public Outline infer(NullableTypeNode node, Inferencer inferencer) {
        Outline inner = node.inner().infer(inferencer);
        return Option.from(node, inner, node.ast().Nothing);
    }
}
