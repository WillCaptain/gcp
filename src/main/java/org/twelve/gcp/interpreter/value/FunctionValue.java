package org.twelve.gcp.interpreter.value;

import org.twelve.gcp.interpreter.Environment;
import org.twelve.gcp.node.function.FunctionNode;

import java.util.function.Function;

/**
 * Closure runtime value: a function node plus its captured lexical environment.
 * <p>
 * GCP functions are curried – each FunctionNode takes exactly one argument and
 * returns another Value (possibly another FunctionValue for multi-arg functions).
 * </p>
 * Also used to wrap built-in Java lambdas (builtinFn).
 */
public final class FunctionValue implements Value {

    private final FunctionNode node;
    private final Environment closure;

    /** For built-in (Java-implemented) functions. */
    private final Function<Value, Value> builtinFn;

    public FunctionValue(FunctionNode node, Environment closure) {
        this.node = node;
        this.closure = closure;
        this.builtinFn = null;
    }

    /** Creates a built-in function that does not need an AST node. */
    public FunctionValue(Function<Value, Value> builtinFn) {
        this.node = null;
        this.closure = null;
        this.builtinFn = builtinFn;
    }

    public boolean isBuiltin() { return builtinFn != null; }

    public Function<Value, Value> builtinFn() { return builtinFn; }

    public FunctionNode node() { return node; }

    public Environment closure() { return closure; }

    @Override public Object unwrap() { return this; }
    @Override public boolean isTruthy() { return true; }

    @Override
    public String display() {
        if (builtinFn != null) return "<builtin-fn>";
        return "<fn:" + (node != null ? node.argument().name() : "?") + ">";
    }

    @Override public String toString() { return display(); }
}
