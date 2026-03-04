package org.twelve.gcp.inference;

import org.twelve.gcp.exception.GCPErrCode;
import org.twelve.gcp.exception.GCPErrorReporter;
import org.twelve.gcp.node.expression.AwaitNode;
import org.twelve.gcp.outline.Outline;
import org.twelve.gcp.outline.adt.Promise;

/**
 * Type inference for {@link AwaitNode}.
 *
 * <p>If the awaited expression has type {@code Promise<T>}, the await expression
 * infers to {@code T}. Awaiting a non-Promise type is a type error.
 */
public class AwaitInference implements Inference<AwaitNode> {
    @Override
    public Outline infer(AwaitNode node, Inferencer inferencer) {
        Outline promiseOutline = node.promise().infer(inferencer);
        if (promiseOutline instanceof Promise) {
            return ((Promise) promiseOutline).innerOutline();
        }
        GCPErrorReporter.report(node, GCPErrCode.OUTLINE_MISMATCH,
                "await requires a Promise<T>, got: " + promiseOutline);
        return node.ast().Error;
    }
}
