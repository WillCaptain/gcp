package org.twelve.gcp.interpreter.value;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

/**
 * Runtime value for an async computation, wrapping a {@link CompletableFuture}.
 *
 * <p>Produced by evaluating an {@code AsyncNode}. The computation runs on the
 * common ForkJoinPool thread. Use {@link #get()} (or evaluate an {@code AwaitNode})
 * to block the calling thread until the result is available.
 */
public final class PromiseValue implements Value {

    private final CompletableFuture<Value> future;

    public PromiseValue(CompletableFuture<Value> future) {
        this.future = future;
    }

    /** Returns the underlying future. */
    public CompletableFuture<Value> future() {
        return future;
    }

    /**
     * Blocks until the async computation resolves and returns the result.
     * Returns {@link UnitValue#INSTANCE} on interruption or execution failure.
     */
    public Value get() {
        try {
            return future.get();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return UnitValue.INSTANCE;
        } catch (ExecutionException e) {
            return UnitValue.INSTANCE;
        }
    }

    @Override
    public Object unwrap() {
        return future;
    }

    @Override
    public boolean isTruthy() {
        return true;
    }

    @Override
    public String display() {
        if (future.isDone()) {
            try {
                return "Promise<resolved:" + future.get().display() + ">";
            } catch (Exception e) {
                return "Promise<failed>";
            }
        }
        return "Promise<pending>";
    }

    @Override
    public String toString() {
        return display();
    }
}
