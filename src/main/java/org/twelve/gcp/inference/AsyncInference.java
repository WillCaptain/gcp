package org.twelve.gcp.inference;

import org.twelve.gcp.node.expression.AsyncNode;
import org.twelve.gcp.outline.Outline;
import org.twelve.gcp.outline.adt.Promise;

/**
 * Type inference for {@link AsyncNode}.
 *
 * <p>If the inner expression has type {@code T}, the async expression infers to
 * {@code Promise<T>}.
 */
public class AsyncInference implements Inference<AsyncNode> {
    @Override
    public Outline infer(AsyncNode node, Inferencer inferencer) {
        Outline inner = node.body().infer(inferencer);
        return Promise.from(node, inner);
    }
}
