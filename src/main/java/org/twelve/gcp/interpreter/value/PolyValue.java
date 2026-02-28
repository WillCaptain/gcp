package org.twelve.gcp.interpreter.value;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Runtime representation of a poly (union) value produced by the {@code &} operator.
 * A PolyValue carries multiple simultaneously-present values, one per type-variant.
 * <p>
 * Example: {@code let db = 10 & "Will" & {name="Will"}} evaluates to a PolyValue
 * holding {@code [IntValue(10), StringValue("Will"), EntityValue{name=Will}]}.
 * <p>
 * The {@code as} expression extracts the variant whose runtime type matches
 * the target type annotation (see {@link org.twelve.gcp.interpreter.interpretation.AsInterpretation}).
 */
public class PolyValue implements Value {

    private final List<Value> options;

    public PolyValue(List<Value> options) {
        this.options = Collections.unmodifiableList(options);
    }

    /** All value variants stored in this poly value. */
    public List<Value> options() {
        return options;
    }

    /**
     * Extract the first variant whose Java type matches {@code targetClass}.
     * Returns {@link UnitValue#INSTANCE} when no variant matches.
     */
    public Value extract(Class<? extends Value> targetClass) {
        for (Value v : options) {
            if (targetClass.isInstance(v)) return v;
        }
        return UnitValue.INSTANCE;
    }

    @Override
    public Object unwrap() {
        return options.isEmpty() ? null : options.get(0).unwrap();
    }

    @Override
    public boolean isTruthy() {
        return !options.isEmpty();
    }

    @Override
    public String display() {
        return options.stream().map(Value::display).collect(Collectors.joining(" & "));
    }

    @Override
    public String toString() {
        return display();
    }
}
