package org.twelve.gcp.interpreter.interpretation;

import org.twelve.gcp.interpreter.Interpretation;
import org.twelve.gcp.interpreter.Interpreter;
import org.twelve.gcp.interpreter.value.*;
import org.twelve.gcp.node.expression.ArrayNode;
import org.twelve.gcp.node.expression.Expression;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import static org.twelve.gcp.interpreter.interpretation.BuiltinMethods.toLong;

public class ArrayInterpretation implements Interpretation<ArrayNode> {
    @Override
    public Value interpret(ArrayNode node, Interpreter interp) {
        if (node.isEmpty()) return new ArrayValue(Collections.emptyList());

        if (node.values() != null) {
            List<Value> elements = new ArrayList<>();
            for (Expression e : node.values()) elements.add(interp.eval(e));

            if (node.processor() != null) {
                Value fn = interp.eval(node.processor());
                elements = elements.stream().map(e -> interp.apply(fn, e)).collect(Collectors.toList());
            }
            if (node.condition() != null) {
                Value fn = interp.eval(node.condition());
                elements = elements.stream().filter(e -> interp.apply(fn, e).isTruthy()).collect(Collectors.toList());
            }
            return new ArrayValue(elements);
        }

        long begin = node.begin() != null ? toLong(interp.eval(node.begin())) : 0L;
        long end   = toLong(interp.eval(node.end()));
        long step  = node.step() != null ? toLong(interp.eval(node.step())) : 1L;

        List<Value> elements = new ArrayList<>();
        for (long i = begin; i <= end; i += step) elements.add(new IntValue(i));

        if (node.condition() != null) {
            Value condFn = interp.eval(node.condition());
            elements.removeIf(v -> !interp.apply(condFn, v).isTruthy());
        }
        if (node.processor() != null) {
            Value procFn = interp.eval(node.processor());
            elements = elements.stream().map(v -> interp.apply(procFn, v)).collect(Collectors.toList());
        }
        return new ArrayValue(elements);
    }
}
