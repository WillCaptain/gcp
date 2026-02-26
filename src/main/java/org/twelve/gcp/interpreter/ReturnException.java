package org.twelve.gcp.interpreter;

import org.twelve.gcp.interpreter.value.Value;

/**
 * Control-flow exception used to implement early {@code return} from function bodies.
 * Not a true error; suppressed inside OutlineInterpreter after catching the returned value.
 */
public class ReturnException extends RuntimeException {
    private final Value value;

    public ReturnException(Value value) {
        super(null, null, true, false); // no stack trace – performance sensitive
        this.value = value;
    }

    public Value value() { return value; }
}
