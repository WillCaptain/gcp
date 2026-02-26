package org.twelve.gcp.interpreter.interpretation;

import org.twelve.gcp.interpreter.Interpretation;
import org.twelve.gcp.interpreter.Interpreter;
import org.twelve.gcp.interpreter.value.*;
import org.twelve.gcp.node.expression.accessor.ArrayAccessor;

public class ArrayAccessorInterpretation implements Interpretation<ArrayAccessor> {
    @Override
    public Value interpret(ArrayAccessor node, Interpreter interp) {
        Value target = interp.eval(node.array());
        Value index  = interp.eval(node.index());

        if (target instanceof ArrayValue arr && index instanceof IntValue iv)
            return arr.get((int) iv.value());
        if (target instanceof TupleValue tv && index instanceof IntValue iv)
            return tv.get((int) iv.value());
        if (target instanceof DictValue dv) {
            Value v = dv.get(index);
            return v != null ? v : UnitValue.INSTANCE;
        }
        throw new RuntimeException("Cannot index " + target + " with " + index);
    }
}
