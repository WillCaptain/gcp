package org.twelve.gcp.interpreter.interpretation;

import org.twelve.gcp.interpreter.Environment;
import org.twelve.gcp.interpreter.Interpretation;
import org.twelve.gcp.interpreter.Interpreter;
import org.twelve.gcp.interpreter.value.PromiseValue;
import org.twelve.gcp.interpreter.value.Value;
import org.twelve.gcp.node.expression.AsyncNode;

import java.util.concurrent.CompletableFuture;

/**
 * Interpretation for {@link AsyncNode}.
 *
 * <p>Captures the lexical environment at the point of the {@code async} expression,
 * then submits the body for asynchronous evaluation on the common ForkJoinPool.
 * The result is wrapped in a {@link PromiseValue}.
 *
 * <p>Note: the {@link Interpreter} instance is shared; concurrent mutation of its
 * environment by overlapping async tasks is not safe. In practice, GCP programs that
 * chain {@code async}/{@code await} sequentially are always correct.
 */
public class AsyncInterpretation implements Interpretation<AsyncNode> {

    @Override
    public Value interpret(AsyncNode node, Interpreter interp) {
        Environment capturedEnv = interp.env();
        CompletableFuture<Value> future = CompletableFuture.supplyAsync(() -> {
            interp.setEnv(capturedEnv);
            return interp.eval(node.body());
        });
        return new PromiseValue(future);
    }
}
